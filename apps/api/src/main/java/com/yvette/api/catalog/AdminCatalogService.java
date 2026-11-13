package com.yvette.api.catalog;

import com.yvette.api.catalog.AdminCatalogDtos.AdminCategory;
import com.yvette.api.catalog.AdminCatalogDtos.AdminImage;
import com.yvette.api.catalog.AdminCatalogDtos.AdminProductDetail;
import com.yvette.api.catalog.AdminCatalogDtos.AdminProductSummary;
import com.yvette.api.catalog.AdminCatalogDtos.AdminVariant;
import com.yvette.api.catalog.AdminCatalogDtos.CategoryUpsertRequest;
import com.yvette.api.catalog.AdminCatalogDtos.ProductUpsertRequest;
import com.yvette.api.catalog.AdminCatalogDtos.RelatedProduct;
import com.yvette.api.catalog.AdminCatalogDtos.VariantUpsert;
import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.ConflictException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.common.PageResponse;
import com.yvette.api.inventory.InventoryLevel;
import com.yvette.api.inventory.InventoryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Back-office catalogue management (docs/06 Epic E1). */
@Service
@Transactional
public class AdminCatalogService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final InventoryService inventory;
    private final EntityManager em;
    private final String currency;
    private final JdbcClient jdbc;

    public AdminCatalogService(ProductRepository products, CategoryRepository categories, InventoryService inventory,
                               EntityManager em, @Value("${yvette.store.currency:KES}") String currency,
                               JdbcClient jdbc) {
        this.jdbc = jdbc;
        this.products = products;
        this.categories = categories;
        this.inventory = inventory;
        this.em = em;
        this.currency = currency;
    }

    // ------------------------------------------------------------------ products

    @Transactional(readOnly = true)
    public PageResponse<AdminProductSummary> listProducts(String query, ProductStatus status, Pageable pageable) {
        Specification<Product> spec = (root, q, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> {
                q.distinct(true);
                var variants = root.join("variants", JoinType.LEFT);
                return cb.or(cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(variants.get("sku")), like));
            });
        }
        Page<Product> page = products.findAll(spec, pageable);
        List<Long> variantIds = page.getContent().stream()
                .flatMap(p -> p.getVariants().stream()).map(ProductVariant::getId).toList();
        Map<Long, InventoryLevel> levels = inventory.levelsByVariant(variantIds);
        return PageResponse.of(page, p -> toSummary(p, levels));
    }

    @Transactional(readOnly = true)
    public AdminProductDetail getProduct(Long id) {
        return toDetail(find(id));
    }

    public AdminProductDetail createProduct(ProductUpsertRequest req, Long staffId) {
        String slug = (req.slug() == null || req.slug().isBlank())
                ? Slugs.unique(Slugs.fromTitle(req.title()), products::existsBySlug)
                : req.slug();
        if (products.existsBySlug(slug)) throw new ConflictException("Slug '" + slug + "' is already used");

        Product product = new Product(slug, req.title().trim());
        applyDetails(product, slug, req);
        validateVariantsForProduct(req.variants(), null);
        for (VariantUpsert v : req.variants()) {
            if (v.id() != null) throw new BadRequestException("New products cannot reference existing variant ids");
        }
        product = products.saveAndFlush(product);
        addNewVariants(product, req.variants(), staffId);
        product.replaceImages(toImageData(req));
        if (req.relatedProductIds() != null) setRelated(product.getId(), req.relatedProductIds());
        return toDetail(product);
    }

    public AdminProductDetail updateProduct(Long id, ProductUpsertRequest req, Long staffId) {
        Product product = find(id);
        String slug = (req.slug() == null || req.slug().isBlank()) ? product.getSlug() : req.slug();
        if (products.existsBySlugAndIdNot(slug, id)) throw new ConflictException("Slug '" + slug + "' is already used");
        validateVariantsForProduct(req.variants(), id);

        Set<Long> existingIds = product.getVariants().stream().map(ProductVariant::getId).collect(Collectors.toSet());
        Set<Long> keptIds = req.variants().stream().map(VariantUpsert::id).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!existingIds.containsAll(keptIds)) {
            throw new BadRequestException("Unknown variant id for this product");
        }

        // Remove dropped variants first and flush, so their SKUs can be reused in the same save.
        product.getVariants().removeIf(v -> !keptIds.contains(v.getId()));
        em.flush();

        Map<Long, ProductVariant> byId = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
        for (int i = 0; i < req.variants().size(); i++) {
            VariantUpsert v = req.variants().get(i);
            if (v.id() == null) continue;
            byId.get(v.id()).update(v.sku().trim(), blankToNull(v.size()), blankToNull(v.colour()), v.price(),
                    v.compareAtPrice(), currency, i);
            if (v.lowStockThreshold() != null) inventory.setThreshold(v.id(), v.lowStockThreshold());
        }
        applyDetails(product, slug, req);
        product.replaceImages(toImageData(req));
        em.flush();
        addNewVariants(product, req.variants(), staffId);
        if (req.relatedProductIds() != null) setRelated(product.getId(), req.relatedProductIds());

        // A live product must stay publishable.
        if (product.getStatus() == ProductStatus.ACTIVE) {
            List<String> problems = publishProblems(product);
            if (!problems.isEmpty()) {
                throw new BadRequestException("This change would leave a live product incomplete", problems);
            }
        }
        return toDetail(product);
    }

    public AdminProductDetail changeStatus(Long id, ProductStatus status) {
        Product product = find(id);
        if (status == ProductStatus.ACTIVE) {
            List<String> problems = publishProblems(product);
            if (!problems.isEmpty()) throw new BadRequestException("Product is not ready to publish", problems);
        }
        product.changeStatus(status);
        return toDetail(product);
    }

    /** Required attributes before publishing (docs/06 E1-2). */
    static List<String> publishProblems(Product p) {
        List<String> problems = new ArrayList<>();
        if (isBlank(p.getDescription())) problems.add("Add a description");
        if (p.getCategory() == null) problems.add("Choose a category");
        if (isBlank(p.getMaterial())) problems.add("Add the material");
        if (p.getImages().isEmpty()) problems.add("Add at least one image");
        if (p.getVariants().isEmpty()) problems.add("Add at least one variant (size/colour)");
        if (p.getVariants().stream().anyMatch(v -> v.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            problems.add("Every variant needs a price above zero");
        }
        return problems;
    }

    public AdminProductDetail updateRelated(Long id, List<Long> relatedIds) {
        Product product = find(id);
        setRelated(id, relatedIds);
        return toDetail(product);
    }

    // ------------------------------------------------------------------ categories

    @Transactional(readOnly = true)
    public List<AdminCategory> listCategories() {
        return categories.findAllByOrderByPositionAsc().stream()
                .map(c -> new AdminCategory(c.getId(), c.getName(), c.getSlug(), c.getPosition(),
                        products.countByCategoryId(c.getId())))
                .toList();
    }

    public AdminCategory createCategory(CategoryUpsertRequest req) {
        String slug = isBlank(req.slug()) ? Slugs.fromTitle(req.name()) : req.slug();
        if (categories.existsBySlug(slug)) throw new ConflictException("Category slug '" + slug + "' is already used");
        Category c = categories.save(new Category(slug, req.name().trim(), req.position()));
        return new AdminCategory(c.getId(), c.getName(), c.getSlug(), c.getPosition(), 0);
    }

    public AdminCategory updateCategory(Long id, CategoryUpsertRequest req) {
        Category c = categories.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        String slug = isBlank(req.slug()) ? c.getSlug() : req.slug();
        if (categories.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException("Category slug '" + slug + "' is already used");
        }
        c.update(slug, req.name().trim(), req.position());
        return new AdminCategory(c.getId(), c.getName(), c.getSlug(), c.getPosition(),
                products.countByCategoryId(c.getId()));
    }

    // ------------------------------------------------------------------ helpers

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private void applyDetails(Product product, String slug, ProductUpsertRequest req) {
        Category category = req.categoryId() == null ? null
                : categories.findById(req.categoryId()).orElseThrow(() -> new BadRequestException("Unknown category"));
        String[] tags = req.styleTags() == null ? new String[0]
                : req.styleTags().stream().map(t -> t.trim().toLowerCase(Locale.ROOT)).distinct().toArray(String[]::new);
        product.updateDetails(slug, req.title().trim(), blankToNull(req.description()), category,
                blankToNull(req.material()), blankToNull(req.fit()), blankToNull(req.occasion()),
                blankToNull(req.season()), tags, blankToNull(req.seoTitle()), blankToNull(req.seoDescription()));
    }

    private void validateVariantsForProduct(List<VariantUpsert> variants, Long productId) {
        Set<String> seen = new HashSet<>();
        for (VariantUpsert v : variants) {
            String sku = v.sku().trim().toLowerCase(Locale.ROOT);
            if (!seen.add(sku)) throw new BadRequestException("SKU '" + v.sku() + "' is used twice");
            if (v.compareAtPrice() != null && v.compareAtPrice().compareTo(v.price()) <= 0) {
                throw new BadRequestException("Compare-at price for " + v.sku() + " must be higher than the price");
            }
            if (products.skuUsedByOtherProduct(v.sku().trim(), productId == null ? -1L : productId)) {
                throw new ConflictException("SKU '" + v.sku() + "' is already used by another product");
            }
        }
    }

    private void addNewVariants(Product product, List<VariantUpsert> requested, Long staffId) {
        List<ProductVariant> created = new ArrayList<>();
        List<VariantUpsert> specs = new ArrayList<>();
        for (int i = 0; i < requested.size(); i++) {
            VariantUpsert v = requested.get(i);
            if (v.id() != null) continue;
            created.add(product.addVariant(v.sku().trim(), blankToNull(v.size()), blankToNull(v.colour()), v.price(),
                    v.compareAtPrice(), currency, i));
            specs.add(v);
        }
        if (created.isEmpty()) return;
        em.flush(); // assigns ids to the new variants
        for (int i = 0; i < created.size(); i++) {
            VariantUpsert v = specs.get(i);
            inventory.createLevel(created.get(i).getId(),
                    v.lowStockThreshold() == null ? InventoryService.DEFAULT_LOW_STOCK_THRESHOLD : v.lowStockThreshold(),
                    v.openingStock() == null ? 0 : v.openingStock(), staffId);
        }
    }

    private void setRelated(Long productId, List<Long> relatedIds) {
        List<Long> ids = relatedIds.stream().distinct().filter(id -> !id.equals(productId)).toList();
        if (products.findAllById(ids).size() != ids.size()) {
            throw new BadRequestException("Unknown product in \"complete the look\"");
        }
        jdbc.sql("DELETE FROM product_relations WHERE product_id = :id").param("id", productId).update();
        for (int i = 0; i < ids.size(); i++) {
            jdbc.sql("INSERT INTO product_relations (product_id, related_product_id, position) VALUES (:p, :r, :pos)")
                    .param("p", productId).param("r", ids.get(i)).param("pos", i).update();
        }
    }

    private List<RelatedProduct> related(Long productId) {
        return jdbc.sql("""
                        SELECT p.id, p.title, p.slug, p.status FROM product_relations r
                        JOIN products p ON p.id = r.related_product_id
                        WHERE r.product_id = :id ORDER BY r.position""")
                .param("id", productId)
                .query((rs, n) -> new RelatedProduct(rs.getLong(1), rs.getString(2), rs.getString(3),
                        ProductStatus.valueOf(rs.getString(4))))
                .list();
    }

    private static List<ProductImage.Data> toImageData(ProductUpsertRequest req) {
        return req.images().stream().map(i -> new ProductImage.Data(i.url().trim(), blankToNull(i.altText()))).toList();
    }

    private AdminProductSummary toSummary(Product p, Map<Long, InventoryLevel> levels) {
        BigDecimal min = p.getVariants().stream().map(ProductVariant::getPrice).min(Comparator.naturalOrder()).orElse(null);
        BigDecimal max = p.getVariants().stream().map(ProductVariant::getPrice).max(Comparator.naturalOrder()).orElse(null);
        int stock = p.getVariants().stream().mapToInt(v -> {
            InventoryLevel l = levels.get(v.getId());
            return l == null ? 0 : l.getOnHand();
        }).sum();
        return new AdminProductSummary(p.getId(), p.getTitle(), p.getSlug(), p.getStatus(),
                p.getCategory() == null ? null : p.getCategory().getName(), p.getVariants().size(), stock, min, max,
                currency, p.getImages().isEmpty() ? null : p.getImages().getFirst().getUrl(), p.getUpdatedAt());
    }

    private AdminProductDetail toDetail(Product p) {
        Map<Long, InventoryLevel> levels = inventory.levelsByVariant(
                p.getVariants().stream().map(ProductVariant::getId).toList());
        List<AdminVariant> variants = p.getVariants().stream()
                .sorted(Comparator.comparingInt(ProductVariant::getPosition))
                .map(v -> {
                    InventoryLevel l = levels.get(v.getId());
                    return new AdminVariant(v.getId(), v.getSku(), v.getSize(), v.getColour(), v.getPrice(),
                            v.getCompareAtPrice(), v.getCurrency(),
                            l == null ? 0 : l.getOnHand(), l == null ? 0 : l.getReserved(),
                            l == null ? 0 : l.available(),
                            l == null ? InventoryService.DEFAULT_LOW_STOCK_THRESHOLD : l.getLowStockThreshold(),
                            l == null || l.isLow());
                })
                .toList();
        return new AdminProductDetail(p.getId(), p.getTitle(), p.getSlug(), p.getStatus(), p.getDescription(),
                p.getCategory() == null ? null : p.getCategory().getId(), p.getMaterial(), p.getFit(),
                p.getOccasion(), p.getSeason(),
                p.getStyleTags() == null ? List.of() : Arrays.asList(p.getStyleTags()),
                p.getSeoTitle(), p.getSeoDescription(), variants,
                p.getImages().stream().map(i -> new AdminImage(i.getUrl(), i.getAltText())).toList(),
                publishProblems(p), related(p.getId()), p.getCreatedAt(), p.getUpdatedAt());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
