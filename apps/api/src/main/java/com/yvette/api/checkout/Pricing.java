package com.yvette.api.checkout;

import com.yvette.api.promotion.Promotion;
import com.yvette.api.promotion.PromotionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The one place that works out what a basket costs: goods, discount, delivery and total.
 * Used for the cart preview and again, authoritatively, when the order is placed.
 */
@Component
public class Pricing {

    private final PromotionRepository promotions;

    public Pricing(PromotionRepository promotions) {
        this.promotions = promotions;
    }

    /** The promotion that applies (or why a code doesn't), with the discount it gives. */
    public record AppliedDiscount(Promotion promotion, BigDecimal amount, boolean freeShipping, String code,
                                  String problem) {
        static AppliedDiscount none() {
            return new AppliedDiscount(null, BigDecimal.ZERO, false, null, null);
        }
    }

    public record Totals(BigDecimal subtotal, AppliedDiscount discount, BigDecimal shipping, BigDecimal total) {}

    public AppliedDiscount discountFor(BigDecimal subtotal, String code, Instant now) {
        String normalised = Promotion.normaliseCode(code);
        if (normalised != null) {
            Promotion p = promotions.findByCode(normalised).orElse(null);
            if (p == null) return new AppliedDiscount(null, BigDecimal.ZERO, false, normalised, "We don't recognise that code");
            String problem = p.problemFor(subtotal, now);
            if (problem != null) return new AppliedDiscount(null, BigDecimal.ZERO, false, normalised, problem);
            return new AppliedDiscount(p, p.discountOn(subtotal), p.getType() == Promotion.Type.FREE_SHIPPING,
                    normalised, null);
        }
        // No code: the best automatic sale, if any.
        List<Promotion> automatic = promotions.findActiveAutomatic().stream()
                .filter(p -> p.problemFor(subtotal, now) == null)
                .toList();
        return automatic.stream()
                .max(Comparator.comparing(p -> p.discountOn(subtotal)))
                .map(p -> new AppliedDiscount(p, p.discountOn(subtotal), p.getType() == Promotion.Type.FREE_SHIPPING,
                        null, null))
                .orElse(AppliedDiscount.none());
    }

    public Totals totals(BigDecimal subtotal, AppliedDiscount discount, ShippingMethod method) {
        BigDecimal goods = subtotal.subtract(discount.amount());
        BigDecimal shipping = method == null || discount.freeShipping() ? BigDecimal.ZERO : method.priceFor(goods);
        return new Totals(subtotal, discount, shipping, goods.add(shipping));
    }
}
