package com.yvette.api.promotion;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;

/** A discount code, or (with no code) an automatic sale applied to every cart (FR-CO-05, FR-AD-09). */
@Entity
@Table(name = "promotions")
public class Promotion {

    public enum Type { PERCENT, FIXED, FREE_SHIPPING }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;

    @Enumerated(EnumType.STRING)
    private Type type;

    private BigDecimal value;
    private BigDecimal minSubtotal;
    private Instant startsAt;
    private Instant endsAt;
    private Integer usageLimit;
    private int timesUsed;
    private boolean active = true;
    private Instant createdAt = Instant.now();

    protected Promotion() {}

    public Promotion(String code, String name, Type type, BigDecimal value) {
        this.code = normaliseCode(code);
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public static String normaliseCode(String code) {
        return code == null || code.isBlank() ? null : code.strip().toUpperCase(Locale.ROOT);
    }

    public void update(String code, String name, Type type, BigDecimal value, BigDecimal minSubtotal,
                       Instant startsAt, Instant endsAt, Integer usageLimit, boolean active) {
        this.code = normaliseCode(code);
        this.name = name;
        this.type = type;
        this.value = value == null ? BigDecimal.ZERO : value;
        this.minSubtotal = minSubtotal;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.usageLimit = usageLimit;
        this.active = active;
    }

    /** Why this promotion can't be used right now for this subtotal, or null if it can. */
    public String problemFor(BigDecimal subtotal, Instant now) {
        if (!active) return "This code is no longer active";
        if (startsAt != null && now.isBefore(startsAt)) return "This code isn't active yet";
        if (endsAt != null && !now.isBefore(endsAt)) return "This code has expired";
        if (usageLimit != null && timesUsed >= usageLimit) return "This code has been fully used";
        if (minSubtotal != null && subtotal.compareTo(minSubtotal) < 0) {
            return "Spend at least KSh " + minSubtotal.setScale(0, RoundingMode.HALF_UP).toPlainString()
                    + " to use this code";
        }
        return null;
    }

    /** Discount on the goods (free-shipping promotions discount the delivery instead). */
    public BigDecimal discountOn(BigDecimal subtotal) {
        return switch (type) {
            case PERCENT -> subtotal.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .min(subtotal);
            case FIXED -> value.min(subtotal);
            case FREE_SHIPPING -> BigDecimal.ZERO;
        };
    }

    public void recordUse() { timesUsed++; }
    public void undoUse() { timesUsed = Math.max(0, timesUsed - 1); }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Type getType() { return type; }
    public BigDecimal getValue() { return value; }
    public BigDecimal getMinSubtotal() { return minSubtotal; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Integer getUsageLimit() { return usageLimit; }
    public int getTimesUsed() { return timesUsed; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
