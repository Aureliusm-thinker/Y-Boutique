package com.yvette.api.order;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** What was bought, frozen at the time of purchase. */
@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long variantId;
    private Long productId;
    private String sku;
    private String title;
    private String size;
    private String colour;
    private String imageUrl;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
    private int returnedQuantity;

    protected OrderLine() {}

    public record Snapshot(Long variantId, Long productId, String sku, String title, String size, String colour,
                           String imageUrl, BigDecimal unitPrice) {}

    OrderLine(Order order, Snapshot s, int quantity) {
        this.order = order;
        this.variantId = s.variantId();
        this.productId = s.productId();
        this.sku = s.sku();
        this.title = s.title();
        this.size = s.size();
        this.colour = s.colour();
        this.imageUrl = s.imageUrl();
        this.unitPrice = s.unitPrice();
        this.quantity = quantity;
        this.lineTotal = s.unitPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public void recordReturn(int qty) {
        this.returnedQuantity = Math.min(quantity, returnedQuantity + qty);
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Long getVariantId() { return variantId; }
    public Long getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getTitle() { return title; }
    public String getSize() { return size; }
    public String getColour() { return colour; }
    public String getImageUrl() { return imageUrl; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public int getReturnedQuantity() { return returnedQuantity; }
}
