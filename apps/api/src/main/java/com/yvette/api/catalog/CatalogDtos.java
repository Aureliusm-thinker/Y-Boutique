package com.yvette.api.catalog;

import java.math.BigDecimal;
import java.util.List;

/** Public (storefront) API representations of the catalogue. */
public final class CatalogDtos {

    private CatalogDtos() {}

    public record CategoryDto(Long id, String slug, String name) {}

    public record ProductSummaryDto(
            Long id,
            String slug,
            String title,
            String categorySlug,
            BigDecimal priceFrom,
            BigDecimal compareAtPrice,
            String currency,
            String imageUrl,
            String imageAlt) {}

    public record VariantDto(
            Long id,
            String sku,
            String size,
            String colour,
            BigDecimal price,
            BigDecimal compareAtPrice,
            String currency,
            boolean inStock) {}

    public record ImageDto(String url, String altText) {}

    public record ProductDetailDto(
            Long id,
            String slug,
            String title,
            String description,
            CategoryDto category,
            String material,
            String fit,
            String occasion,
            String season,
            List<String> styleTags,
            List<VariantDto> variants,
            List<ImageDto> images,
            String seoTitle,
            String seoDescription) {}
}
