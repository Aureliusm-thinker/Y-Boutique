package com.yvette.api.catalog;

import com.yvette.api.catalog.CatalogDtos.CategoryDto;
import com.yvette.api.catalog.CatalogDtos.ImageDto;
import com.yvette.api.catalog.CatalogDtos.ProductDetailDto;
import com.yvette.api.catalog.CatalogDtos.ProductSummaryDto;
import com.yvette.api.catalog.CatalogDtos.VariantDto;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.common.PageResponse;
import com.yvette.api.inventory.InventoryService;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final InventoryService inventory;

    public CatalogService(ProductRepository products, CategoryRepository categories, InventoryService inventory) {
        this.products = products;
        this.categories = categories;
        this.inventory = inventory;
    }

    public List<CategoryDto> listCategories() {
        return categories.findAllByOrderByPositionAsc().stream().map(CatalogService::toDto).toList();
    }

    public PageResponse<ProductSummaryDto> listProducts(String categorySlug, Pageable pageable) {
        Page<Product> page = (categorySlug == null || categorySlug.isBlank())
                ? products.findByStatus(ProductStatus.ACTIVE, pageable)
                : products.findByStatusAndCategorySlug(ProductStatus.ACTIVE, categorySlug, pageable);
        return PageResponse.of(page, CatalogService::toSummary);
    }

    public ProductDetailDto getProduct(String slug) {
        Product p = products.findBySlugAndStatus(slug, ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));

        Map<Long, Integer> available = inventory.availableByVariant(
                p.getVariants().stream().map(ProductVariant::getId).toList());

        List<VariantDto> variants = p.getVariants().stream()
                .map(v -> new VariantDto(v.getId(), v.getSku(), v.getSize(), v.getColour(), v.getPrice(),
                        v.getCompareAtPrice(), v.getCurrency(), available.getOrDefault(v.getId(), 0) > 0))
                .toList();

        return new ProductDetailDto(
                p.getId(), p.getSlug(), p.getTitle(), p.getDescription(),
                p.getCategory() == null ? null : toDto(p.getCategory()),
                p.getMaterial(), p.getFit(), p.getOccasion(), p.getSeason(),
                p.getStyleTags() == null ? List.of() : Arrays.asList(p.getStyleTags()),
                variants,
                p.getImages().stream().map(i -> new ImageDto(i.getUrl(), i.getAltText())).toList(),
                p.getSeoTitle(), p.getSeoDescription());
    }

    private static CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getSlug(), c.getName());
    }

    private static ProductSummaryDto toSummary(Product p) {
        ProductVariant cheapest = p.getVariants().stream()
                .min(Comparator.comparing(ProductVariant::getPrice))
                .orElse(null);
        ProductImage image = p.getImages().isEmpty() ? null : p.getImages().getFirst();
        return new ProductSummaryDto(
                p.getId(), p.getSlug(), p.getTitle(),
                p.getCategory() == null ? null : p.getCategory().getSlug(),
                cheapest == null ? null : cheapest.getPrice(),
                cheapest == null ? null : cheapest.getCompareAtPrice(),
                cheapest == null ? null : cheapest.getCurrency(),
                image == null ? null : image.getUrl(),
                image == null ? null : image.getAltText());
    }
}
