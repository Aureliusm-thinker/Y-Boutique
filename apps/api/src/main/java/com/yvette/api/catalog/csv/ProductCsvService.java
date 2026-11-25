package com.yvette.api.catalog.csv;

import com.yvette.api.catalog.AdminCatalogDtos.ImageUpsert;
import com.yvette.api.catalog.AdminCatalogDtos.ProductUpsertRequest;
import com.yvette.api.catalog.AdminCatalogDtos.VariantUpsert;
import com.yvette.api.catalog.AdminCatalogService;
import com.yvette.api.catalog.Category;
import com.yvette.api.catalog.CategoryRepository;
import com.yvette.api.catalog.Product;
import com.yvette.api.catalog.ProductImage;
import com.yvette.api.catalog.ProductRepository;
import com.yvette.api.catalog.ProductVariant;
import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.ConflictException;
import com.yvette.api.inventory.InventoryLevel;
import com.yvette.api.inventory.InventoryService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Product spreadsheet import and export (backlog E1-5).
 *
 * <p>One row per variant (SKU). Product columns are repeated on each of its rows (or left blank after the first).
 * Rows are grouped into products by {@code slug} (or by {@code title} for new products without a slug).
 *
 * <p>Import rules:
 * <ul>
 *   <li>An existing slug updates that product; anything else creates a new <b>draft</b> product.</li>
 *   <li>A SKU already on the product updates that variant; a new SKU adds a variant. Variants missing from the
 *       file are kept, never deleted.</li>
 *   <li>A column missing from the file leaves that field as it is; a column that is there but empty clears it.</li>
 *   <li>{@code stock_on_hand} only sets the opening stock of <i>new</i> SKUs. Stock changes for existing SKUs go
 *       through Inventory, so every change keeps its reason in the stock history.</li>
 *   <li>Every row is checked first; nothing is saved unless the whole file is valid, and then everything is saved
 *       together (all or nothing).</li>
 * </ul>
 */
@Service
public class ProductCsvService {

    public static final int MAX_ROWS = 5000;

    static final List<String> COLUMNS = List.of(
            "slug", "title", "status", "category", "description", "material", "fit", "occasion", "season",
            "style_tags", "seo_title", "seo_description", "image_urls",
            "sku", "size", "colour", "price", "compare_at_price", "low_stock_threshold", "stock_on_hand");

    private static final List<String> PRODUCT_COLUMNS = List.of(
            "title", "category", "description", "material", "fit", "occasion", "season",
            "style_tags", "seo_title", "seo_description", "image_urls");

    public record RowError(int line, String message) {}

    public record ProductChange(String slug, String title, boolean created, int newVariants, int updatedVariants,
                                List<String> changedFields) {}

    public record ImportResult(boolean applied, int rows, int productsCreated, int productsUpdated,
                               int variantsCreated, int variantsUpdated, List<ProductChange> products,
                               List<RowError> errors, List<String> notes) {}

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final InventoryService inventory;
    private final AdminCatalogService catalog;
    private final Validator validator;
    private final TransactionTemplate readOnly;
    private final TransactionTemplate writable;

    public ProductCsvService(ProductRepository products, CategoryRepository categories, InventoryService inventory,
                             AdminCatalogService catalog, Validator validator, PlatformTransactionManager tx) {
        this.products = products;
        this.categories = categories;
        this.inventory = inventory;
        this.catalog = catalog;
        this.validator = validator;
        this.readOnly = new TransactionTemplate(tx);
        this.readOnly.setReadOnly(true);
        this.writable = new TransactionTemplate(tx);
    }

    // ------------------------------------------------------------------ Export

