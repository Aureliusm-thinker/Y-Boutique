package com.yvette.api.catalog;

import com.yvette.api.catalog.AdminCatalogDtos.AdminCategory;
import com.yvette.api.catalog.AdminCatalogDtos.AdminProductDetail;
import com.yvette.api.catalog.AdminCatalogDtos.AdminProductSummary;
import com.yvette.api.catalog.AdminCatalogDtos.CategoryUpsertRequest;
import com.yvette.api.catalog.AdminCatalogDtos.ProductUpsertRequest;
import com.yvette.api.common.CurrentStaff;
import com.yvette.api.common.PageResponse;
import com.yvette.api.staff.Roles;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {

    private final AdminCatalogService catalog;

    public AdminCatalogController(AdminCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products")
    @PreAuthorize(Roles.CATALOG_READ)
    public PageResponse<AdminProductSummary> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by("id")));
        return catalog.listProducts(q, status, pageable);
    }

    @GetMapping("/products/{id}")
    @PreAuthorize(Roles.CATALOG_READ)
    public AdminProductDetail product(@PathVariable Long id) {
        return catalog.getProduct(id);
    }

    @PostMapping("/products")
    @PreAuthorize(Roles.CATALOG_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductDetail create(@Valid @RequestBody ProductUpsertRequest request, @AuthenticationPrincipal Jwt jwt) {
        return catalog.createProduct(request, CurrentStaff.id(jwt));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize(Roles.CATALOG_WRITE)
    public AdminProductDetail update(@PathVariable Long id, @Valid @RequestBody ProductUpsertRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        return catalog.updateProduct(id, request, CurrentStaff.id(jwt));
    }

    @PostMapping("/products/{id}/publish")
    @PreAuthorize(Roles.CATALOG_WRITE)
    public AdminProductDetail publish(@PathVariable Long id) {
        return catalog.changeStatus(id, ProductStatus.ACTIVE);
    }

    @PostMapping("/products/{id}/unpublish")
    @PreAuthorize(Roles.CATALOG_WRITE)
    public AdminProductDetail unpublish(@PathVariable Long id) {
        return catalog.changeStatus(id, ProductStatus.DRAFT);
    }

    @PostMapping("/products/{id}/archive")
    @PreAuthorize(Roles.CATALOG_WRITE)
    public AdminProductDetail archive(@PathVariable Long id) {
        return catalog.changeStatus(id, ProductStatus.ARCHIVED);
    }

    /** Sets the curated "complete the look" products shown on this product's page. */
    @PutMapping("/products/{id}/related")
    @PreAuthorize(Roles.CATALOG_WRITE)
    public AdminProductDetail setRelated(@PathVariable Long id,
                                         @Valid @RequestBody AdminCatalogDtos.RelatedUpdate request) {
        return catalog.updateRelated(id, request.productIds());
    }

    @GetMapping("/categories")
    @PreAuthorize(Roles.CATALOG_READ)
    public List<AdminCategory> categories() {
        return catalog.listCategories();
    }

    @PostMapping("/categories")
    @PreAuthorize(Roles.CATALOG_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCategory createCategory(@Valid @RequestBody CategoryUpsertRequest request) {
        return catalog.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize(Roles.CATALOG_WRITE)
    public AdminCategory updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryUpsertRequest request) {
        return catalog.updateCategory(id, request);
    }
}
