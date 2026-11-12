package com.yvette.api.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    private String url;
    private String altText;
    private int position;

    protected ProductImage() {}

    public record Data(String url, String altText) {}

    ProductImage(Product product, String url, String altText, int position) {
        this.product = product;
        this.url = url;
        this.altText = altText;
        this.position = position;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public String getUrl() { return url; }
    public String getAltText() { return altText; }
    public int getPosition() { return position; }
}
