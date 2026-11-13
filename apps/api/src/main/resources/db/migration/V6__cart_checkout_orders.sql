-- Sprint 3: cart, checkout, shipping, discounts, orders and payments (docs/06 Epic E3).
-- Money is NUMERIC(12,2) in the store currency (KES).

CREATE TABLE carts (
    id             BIGSERIAL PRIMARY KEY,
    token          UUID NOT NULL UNIQUE,          -- secret handle kept in the shopper's cookie
    customer_id    BIGINT,                        -- set when a signed-in customer owns the cart (Sprint 4)
    discount_code  VARCHAR(40),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    id          BIGSERIAL PRIMARY KEY,
    cart_id     BIGINT NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    variant_id  BIGINT NOT NULL REFERENCES product_variants (id) ON DELETE CASCADE,
    quantity    INT NOT NULL CHECK (quantity BETWEEN 1 AND 10),
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (cart_id, variant_id)
);

CREATE TABLE shipping_methods (
    code         VARCHAR(30) PRIMARY KEY,
    name         VARCHAR(80) NOT NULL,
    description  VARCHAR(200),
    price        NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    free_over    NUMERIC(12, 2),                  -- free when the discounted subtotal reaches this
    position     INT NOT NULL DEFAULT 0,
    active       BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO shipping_methods (code, name, description, price, free_over, position) VALUES
    ('NAIROBI', 'Nairobi delivery', 'Delivered in 1–2 working days within Nairobi', 300, 10000, 1),
    ('COUNTRYWIDE', 'Countrywide courier', 'Delivered in 2–4 working days anywhere in Kenya', 600, 15000, 2),
    ('EXPRESS', 'Same-day Nairobi', 'Order by 12 noon for delivery today (Mon–Sat)', 800, NULL, 3),
    ('PICKUP', 'Collect from the boutique', 'Ready for collection the next working day', 0, NULL, 4);

CREATE TABLE promotions (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(40) UNIQUE,             -- stored upper-case; NULL = automatic sale (no code needed)
    name          VARCHAR(120) NOT NULL,
    type          VARCHAR(20) NOT NULL CHECK (type IN ('PERCENT', 'FIXED', 'FREE_SHIPPING')),
    value         NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (value >= 0),
    min_subtotal  NUMERIC(12, 2),
    starts_at     TIMESTAMPTZ,
    ends_at       TIMESTAMPTZ,
    usage_limit   INT CHECK (usage_limit > 0),
    times_used    INT NOT NULL DEFAULT 0,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (type <> 'PERCENT' OR value <= 100)
);

CREATE TABLE orders (
    id                    BIGSERIAL PRIMARY KEY,
    number                VARCHAR(20) NOT NULL UNIQUE,     -- e.g. YV-100042
    access_token          UUID NOT NULL UNIQUE,            -- lets a guest view their own order
    cart_token            UUID,                            -- the cart emptied once this order is paid
    customer_id           BIGINT,
    email                 VARCHAR(254) NOT NULL,
    phone                 VARCHAR(20) NOT NULL,
    status                VARCHAR(20) NOT NULL
                          CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    payment_status        VARCHAR(20) NOT NULL
                          CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED')),
    payment_method        VARCHAR(10) NOT NULL CHECK (payment_method IN ('MPESA', 'CARD')),
    shipping_name         VARCHAR(120) NOT NULL,
    address_line1         VARCHAR(200),
    address_line2         VARCHAR(200),
    city                  VARCHAR(80),
    county                VARCHAR(40),
    postal_code           VARCHAR(20),
    country               VARCHAR(2) NOT NULL DEFAULT 'KE',
    delivery_notes        VARCHAR(300),
    shipping_method_code  VARCHAR(30) NOT NULL,
    shipping_method_name  VARCHAR(80) NOT NULL,
    subtotal              NUMERIC(12, 2) NOT NULL,
    discount_total        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    shipping_total        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total                 NUMERIC(12, 2) NOT NULL,
    refunded_total        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    currency              VARCHAR(3) NOT NULL DEFAULT 'KES',
    promotion_id          BIGINT REFERENCES promotions (id),
    discount_code         VARCHAR(40),
    carrier               VARCHAR(60),
    tracking_number       VARCHAR(80),
    tracking_url          VARCHAR(500),
    placed_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at               TIMESTAMPTZ,
    shipped_at            TIMESTAMPTZ,
    delivered_at          TIMESTAMPTZ,
    cancelled_at          TIMESTAMPTZ,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE SEQUENCE order_number_seq START 100001;
CREATE INDEX idx_orders_status_placed ON orders (status, placed_at);
CREATE INDEX idx_orders_email ON orders (lower(email));
CREATE INDEX idx_orders_customer ON orders (customer_id);

-- A snapshot of what was bought, so later catalogue edits never change past orders.
CREATE TABLE order_lines (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    variant_id         BIGINT REFERENCES product_variants (id) ON DELETE SET NULL,
    product_id         BIGINT REFERENCES products (id) ON DELETE SET NULL,
    sku                VARCHAR(64) NOT NULL,
    title              VARCHAR(200) NOT NULL,
    size               VARCHAR(20),
    colour             VARCHAR(40),
    image_url          VARCHAR(500),
    unit_price         NUMERIC(12, 2) NOT NULL,
    quantity           INT NOT NULL CHECK (quantity > 0),
    line_total         NUMERIC(12, 2) NOT NULL,
    returned_quantity  INT NOT NULL DEFAULT 0 CHECK (returned_quantity >= 0)
);

CREATE INDEX idx_order_lines_order ON order_lines (order_id);
CREATE INDEX idx_order_lines_product ON order_lines (product_id);

-- Timeline shown to staff and (for customer-facing types) to the customer.
CREATE TABLE order_events (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    type           VARCHAR(40) NOT NULL,
    message        VARCHAR(500) NOT NULL,
    customer_visible BOOLEAN NOT NULL DEFAULT FALSE,
    staff_user_id  BIGINT REFERENCES staff_users (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_events_order ON order_events (order_id, created_at);

CREATE TABLE payments (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    provider            VARCHAR(20) NOT NULL,      -- TEST, MPESA_TEST, MPESA, STRIPE
    method              VARCHAR(10) NOT NULL,
    kind                VARCHAR(10) NOT NULL DEFAULT 'CHARGE' CHECK (kind IN ('CHARGE', 'REFUND')),
    status              VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    amount              NUMERIC(12, 2) NOT NULL,
    provider_reference  VARCHAR(120),
    phone               VARCHAR(20),
    redirect_url        VARCHAR(1000),
    failure_message     VARCHAR(300),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order ON payments (order_id);
CREATE UNIQUE INDEX idx_payments_provider_ref ON payments (provider, provider_reference) WHERE provider_reference IS NOT NULL;

-- Stock movements now include sales and cancelled-sale corrections.
ALTER TABLE inventory_adjustments DROP CONSTRAINT inventory_adjustments_reason_check;
ALTER TABLE inventory_adjustments ADD CONSTRAINT inventory_adjustments_reason_check
    CHECK (reason IN ('RECEIVED', 'RETURNED', 'DAMAGED', 'LOST', 'CORRECTION', 'OTHER', 'SOLD'));
ALTER TABLE inventory_adjustments ADD COLUMN order_id BIGINT REFERENCES orders (id);
