-- Sprint 1: staff accounts with roles (docs/03 §4) and audited stock adjustments (FR-AD-04).

CREATE TABLE staff_users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(254) NOT NULL UNIQUE,      -- stored lower-case
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(120) NOT NULL,
    role           VARCHAR(20)  NOT NULL
                   CHECK (role IN ('OWNER', 'MANAGER', 'INVENTORY', 'FULFILMENT', 'SUPPORT', 'MARKETING')),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at  TIMESTAMPTZ
);

CREATE TABLE inventory_adjustments (
    id             BIGSERIAL PRIMARY KEY,
    variant_id     BIGINT NOT NULL REFERENCES product_variants (id) ON DELETE CASCADE,
    delta          INT NOT NULL CHECK (delta <> 0),
    reason         VARCHAR(20) NOT NULL
                   CHECK (reason IN ('RECEIVED', 'RETURNED', 'DAMAGED', 'LOST', 'CORRECTION', 'OTHER')),
    note           VARCHAR(300),
    on_hand_after  INT NOT NULL,
    staff_user_id  BIGINT REFERENCES staff_users (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_adjustments_variant_time ON inventory_adjustments (variant_id, created_at DESC);