    public String export() {
        return readOnly.execute(status -> {
            StringBuilder out = new StringBuilder("﻿"); // BOM: Excel then opens it as UTF-8 (KSh, accents)
            out.append(Csv.line(COLUMNS));
            List<Product> all = products.findAllByOrderByIdAsc();
            Map<Long, InventoryLevel> levels = inventory.levelsByVariant(all.stream()
                    .flatMap(p -> p.getVariants().stream()).map(ProductVariant::getId).toList());
            for (Product p : all) {
                List<String> productCells = List.of(
                        p.getSlug(), p.getTitle(), p.getStatus().name().toLowerCase(Locale.ROOT),
                        p.getCategory() == null ? "" : p.getCategory().getSlug(),
                        nz(p.getDescription()), nz(p.getMaterial()), nz(p.getFit()), nz(p.getOccasion()),
                        nz(p.getSeason()),
                        String.join(" | ", p.getStyleTags() == null ? new String[0] : p.getStyleTags()),
                        nz(p.getSeoTitle()), nz(p.getSeoDescription()),
                        p.getImages().stream().map(ProductImage::getUrl).collect(Collectors.joining(" | ")));
                List<ProductVariant> variants = p.getVariants();
                if (variants.isEmpty()) {
                    List<String> row = new ArrayList<>(productCells);
                    row.addAll(List.of("", "", "", "", "", "", ""));
                    out.append(Csv.line(row));
                }
                for (ProductVariant v : variants) {
                    InventoryLevel level = levels.get(v.getId());
                    List<String> row = new ArrayList<>(productCells);
                    row.addAll(List.of(v.getSku(), nz(v.getSize()), nz(v.getColour()), v.getPrice().toPlainString(),
                            v.getCompareAtPrice() == null ? "" : v.getCompareAtPrice().toPlainString(),
                            level == null ? "" : String.valueOf(level.getLowStockThreshold()),
                            level == null ? "0" : String.valueOf(level.getOnHand())));
                    out.append(Csv.line(row));
                }
            }
            return out.toString();
        });
    }

    // ------------------------------------------------------------------ Import

    /** A product's worth of rows, checked and turned into the same request the product form sends. */
    private record Planned(int line, Long existingId, ProductUpsertRequest request, ProductChange change) {}

    public ImportResult importCsv(String text, boolean dryRun, Long staffId) {
        List<RowError> errors = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        List<Csv.Row> rows;
        try {
            rows = Csv.parse(text == null ? "" : text);
        } catch (IllegalArgumentException e) {
            return failed(0, List.of(new RowError(0, e.getMessage())));
        }
        if (rows.isEmpty()) return failed(0, List.of(new RowError(0, "The file is empty")));
        if (rows.size() - 1 > MAX_ROWS) {
            return failed(rows.size() - 1, List.of(new RowError(0,
                    "The file has " + (rows.size() - 1) + " rows; split it into files of at most " + MAX_ROWS)));
        }

        Map<String, Integer> header = new HashMap<>();
        Csv.Row headerRow = rows.getFirst();
        for (int i = 0; i < headerRow.cells().size(); i++) {
            String name = headerRow.cells().get(i).strip().toLowerCase(Locale.ROOT).replace(' ', '_');
            if (name.isEmpty()) continue;
            if (name.equals("color")) name = "colour";
            if (!COLUMNS.contains(name)) {
                errors.add(new RowError(headerRow.line(), "Unknown column \"" + headerRow.cells().get(i).strip()
                        + "\". Columns are: " + String.join(", ", COLUMNS)));
            } else if (header.putIfAbsent(name, i) != null) {
                errors.add(new RowError(headerRow.line(), "Column \"" + name + "\" appears twice"));
            }
        }
        for (String required : List.of("sku", "price")) {
            if (!header.containsKey(required)) errors.add(new RowError(headerRow.line(), "Missing column \"" + required + "\""));
        }
        if (!header.containsKey("slug") && !header.containsKey("title")) {
            errors.add(new RowError(headerRow.line(), "Add a \"slug\" or \"title\" column so rows can be grouped into products"));
        }
        if (!errors.isEmpty()) return failed(rows.size() - 1, errors);

        List<Csv.Row> data = rows.subList(1, rows.size());
        List<Planned> plan = readOnly.execute(status -> plan(data, header, errors, notes));
        if (!errors.isEmpty()) return failed(data.size(), errors);

        if (!dryRun) {
            Planned[] current = new Planned[1];
            try {
                writable.executeWithoutResult(status -> {
                    for (Planned p : plan) {
                        if (!hasChanges(p.change())) continue;
                        current[0] = p;
                        if (p.existingId() == null) catalog.createProduct(p.request(), staffId);
                        else catalog.updateProduct(p.existingId(), p.request(), staffId);
                    }
                });
            } catch (BadRequestException | ConflictException e) {
                // Rolled back: nothing was saved.
                Planned p = current[0];
                String who = p == null ? "" : "\"" + p.change().title() + "\": ";
                return failed(data.size(), List.of(new RowError(p == null ? 0 : p.line(), who + describe(e))));
            }
        }
        return summary(!dryRun, data.size(), plan, notes);
    }

