package com.yvette.api.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slug;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // Structured attributes — used for filters now and AI/ML features later.
    private String material;
    private String fit;
    private String occasion;
    private String season;

    @Column(name = "style_tags")
    private String[] styleTags;

    private String seoTitle;
    private String seoDescription;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ProductImage> images = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    protected Product() {}

    public Product(String slug, String title) {
        this.slug = slug;
        this.title = title;
        this.status = ProductStatus.DRAFT;
        this.styleTags = new String[0];
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public void updateDetails(String slug, String title, String description, Category category,
                              String material, String fit, String occasion, String season,
                              String[] styleTags, String seoTitle, String seoDescription) {
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.category = category;
        this.material = material;
        this.fit = fit;
        this.occasion = occasion;
        this.season = season;
        this.styleTags = styleTags;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.updatedAt = Instant.now();
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public ProductVariant addVariant(String sku, String size, String colour, java.math.BigDecimal price,
                                     java.math.BigDecimal compareAtPrice, String currency, int position) {
        ProductVariant v = new ProductVariant(this, sku, size, colour, price, compareAtPrice, currency, position);
        variants.add(v);
        return v;
    }

    public void replaceImages(List<ProductImage.Data> newImages) {
        images.clear();
        for (int i = 0; i < newImages.size(); i++) {
            images.add(new ProductImage(this, newImages.get(i).url(), newImages.get(i).altText(), i));
        }
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ProductStatus getStatus() { return status; }
    public Category getCategory() { return category; }
    public String getMaterial() { return material; }
    public String getFit() { return fit; }
    public String getOccasion() { return occasion; }
    public String getSeason() { return season; }
    public String[] getStyleTags() { return styleTags; }
    public String getSeoTitle() { return seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public List<ProductVariant> getVariants() { return variants; }
    public List<ProductImage> getImages() { return images; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
