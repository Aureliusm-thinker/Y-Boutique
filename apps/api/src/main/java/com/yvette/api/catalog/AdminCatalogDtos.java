package com.yvette.api.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Back-office (admin) representations of the catalogue. */
public final class AdminCatalogDtos {

    private AdminCatalogDtos() {}

    static final String SLUG_PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";

    // ---- Requests ----

    public record ProductUpsertRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 160) @Pattern(regexp = SLUG_PATTERN, message = "use lower-case letters, numbers and dashes")
            String slug,
            @Size(max = 5000) String description,
            Long categoryId,
            @Size(max = 80) String material,
            @Size(max = 40) String fit,
            @Size(max = 80) String occasion,
            @Size(max = 40) String season,
            @Size(max = 10) List<@NotBlank @Size(max = 30) String> styleTags,
            @Size(max = 200) String seoTitle,
            @Size(max = 320) String seoDescription,
            @NotNull @Size(max = 50) List<@Valid VariantUpsert> variants,
            @NotNull @Size(max = 20) List<@Valid ImageUpsert> images) {}

    public record VariantUpsert(
            Long id,
            @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "letters, numbers, - and _ only")
            String sku,
            @Size(max = 20) String size,
            @Size(max = 40) String colour,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal compareAtPrice,
            @Min(0) Integer lowStockThreshold,
            /** Only used when creating a new variant; later changes go through stock adjustments. */
            @Min(0) Integer openingStock) {}

    public record ImageUpsert(
            @NotBlank @Size(max = 500) @Pattern(regexp = "^https?://.+", message = "must be an http(s) URL") String url,
            @Size(max = 200) String altText) {}

    public record CategoryUpsertRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 120) @Pattern(regexp = SLUG_PATTERN, message = "use lower-case letters, numbers and dashes")
            String slug,
            @Min(0) int position) {}

    // ---- Responses ----

    public record AdminProductSummary(
            Long id, String title, String slug, ProductStatus status, String categoryName,
            int variantCount, int totalOnHand, BigDecimal priceMin, BigDecimal priceMax, String currency,
            String imageUrl, Instant updatedAt) {}

    public record AdminVariant(
            Long id, String sku, String size, String colour, BigDecimal price, BigDecimal compareAtPrice,
            String currency, int onHand, int reserved, int available, int lowStockThreshold, boolean low) {}

    public record AdminImage(String url, String altText) {}

    public record AdminProductDetail(
            Long id, String title, String slug, ProductStatus status, String description, Long categoryId,
            String material, String fit, String occasion, String season, List<String> styleTags,
            String seoTitle, String seoDescription, List<AdminVariant> variants, List<AdminImage> images,
            /** What still needs filling in before the product can be published. Empty = ready. */
            List<String> publishProblems,
            Instant createdAt, Instant updatedAt) {}

    public record AdminCategory(Long id, String name, String slug, int position, long productCount) {}
}
