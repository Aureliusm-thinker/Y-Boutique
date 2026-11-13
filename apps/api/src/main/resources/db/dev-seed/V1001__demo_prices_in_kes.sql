-- Demo catalogue priced in Kenyan shillings (local development only).
UPDATE product_variants v SET price = p.price, compare_at_price = p.compare_at, currency = 'KES'
FROM (VALUES
    ('LWD-SAND-S', 12900, NULL::numeric), ('LWD-SAND-M', 12900, NULL), ('LWD-SAND-L', 12900, NULL),
    ('SSD-BLK-S', 18900, 22900), ('SSD-BLK-M', 18900, 22900),
    ('CPS-WHT-M', 7900, NULL), ('CPS-WHT-L', 7900, NULL),
    ('WST-NAT-OS', 6500, NULL)
) AS p(sku, price, compare_at)
WHERE v.sku = p.sku;
