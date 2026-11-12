package com.yvette.api.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Append-only record of every stock change (who, why, by how much, resulting level). */
@Entity
@Table(name = "inventory_adjustments")
public class InventoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long variantId;
    private int delta;

    @Enumerated(EnumType.STRING)
    private AdjustmentReason reason;

    private String note;
    private int onHandAfter;
    private Long staffUserId;
    private Instant createdAt = Instant.now();

    protected InventoryAdjustment() {}

    public InventoryAdjustment(Long variantId, int delta, AdjustmentReason reason, String note, int onHandAfter,
                               Long staffUserId) {
        this.variantId = variantId;
        this.delta = delta;
        this.reason = reason;
        this.note = note;
        this.onHandAfter = onHandAfter;
        this.staffUserId = staffUserId;
    }

    public Long getId() { return id; }
    public Long getVariantId() { return variantId; }
    public int getDelta() { return delta; }
    public AdjustmentReason getReason() { return reason; }
    public String getNote() { return note; }
    public int getOnHandAfter() { return onHandAfter; }
    public Long getStaffUserId() { return staffUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
