-- Sprint 4: customer accounts (docs/06 Epic E4) and order management for staff (Epic E5).

CREATE TABLE customers (
    id                 BIGSERIAL PRIMARY KEY,
    email              VARCHAR(254) NOT NULL UNIQUE,      -- stored lower-case
    password_hash      VARCHAR(100) NOT NULL,
    full_name          VARCHAR(120) NOT NULL,
    phone              VARCHAR(20),                       -- 2547XXXXXXXX
    email_verified_at  TIMESTAMPTZ,
    marketing_opt_in   BOOLEAN NOT NULL DEFAULT FALSE,
    -- Optional style profile (FR-CA-07): {"sizes":[],"colours":[],"styles":[]}. Feeds personalisation later.
    preferences        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at      TIMESTAMPTZ
);

-- One-time links for email verification and password reset. Only a hash of the token is stored.
CREATE TABLE customer_tokens (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    purpose      VARCHAR(20) NOT NULL CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
    token_hash   VARCHAR(64) NOT NULL UNIQUE,
    expires_at   TIMESTAMPTZ NOT NULL,
    used_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE customer_addresses (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    label        VARCHAR(40),
    full_name    VARCHAR(120) NOT NULL,
    phone        VARCHAR(20),
    line1        VARCHAR(200) NOT NULL,
    line2        VARCHAR(200),
    city         VARCHAR(80) NOT NULL,
    county       VARCHAR(40) NOT NULL,
    postal_code  VARCHAR(20),
    is_default   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_addresses_customer ON customer_addresses (customer_id);

CREATE TABLE wishlist_items (
    customer_id  BIGINT NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    product_id   BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    added_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (customer_id, product_id)
);

-- "Email me when it's back" (FR / E4-6). Guests can sign up with just an email.
CREATE TABLE stock_alerts (
    id           BIGSERIAL PRIMARY KEY,
    email        VARCHAR(254) NOT NULL,
    customer_id  BIGINT REFERENCES customers (id) ON DELETE CASCADE,
    variant_id   BIGINT NOT NULL REFERENCES product_variants (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    notified_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_stock_alerts_pending ON stock_alerts (variant_id, lower(email)) WHERE notified_at IS NULL;

-- Support notes on a customer (E5-5). Order notes use order_events (type NOTE).
CREATE TABLE customer_notes (
    id             BIGSERIAL PRIMARY KEY,
    customer_id    BIGINT NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    staff_user_id  BIGINT REFERENCES staff_users (id),
    note           VARCHAR(1000) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE orders ADD CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE SET NULL;
ALTER TABLE carts ADD CONSTRAINT fk_carts_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE;
CREATE INDEX idx_carts_customer ON carts (customer_id);

-- Reviews from verified buyers with fit feedback (FR-RV-01/02). Moderated before they appear.
CREATE TABLE reviews (
    id             BIGSERIAL PRIMARY KEY,
    product_id     BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    customer_id    BIGINT REFERENCES customers (id) ON DELETE SET NULL,
    order_id       BIGINT REFERENCES orders (id) ON DELETE SET NULL,
    author_name    VARCHAR(60) NOT NULL,
    rating         SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title          VARCHAR(120),
    body           VARCHAR(2000),
    fit            VARCHAR(20) CHECK (fit IN ('RUNS_SMALL', 'TRUE_TO_SIZE', 'RUNS_LARGE')),
    size_bought    VARCHAR(20),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    moderated_at   TIMESTAMPTZ,
    moderated_by   BIGINT REFERENCES staff_users (id)
);

CREATE INDEX idx_reviews_product_status ON reviews (product_id, status);
CREATE UNIQUE INDEX idx_reviews_one_per_order_product ON reviews (order_id, product_id) WHERE order_id IS NOT NULL;
