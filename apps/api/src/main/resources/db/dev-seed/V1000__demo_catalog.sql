-- Demo catalogue for local development only (loaded by the "dev" profile).

INSERT INTO categories (slug, name, position) VALUES
    ('dresses', 'Dresses', 1),
    ('tops', 'Tops', 2),
    ('accessories', 'Accessories', 3);

INSERT INTO products (slug, title, description, status, category_id, material, fit, occasion, season, style_tags) VALUES
    ('linen-wrap-dress', 'Linen Wrap Dress',
     'A breezy wrap dress in washed linen with a tie waist and flutter sleeves.',
     'ACTIVE', (SELECT id FROM categories WHERE slug = 'dresses'),
     'Linen', 'Relaxed', 'Casual', 'Summer', '{minimal,boho}'),
    ('silk-slip-dress', 'Silk Slip Dress',
     'Bias-cut slip dress in mulberry silk. Pairs with a blazer by day or heels by night.',
     'ACTIVE', (SELECT id FROM categories WHERE slug = 'dresses'),
     'Silk', 'Regular', 'Evening', 'All season', '{classic,minimal}'),
    ('cotton-poplin-shirt', 'Cotton Poplin Shirt',
     'Crisp oversized shirt in organic cotton poplin.',
     'ACTIVE', (SELECT id FROM categories WHERE slug = 'tops'),
     'Cotton', 'Relaxed', 'Work', 'All season', '{classic}'),
    ('woven-straw-tote', 'Woven Straw Tote',
     'Hand-woven straw tote with leather handles.',
     'ACTIVE', (SELECT id FROM categories WHERE slug = 'accessories'),
     'Straw', NULL, 'Holiday', 'Summer', '{boho}');

INSERT INTO product_variants (product_id, sku, size, colour, price, compare_at_price, position)
SELECT p.id, v.sku, v.size, v.colour, v.price, v.compare_at, v.pos
FROM (VALUES
    ('linen-wrap-dress',    'LWD-SAND-S',  'S',  'Sand',   129.00, NULL::numeric, 1),
    ('linen-wrap-dress',    'LWD-SAND-M',  'M',  'Sand',   129.00, NULL,          2),
    ('linen-wrap-dress',    'LWD-SAND-L',  'L',  'Sand',   129.00, NULL,          3),
    ('silk-slip-dress',     'SSD-BLK-S',   'S',  'Black',  189.00, 229.00,        1),
    ('silk-slip-dress',     'SSD-BLK-M',   'M',  'Black',  189.00, 229.00,        2),
    ('cotton-poplin-shirt', 'CPS-WHT-M',   'M',  'White',   79.00, NULL,          1),
    ('cotton-poplin-shirt', 'CPS-WHT-L',   'L',  'White',   79.00, NULL,          2),
    ('woven-straw-tote',    'WST-NAT-OS',  'One size', 'Natural', 65.00, NULL,    1)
) AS v(slug, sku, size, colour, price, compare_at, pos)
JOIN products p ON p.slug = v.slug;

INSERT INTO inventory_levels (variant_id, on_hand)
SELECT id, CASE WHEN sku = 'SSD-BLK-S' THEN 0 ELSE 10 END FROM product_variants;

INSERT INTO product_images (product_id, url, alt_text, position)
SELECT p.id, i.url, i.alt, 1
FROM (VALUES
    ('linen-wrap-dress',    'https://placehold.co/800x1000/e8dfd3/5b4a3a.png?text=Linen+Wrap+Dress',    'Sand linen wrap dress'),
    ('silk-slip-dress',     'https://placehold.co/800x1000/2b2b2b/f2efe9.png?text=Silk+Slip+Dress',     'Black silk slip dress'),
    ('cotton-poplin-shirt', 'https://placehold.co/800x1000/f4f4f2/3a3a3a.png?text=Poplin+Shirt',        'White cotton poplin shirt'),
    ('woven-straw-tote',    'https://placehold.co/800x1000/d9c39a/4a3b22.png?text=Straw+Tote',          'Natural woven straw tote')
) AS i(slug, url, alt)
JOIN products p ON p.slug = i.slug;