    private List<Planned> plan(List<Csv.Row> data, Map<String, Integer> header, List<RowError> errors,
                               List<String> notes) {
        Map<String, Category> categoryByKey = new HashMap<>();
        for (Category c : categories.findAll()) {
            categoryByKey.put(c.getSlug().toLowerCase(Locale.ROOT), c);
            categoryByKey.put(c.getName().toLowerCase(Locale.ROOT), c);
        }

        // Group rows into products, keeping file order.
        Map<String, List<Csv.Row>> groups = new LinkedHashMap<>();
        for (Csv.Row row : data) {
            String slug = cell(row, header, "slug");
            String title = cell(row, header, "title");
            if (slug.isEmpty() && title.isEmpty()) {
                errors.add(new RowError(row.line(), "Needs a slug (existing product) or a title (new product)"));
                continue;
            }
            String key = slug.isEmpty() ? "title:" + title.toLowerCase(Locale.ROOT) : "slug:" + slug.toLowerCase(Locale.ROOT);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        Map<String, Integer> skuLine = new HashMap<>();
        boolean stockIgnored = false;
        List<Planned> plan = new ArrayList<>();

        for (List<Csv.Row> group : groups.values()) {
            Csv.Row first = group.getFirst();
            int errorsBefore = errors.size();
            String slug = cell(first, header, "slug").toLowerCase(Locale.ROOT);
            Optional<Product> existing = slug.isEmpty() ? Optional.empty() : products.findBySlug(slug);

            // Product columns: taken from the first row; later rows may repeat them or leave them empty.
            Map<String, String> fields = new HashMap<>();
            for (String col : PRODUCT_COLUMNS) {
                if (!header.containsKey(col)) continue;
                fields.put(col, cell(first, header, col));
                for (Csv.Row other : group.subList(1, group.size())) {
                    String value = cell(other, header, col);
                    if (!value.isEmpty() && !value.equals(fields.get(col))) {
                        errors.add(new RowError(other.line(), "\"" + col + "\" differs from line " + first.line()
                                + " for the same product; repeat it exactly or leave it empty"));
                    }
                }
            }

            Product p = existing.orElse(null);
            String title = pick(fields, "title", p == null ? null : p.getTitle());
            if (title == null || title.isBlank()) {
                errors.add(new RowError(first.line(), "A new product needs a title"));
                continue;
            }

            Long categoryId;
            if (fields.containsKey("category")) {
                String c = fields.get("category");
                if (c.isEmpty()) {
                    categoryId = null;
                } else {
                    Category found = categoryByKey.get(c.toLowerCase(Locale.ROOT));
                    if (found == null) {
                        errors.add(new RowError(first.line(), "Unknown category \"" + c + "\""));
                        continue;
                    }
                    categoryId = found.getId();
                }
            } else {
                categoryId = p == null || p.getCategory() == null ? null : p.getCategory().getId();
            }

            List<String> tags = fields.containsKey("style_tags") ? split(fields.get("style_tags"))
                    : p == null || p.getStyleTags() == null ? List.of() : Arrays.asList(p.getStyleTags());

            List<ImageUpsert> images;
            if (fields.containsKey("image_urls")) {
                Map<String, String> altByUrl = p == null ? Map.of() : p.getImages().stream()
                        .filter(i -> i.getAltText() != null)
                        .collect(Collectors.toMap(ProductImage::getUrl, ProductImage::getAltText, (a, b) -> a));
                images = split(fields.get("image_urls")).stream()
                        .map(u -> new ImageUpsert(u, altByUrl.get(u))).toList();
            } else {
                images = p == null ? List.of()
                        : p.getImages().stream().map(i -> new ImageUpsert(i.getUrl(), i.getAltText())).toList();
            }

            // Variants: existing ones first (updated where the file mentions them), then new SKUs.
            Map<String, ProductVariant> existingBySku = p == null ? Map.of() : p.getVariants().stream()
                    .collect(Collectors.toMap(v -> v.getSku().toLowerCase(Locale.ROOT), Function.identity()));
            Map<Long, InventoryLevel> levels = p == null ? Map.of()
                    : inventory.levelsByVariant(p.getVariants().stream().map(ProductVariant::getId).toList());
            Map<String, VariantUpsert> fromFile = new LinkedHashMap<>();
            int newVariants = 0;
            int updatedVariants = 0;
            for (Csv.Row row : group) {
                String sku = cell(row, header, "sku");
                if (sku.isEmpty()) {
                    errors.add(new RowError(row.line(), "Missing SKU"));
                    continue;
                }
                String skuKey = sku.toLowerCase(Locale.ROOT);
                Integer seenOn = skuLine.putIfAbsent(skuKey, row.line());
                if (seenOn != null) {
                    errors.add(new RowError(row.line(), "SKU " + sku + " is already on line " + seenOn));
                    continue;
                }
                ProductVariant current = existingBySku.get(skuKey);
                if (current == null && products.skuUsedByOtherProduct(sku, p == null ? -1L : p.getId())) {
                    errors.add(new RowError(row.line(), "SKU " + sku + " belongs to another product"));
                    continue;
                }
                BigDecimal price = money(row, header, "price", errors);
                BigDecimal compareAt = header.containsKey("compare_at_price")
                        ? money(row, header, "compare_at_price", errors)
                        : current == null ? null : current.getCompareAtPrice();
                Integer threshold = header.containsKey("low_stock_threshold")
                        ? whole(row, header, "low_stock_threshold", errors) : null;
                Integer stock = header.containsKey("stock_on_hand") ? whole(row, header, "stock_on_hand", errors) : null;
                if (price == null) {
                    if (cell(row, header, "price").isEmpty()) errors.add(new RowError(row.line(), "Missing price"));
                    continue;
                }
                if (compareAt != null && compareAt.compareTo(price) <= 0) {
                    errors.add(new RowError(row.line(), "compare_at_price must be higher than price (or empty)"));
                    continue;
                }
                String size = header.containsKey("size") ? cell(row, header, "size") : current == null ? null : current.getSize();
                String colour = header.containsKey("colour") ? cell(row, header, "colour") : current == null ? null : current.getColour();
                if (current != null) {
                    InventoryLevel level = levels.get(current.getId());
                    if (stock != null && level != null && stock != level.getOnHand()) stockIgnored = true;
                    if (changed(current, size, colour, price, compareAt)
                            || (threshold != null && level != null && threshold != level.getLowStockThreshold())) {
                        updatedVariants++;
                    }
                    fromFile.put(skuKey, new VariantUpsert(current.getId(), current.getSku(), size, colour, price,
                            compareAt, threshold, null));
                } else {
                    newVariants++;
                    fromFile.put(skuKey, new VariantUpsert(null, sku, size, colour, price, compareAt, threshold, stock));
                }
            }

            List<VariantUpsert> variants = new ArrayList<>();
            if (p != null) {
                for (ProductVariant v : p.getVariants()) {
                    VariantUpsert updated = fromFile.remove(v.getSku().toLowerCase(Locale.ROOT));
                    variants.add(updated != null ? updated : new VariantUpsert(v.getId(), v.getSku(), v.getSize(),
                            v.getColour(), v.getPrice(), v.getCompareAtPrice(), null, null));
                }
            }
            variants.addAll(fromFile.values());

            ProductUpsertRequest request = new ProductUpsertRequest(
                    title, p == null ? (slug.isEmpty() ? null : slug) : p.getSlug(),
                    pick(fields, "description", p == null ? null : p.getDescription()),
                    categoryId,
                    pick(fields, "material", p == null ? null : p.getMaterial()),
                    pick(fields, "fit", p == null ? null : p.getFit()),
                    pick(fields, "occasion", p == null ? null : p.getOccasion()),
                    pick(fields, "season", p == null ? null : p.getSeason()),
                    tags,
                    pick(fields, "seo_title", p == null ? null : p.getSeoTitle()),
                    pick(fields, "seo_description", p == null ? null : p.getSeoDescription()),
                    variants, images, null);

            for (ConstraintViolation<ProductUpsertRequest> v : validator.validate(request)) {
                errors.add(new RowError(first.line(), readable(v)));
            }
            if (errors.size() > errorsBefore) continue;

            List<String> changedFields = p == null ? List.of() : changedFields(p, request);
            plan.add(new Planned(first.line(), p == null ? null : p.getId(), request,
                    new ProductChange(p == null ? request.slug() : p.getSlug(), title, p == null,
                            newVariants, updatedVariants, changedFields)));
        }
        if (stockIgnored) {
            notes.add("Stock numbers for existing SKUs were not changed. Use Inventory to adjust stock, so each "
                    + "change keeps its reason in the history.");
        }
        if (plan.stream().anyMatch(pl -> pl.change().created())) {
            notes.add("New products are saved as drafts. Check them and publish when ready.");
        }
        return plan;
    }

    // ------------------------------------------------------------------ Helpers

    private ImportResult summary(boolean applied, int rows, List<Planned> plan, List<String> notes) {
        List<ProductChange> changes = plan.stream().map(Planned::change).filter(ProductCsvService::hasChanges).toList();
        return new ImportResult(applied, rows,
                (int) changes.stream().filter(ProductChange::created).count(),
                (int) changes.stream().filter(c -> !c.created()).count(),
                changes.stream().mapToInt(ProductChange::newVariants).sum(),
                changes.stream().mapToInt(ProductChange::updatedVariants).sum(),
                changes, List.of(), notes);
    }

    private static boolean hasChanges(ProductChange c) {
        return c.created() || c.newVariants() > 0 || c.updatedVariants() > 0 || !c.changedFields().isEmpty();
    }

    private static ImportResult failed(int rows, List<RowError> errors) {
        List<RowError> sorted = errors.stream()
                .sorted((a, b) -> Integer.compare(a.line(), b.line())).limit(200).toList();
        return new ImportResult(false, rows, 0, 0, 0, 0, List.of(), sorted, List.of());
    }

    private static String describe(RuntimeException e) {
        List<String> details = e instanceof BadRequestException b ? b.getErrors()
                : e instanceof ConflictException c ? c.getErrors() : List.of();
        return details == null || details.isEmpty() ? e.getMessage() : e.getMessage() + ": " + String.join("; ", details);
    }

    private static List<String> changedFields(Product p, ProductUpsertRequest r) {
        List<String> out = new ArrayList<>();
        if (!Objects.equals(p.getTitle(), r.title())) out.add("title");
        if (!Objects.equals(p.getCategory() == null ? null : p.getCategory().getId(), r.categoryId())) out.add("category");
        if (!Objects.equals(nullIfBlank(p.getDescription()), nullIfBlank(r.description()))) out.add("description");
        if (!Objects.equals(nullIfBlank(p.getMaterial()), nullIfBlank(r.material()))) out.add("material");
        if (!Objects.equals(nullIfBlank(p.getFit()), nullIfBlank(r.fit()))) out.add("fit");
        if (!Objects.equals(nullIfBlank(p.getOccasion()), nullIfBlank(r.occasion()))) out.add("occasion");
        if (!Objects.equals(nullIfBlank(p.getSeason()), nullIfBlank(r.season()))) out.add("season");
        List<String> tags = p.getStyleTags() == null ? List.of() : Arrays.asList(p.getStyleTags());
        List<String> newTags = r.styleTags().stream().map(t -> t.toLowerCase(Locale.ROOT)).distinct().toList();
        if (!tags.equals(newTags)) out.add("style tags");
        if (!Objects.equals(nullIfBlank(p.getSeoTitle()), nullIfBlank(r.seoTitle()))) out.add("SEO title");
        if (!Objects.equals(nullIfBlank(p.getSeoDescription()), nullIfBlank(r.seoDescription()))) out.add("SEO description");
        List<String> urls = p.getImages().stream().map(ProductImage::getUrl).toList();
        if (!urls.equals(r.images().stream().map(ImageUpsert::url).toList())) out.add("images");
        return out;
    }

    private static boolean changed(ProductVariant v, String size, String colour, BigDecimal price, BigDecimal compareAt) {
        return !Objects.equals(nullIfBlank(v.getSize()), nullIfBlank(size))
                || !Objects.equals(nullIfBlank(v.getColour()), nullIfBlank(colour))
                || v.getPrice().compareTo(price) != 0
                || (v.getCompareAtPrice() == null ? compareAt != null
                        : compareAt == null || v.getCompareAtPrice().compareTo(compareAt) != 0);
    }

    private static String readable(ConstraintViolation<?> v) {
        String path = v.getPropertyPath().toString()
                .replaceAll("variants\\[(\\d+)]\\.", "variant $1 ")
                .replaceAll("images\\[(\\d+)]\\.url", "image $1")
                .replaceAll("styleTags\\[\\d+]\\.<list element>", "style tag")
                .replace("<list element>", "");
        return path + ": " + v.getMessage();
    }

    private static String cell(Csv.Row row, Map<String, Integer> header, String column) {
        Integer i = header.get(column);
        if (i == null || i >= row.cells().size()) return "";
        return Csv.unguard(row.cells().get(i).strip());
    }

    private static String pick(Map<String, String> fields, String column, String current) {
        return fields.containsKey(column) ? fields.get(column) : current;
    }

    private static List<String> split(String value) {
        return Arrays.stream(value.split("\\|")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    /** "KSh 1,250.00", "1250" and "1 250" all read as 1250. */
    private static BigDecimal money(Csv.Row row, Map<String, Integer> header, String column, List<RowError> errors) {
        String raw = cell(row, header, column);
        if (raw.isEmpty()) return null;
        String cleaned = raw.replaceAll("(?i)^(ksh|kes)\\.?\\s*", "").replaceAll("[,\\s ]", "");
        try {
            BigDecimal value = new BigDecimal(cleaned);
            if (value.signum() < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            errors.add(new RowError(row.line(), column + " \"" + raw + "\" is not a valid amount"));
            return null;
        }
    }

    private static Integer whole(Csv.Row row, Map<String, Integer> header, String column, List<RowError> errors) {
        String raw = cell(row, header, column);
        if (raw.isEmpty()) return null;
        try {
            int value = Integer.parseInt(raw.replaceAll("[,\\s]", ""));
            if (value < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            errors.add(new RowError(row.line(), column + " \"" + raw + "\" must be a whole number, 0 or more"));
            return null;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String nullIfBlank(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
