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
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slug;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    private int position;

    protected Category() {}

    public Category(String slug, String name, int position) {
        this.slug = slug;
        this.name = name;
        this.position = position;
    }

    public void update(String slug, String name, int position) {
        this.slug = slug;
        this.name = name;
        this.position = position;
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public Category getParent() { return parent; }
    public int getPosition() { return position; }
}
