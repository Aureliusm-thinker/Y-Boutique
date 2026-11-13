-- Two-factor sign-in (TOTP authenticator apps) for staff, and the store currency becomes Kenyan shillings.

ALTER TABLE staff_users
    ADD COLUMN mfa_secret     VARCHAR(64),          -- base32; set during setup, trusted once mfa_enabled
    ADD COLUMN mfa_enabled    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN mfa_last_step  BIGINT NOT NULL DEFAULT 0; -- last accepted 30-second step (blocks code replay)

ALTER TABLE product_variants ALTER COLUMN currency SET DEFAULT 'KES';
UPDATE product_variants SET currency = 'KES' WHERE currency <> 'KES';
