package com.yvette.api.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class CartDtos {

    private CartDtos() {}

    public record AddItemRequest(@NotNull Long variantId, @Min(1) @Max(10) int quantity) {}

    public record SetQuantityRequest(@Min(0) @Max(10) int quantity) {}

    public record DiscountRequest(@Size(max = 40) String code) {}

    public record CartLine(
            Long variantId, String productSlug, String title, String size, String colour, String sku,
            String imageUrl, BigDecimal unitPrice, BigDecimal compareAtPrice, int quantity, BigDecimal lineTotal,
            int available,
            /** e.g. "Only 2 left — quantity reduced" or "No longer available"; null when fine. */
            String problem) {}

    public record DiscountView(String code, String name, BigDecimal amount, boolean freeShipping, String problem) {}

    public record ShippingOption(String code, String name, String description, BigDecimal price,
                                 BigDecimal freeOver, boolean needsAddress) {}

    public record CartView(
            UUID token, List<CartLine> lines, int itemCount, BigDecimal subtotal, DiscountView discount,
            /** Total before delivery (delivery is chosen at checkout). */
            BigDecimal totalBeforeShipping, List<ShippingOption> shippingOptions, String currency,
            boolean readyForCheckout) {}
}
