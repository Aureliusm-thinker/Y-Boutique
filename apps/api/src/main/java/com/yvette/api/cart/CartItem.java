package com.yvette.api.cart;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    private Long variantId;
    private int quantity;
    private Instant addedAt = Instant.now();

    protected CartItem() {}

    CartItem(Cart cart, Long variantId, int quantity) {
        this.cart = cart;
        this.variantId = variantId;
        this.quantity = quantity;
    }

    void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Long getVariantId() { return variantId; }
    public int getQuantity() { return quantity; }
    public Instant getAddedAt() { return addedAt; }
}
