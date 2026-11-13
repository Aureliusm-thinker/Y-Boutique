package com.yvette.api.inventory;

public enum AdjustmentReason {
    /** New stock delivered (also used for a new variant's opening stock). */
    RECEIVED,
    RETURNED,
    DAMAGED,
    LOST,
    /** Stock-take correction. */
    CORRECTION,
    OTHER,
    /** Sold through an order (recorded automatically when the order is paid). */
    SOLD
}
