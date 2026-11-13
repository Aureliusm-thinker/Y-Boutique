package com.yvette.api.catalog;

import com.yvette.api.catalog.CatalogDtos.CategoryDto;
import com.yvette.api.catalog.CatalogDtos.Facets;
import com.yvette.api.catalog.CatalogDtos.ProductDetailDto;
import com.yvette.api.catalog.CatalogDtos.ProductSummaryDto;
import com.yvette.api.catalog.CatalogDtos.SitemapEntry;
import com.yvette.api.catalog.CatalogDtos.Suggestions;
import com.yvette.api.catalog.CatalogSearch.Filters;
import com.yvette.api.catalog.CatalogSearch.Sort;
import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.PageResponse;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public storefront catalogue API. */
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private static final int MAX_PAGE_SIZE = 60;

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return catalog.listCategories();
    }

    /**
     * Browse and search. Filters: {@code sizes} and {@code colours} (comma-separated), {@code minPrice},
     * {@code maxPrice}, {@code inStock}; {@code sort} = newest | price_asc | price_desc | relevance.
     */
    @GetMapping("/products")
    public PageResponse<ProductSummaryDto> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sizes,
            @RequestParam(required = false) String colours,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean inStock,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        String query = q == null ? null : q.strip();
        if (query != null && query.length() > 100) query = query.substring(0, 100);
        Filters filters = new Filters(category, query, csv(sizes), csv(colours), minPrice, maxPrice, inStock,
                parseSort(sort));
        return catalog.listProducts(filters, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    @GetMapping("/facets")
    public Facets facets(@RequestParam(required = false) String category) {
        return catalog.facets(category);
    }

    @GetMapping("/suggest")
    public Suggestions suggest(@RequestParam(defaultValue = "") String q) {
        return catalog.suggest(q.length() > 100 ? q.substring(0, 100) : q);
    }

    @GetMapping("/sitemap")
    public List<SitemapEntry> sitemap() {
        return catalog.sitemap();
    }

    @GetMapping("/products/{slug}")
    public ProductDetailDto product(@PathVariable String slug) {
        return catalog.getProduct(slug);
    }

    private static List<String> csv(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::strip).filter(s -> !s.isEmpty()).limit(20).toList();
    }

    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return null;
        try {
            return Sort.valueOf(sort.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown sort '" + sort + "'. Use newest, price_asc, price_desc or relevance.");
        }
    }
}
