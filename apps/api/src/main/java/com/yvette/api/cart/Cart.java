package com.yvette.api.cart;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A shopper's bag. Identified by a secret token in their cookie; linked to their account once signed in. */
@Entity
@Table(name = "carts")
public class Cart {

    public static final int MAX_QUANTITY = 10;
    public static final int MAX_LINES = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID token;
    private Long customerId;
    private String discountCode;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("addedAt ASC, id ASC")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {}

    public Cart(UUID token) {
        this.token = token;
    }

    public Optional<CartItem> item(Long variantId) {
        return items.stream().filter(i -> i.getVariantId().equals(variantId)).findFirst();
    }

    /** Adds to an existing line or creates one; quantity is capped at {@link #MAX_QUANTITY}. */
    public void add(Long variantId, int quantity) {
        CartItem existing = item(variantId).orElse(null);
        if (existing != null) {
            existing.setQuantity(Math.min(MAX_QUANTITY, existing.getQuantity() + quantity));
        } else {
            items.add(new CartItem(this, variantId, Math.min(MAX_QUANTITY, quantity)));
        }
        touch();
    }

    public void setQuantity(Long variantId, int quantity) {
        if (quantity <= 0) {
            remove(variantId);
            return;
        }
        item(variantId).ifPresent(i -> i.setQuantity(Math.min(MAX_QUANTITY, quantity)));
        touch();
    }

    public void remove(Long variantId) {
        items.removeIf(i -> i.getVariantId().equals(variantId));
        touch();
    }

    public void clear() {
        items.clear();
        discountCode = null;
        touch();
    }

    public void setDiscountCode(String code) {
        this.discountCode = code;
        touch();
    }

    public void assignTo(Long customerId) {
        this.customerId = customerId;
        touch();
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getToken() { return token; }
    public Long getCustomerId() { return customerId; }
    public String getDiscountCode() { return discountCode; }
    public List<CartItem> getItems() { return items; }
    public Instant getUpdatedAt() { return updatedAt; }
}
