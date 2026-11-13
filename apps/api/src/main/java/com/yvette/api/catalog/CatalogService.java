package com.yvette.api.catalog;

import com.yvette.api.catalog.CatalogDtos.CategoryDto;
import com.yvette.api.catalog.CatalogDtos.Facets;
import com.yvette.api.catalog.CatalogDtos.ImageDto;
import com.yvette.api.catalog.CatalogDtos.ProductDetailDto;
import com.yvette.api.catalog.CatalogDtos.ProductSummaryDto;
import com.yvette.api.catalog.CatalogDtos.SitemapEntry;
import com.yvette.api.catalog.CatalogDtos.Suggestions;
import com.yvette.api.catalog.CatalogDtos.VariantDto;
import com.yvette.api.catalog.CatalogSearch.Filters;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.common.PageResponse;
import com.yvette.api.inventory.InventoryService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    /** "Only N left" is shown at or below this many available. */
    static final int SHOW_STOCK_LEFT_AT = 5;

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final InventoryService inventory;
    private final CatalogSearch search;
    private final JdbcClient jdbc;

    public CatalogService(ProductRepository products, CategoryRepository categories, InventoryService inventory,
                          CatalogSearch search, JdbcClient jdbc) {
        this.products = products;
        this.categories = categories;
        this.inventory = inventory;
        this.search = search;
        this.jdbc = jdbc;
    }

    public List<CategoryDto> listCategories() {
        return categories.findAllByOrderByPositionAsc().stream().map(CatalogService::toDto).toList();
    }

    public PageResponse<ProductSummaryDto> listProducts(Filters filters, int page, int size) {
        CatalogSearch.Result result = search.search(filters, page, size);
        List<ProductSummaryDto> items = summaries(result.productIds());
        int totalPages = (int) ((result.total() + size - 1) / size);
        return new PageResponse<>(items, page, size, result.total(), totalPages);
    }

    public Facets facets(String category) {
        CatalogSearch.Facets f = search.facets(category);
        return new Facets(f.sizes(), f.colours(), f.minPrice(), f.maxPrice());
    }

    /** Autocomplete: a few matching products and categories as the shopper types. */
    public Suggestions suggest(String query) {
        if (query == null || query.strip().length() < 2) return new Suggestions(List.of(), List.of());
        Filters filters = new Filters(null, query, List.of(), List.of(), null, null, false,
                CatalogSearch.Sort.RELEVANCE);
        List<ProductSummaryDto> found = summaries(search.search(filters, 0, 6).productIds());
        List<String> words = CatalogSearch.words(query);
        List<CategoryDto> cats = categories.findAllByOrderByPositionAsc().stream()
                .filter(c -> {
                    String name = c.getName().toLowerCase(Locale.ROOT);
                    // "dres" or "dreses" → Dresses: compare the first 4 letters of each word.
                    return words.stream().anyMatch(w -> name.contains(w.length() > 4 ? w.substring(0, 4) : w));
                })
                .map(CatalogService::toDto)
                .toList();
        return new Suggestions(found, cats);
    }

    public ProductDetailDto getProduct(String slug) {
        Product p = products.findBySlugAndStatus(slug, ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));

        Map<Long, Integer> available = inventory.availableByVariant(
                p.getVariants().stream().map(ProductVariant::getId).toList());

        List<VariantDto> variants = p.getVariants().stream()
                .map(v -> {
                    int left = available.getOrDefault(v.getId(), 0);
                    return new VariantDto(v.getId(), v.getSku(), v.getSize(), v.getColour(), v.getPrice(),
                            v.getCompareAtPrice(), v.getCurrency(), left > 0,
                            left > 0 && left <= SHOW_STOCK_LEFT_AT ? left : null);
                })
                .toList();

        List<Long> relatedIds = jdbc.sql("""
                        SELECT related_product_id FROM product_relations
                        WHERE product_id = :id ORDER BY position, related_product_id""")
                .param("id", p.getId()).query(Long.class).list();

        return new ProductDetailDto(
                p.getId(), p.getSlug(), p.getTitle(), p.getDescription(),
                p.getCategory() == null ? null : toDto(p.getCategory()),
                p.getMaterial(), p.getFit(), p.getOccasion(), p.getSeason(),
                p.getStyleTags() == null ? List.of() : Arrays.asList(p.getStyleTags()),
                variants,
                p.getImages().stream().map(i -> new ImageDto(i.getUrl(), i.getAltText())).toList(),
                p.getSeoTitle(), p.getSeoDescription(), p.getUpdatedAt(),
                summaries(relatedIds));
    }

    /** Every public catalogue page worth listing in sitemap.xml. */
    public List<SitemapEntry> sitemap() {
        Stream<SitemapEntry> categoryEntries = categories.findAllByOrderByPositionAsc().stream()
                .map(c -> new SitemapEntry("/shop?category=" + c.getSlug(), null));
        Stream<SitemapEntry> productEntries = products.findByStatus(ProductStatus.ACTIVE, Pageable.unpaged()).stream()
                .map(p -> new SitemapEntry("/products/" + p.getSlug(), p.getUpdatedAt()));
        return Stream.concat(categoryEntries, productEntries).toList();
    }

    /** Live products as summaries, in the order of the given ids (others are skipped). */
    public List<ProductSummaryDto> summaries(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        Map<Long, Product> byId = products.findAllById(ids).stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Integer> available = inventory.availableByVariant(byId.values().stream()
                .flatMap(p -> p.getVariants().stream()).map(ProductVariant::getId).toList());
        List<ProductSummaryDto> out = new ArrayList<>();
        for (Long id : ids) {
            Product p = byId.get(id);
            if (p != null) out.add(toSummary(p, available));
        }
        return out;
    }

    private static CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getSlug(), c.getName());
    }

    private static ProductSummaryDto toSummary(Product p, Map<Long, Integer> available) {
        ProductVariant cheapest = p.getVariants().stream()
                .min(Comparator.comparing(ProductVariant::getPrice))
                .orElse(null);
        ProductImage image = p.getImages().isEmpty() ? null : p.getImages().getFirst();
        boolean soldOut = p.getVariants().stream().noneMatch(v -> available.getOrDefault(v.getId(), 0) > 0);
        return new ProductSummaryDto(
                p.getId(), p.getSlug(), p.getTitle(),
                p.getCategory() == null ? null : p.getCategory().getSlug(),
                cheapest == null ? null : cheapest.getPrice(),
                cheapest == null ? null : cheapest.getCompareAtPrice(),
                cheapest == null ? null : cheapest.getCurrency(),
                image == null ? null : image.getUrl(),
                image == null ? null : image.getAltText(),
                soldOut);
    }
}
