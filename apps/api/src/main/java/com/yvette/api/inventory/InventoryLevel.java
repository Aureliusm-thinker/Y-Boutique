package com.yvette.api.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_levels")
public class InventoryLevel {

    @Id
    private Long variantId;

    private int onHand;
    private int reserved;
    private int lowStockThreshold;

    protected InventoryLevel() {}

    public InventoryLevel(Long variantId, int lowStockThreshold) {
        this.variantId = variantId;
        this.lowStockThreshold = lowStockThreshold;
    }

    /** Applies a stock change; returns false (and changes nothing) if it would go below zero. */
    public boolean adjust(int delta) {
        if (onHand + delta < 0) return false;
        onHand += delta;
        return true;
    }

    /** Holds stock for an unpaid order; false (and no change) if not enough is available. */
    public boolean reserve(int quantity) {
        if (quantity <= 0 || available() < quantity) return false;
        reserved += quantity;
        return true;
    }

    /** Gives back a reservation (unpaid order cancelled or expired). */
    public void release(int quantity) {
        reserved = Math.max(0, reserved - quantity);
    }

    /** A reserved unit was paid for: it leaves the shelf. */
    public void commitSale(int quantity) {
        reserved = Math.max(0, reserved - quantity);
        onHand = Math.max(0, onHand - quantity);
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean isLow() {
        return available() <= lowStockThreshold;
    }

    public Long getVariantId() { return variantId; }
    public int getOnHand() { return onHand; }
    public int getReserved() { return reserved; }
    public int getLowStockThreshold() { return lowStockThreshold; }

    public int available() {
        return Math.max(0, onHand - reserved);
    }
}
