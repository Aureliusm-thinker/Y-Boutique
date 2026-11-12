package com.yvette.api.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    private String sku;
    private String size;
    private String colour;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String currency;
    private int position;

    protected ProductVariant() {}

    ProductVariant(Product product, String sku, String size, String colour, BigDecimal price,
                   BigDecimal compareAtPrice, String currency, int position) {
        this.product = product;
        update(sku, size, colour, price, compareAtPrice, currency, position);
    }

    public void update(String sku, String size, String colour, BigDecimal price, BigDecimal compareAtPrice,
                       String currency, int position) {
        this.sku = sku;
        this.size = size;
        this.colour = colour;
        this.price = price;
        this.compareAtPrice = compareAtPrice;
        this.currency = currency;
        this.position = position;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public String getSku() { return sku; }
    public String getSize() { return size; }
    public String getColour() { return colour; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getCompareAtPrice() { return compareAtPrice; }
    public String getCurrency() { return currency; }
    public int getPosition() { return position; }
}
