-- Demo discount codes (local development only).
INSERT INTO promotions (code, name, type, value, min_subtotal) VALUES
    ('KARIBU10', 'Welcome 10% off', 'PERCENT', 10, NULL),
    ('FREESHIP', 'Free delivery over KSh 5,000', 'FREE_SHIPPING', 0, 5000);
