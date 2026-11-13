-- Sprint 2: typo-tolerant search (pg_trgm) and curated "complete the look" pairs (FR-SF-04, FR-SF-06).

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_products_title_trgm ON products USING gin (lower(title) gin_trgm_ops);
CREATE INDEX idx_variants_size ON product_variants (size);
CREATE INDEX idx_variants_colour ON product_variants (lower(colour));

CREATE TABLE product_relations (
    product_id          BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    related_product_id  BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    position            INT NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, related_product_id),
    CHECK (product_id <> related_product_id)
);
