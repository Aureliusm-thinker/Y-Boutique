package com.yvette.api.checkout;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "shipping_methods")
public class ShippingMethod {

    /** Collection from the boutique: no delivery address needed. */
    public static final String PICKUP = "PICKUP";

    @Id
    private String code;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal freeOver;
    private int position;
    private boolean active;

    protected ShippingMethod() {}

    /** Delivery cost for goods worth {@code goodsTotal} (after discounts). */
    public BigDecimal priceFor(BigDecimal goodsTotal) {
        return freeOver != null && goodsTotal.compareTo(freeOver) >= 0 ? BigDecimal.ZERO : price;
    }

    public boolean needsAddress() {
        return !PICKUP.equals(code);
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getFreeOver() { return freeOver; }
    public int getPosition() { return position; }
    public boolean isActive() { return active; }
}
