-- Core catalogue + inventory schema (see docs/05-architecture.md §4)

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    parent_id   BIGINT REFERENCES categories (id),
    position    INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id               BIGSERIAL PRIMARY KEY,
    slug             VARCHAR(160) NOT NULL UNIQUE,
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                     CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    category_id      BIGINT REFERENCES categories (id),
    -- Structured attributes: required for filtering today and AI/ML later (docs/04 §3.2)
    material         VARCHAR(80),
    fit              VARCHAR(40),
    occasion         VARCHAR(80),
    season           VARCHAR(40),
    style_tags       TEXT[] NOT NULL DEFAULT '{}',
    seo_title        VARCHAR(200),
    seo_description  VARCHAR(320),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_status_category ON products (status, category_id);

CREATE TABLE product_variants (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    sku               VARCHAR(64) NOT NULL UNIQUE,
    size              VARCHAR(20),
    colour            VARCHAR(40),
    price             NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    compare_at_price  NUMERIC(12, 2) CHECK (compare_at_price >= 0),
    currency          VARCHAR(3) NOT NULL DEFAULT 'USD',
    position          INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_variants_product ON product_variants (product_id);

CREATE TABLE product_images (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    alt_text    VARCHAR(200),
    position    INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_images_product ON product_images (product_id);

CREATE TABLE inventory_levels (
    variant_id           BIGINT PRIMARY KEY REFERENCES product_variants (id) ON DELETE CASCADE,
    on_hand              INT NOT NULL DEFAULT 0 CHECK (on_hand >= 0),
    reserved             INT NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    low_stock_threshold  INT NOT NULL DEFAULT 3
);
