package com.yvette.api.catalog;

import com.yvette.api.catalog.CatalogDtos.CategoryDto;
import com.yvette.api.catalog.CatalogDtos.ProductDetailDto;
import com.yvette.api.catalog.CatalogDtos.ProductSummaryDto;
import com.yvette.api.common.PageResponse;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @GetMapping("/products")
    public PageResponse<ProductSummaryDto> products(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
        return catalog.listProducts(category, pageable);
    }

    @GetMapping("/products/{slug}")
    public ProductDetailDto product(@PathVariable String slug) {
        return catalog.getProduct(slug);
    }
}
