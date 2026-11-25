-- Shop catalogue: categories, products, sizes/colours, stock and photos.
-- Loaded with the "dev" profile (see application.yml); the API tests use the smaller base catalogue only.
-- Photos: Unsplash (https://unsplash.com/license), free for commercial use.

INSERT INTO categories (slug, name, position) VALUES
    ('bottoms', 'Bottoms', 4),
    ('outerwear', 'Outerwear', 5),
    ('shoes', 'Shoes', 6)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO products (slug, title, description, status, category_id, material, fit, occasion, season, style_tags,
                      seo_title, seo_description)
SELECT p.slug, p.title, p.description, 'ACTIVE', c.id, p.material, p.fit, p.occasion, p.season, p.tags,
       p.title || ' | Yvette', left(p.description, 155)
FROM (VALUES
    -- Dresses
    ('ankara-print-mini-dress', 'Ankara Print Mini Dress', 'dresses',
     'A fitted off-shoulder mini in vivid turquoise Ankara wax print, with a sweetheart neckline and short sleeves. Fully lined and made in Nairobi.',
     'Cotton wax print', 'Fitted', 'Party', 'All season', '{statement,print,african-print}'::text[]),
    ('ribbed-knit-midi-dress', 'Ribbed Knit Midi Dress', 'dresses',
     'A figure-skimming midi in soft camel rib knit with an off-the-shoulder neckline and long sleeves. Easy to dress up with heels or down with sneakers.',
     'Viscose blend', 'Fitted', 'Casual', 'All season', '{minimal,classic}'::text[]),
    ('floral-tea-dress', 'Floral Tea Dress', 'dresses',
     'A floaty tea dress in a delicate floral print with a V-neck, flutter sleeves and a tiered midi skirt.',
     'Viscose', 'Relaxed', 'Brunch', 'Summer', '{romantic,print}'::text[]),
    -- Tops
    ('silk-sleeveless-blouse', 'Silk Sleeveless Blouse', 'tops',
     'A fluid sleeveless blouse in ivory washed silk with a soft collar and button front. Wear it tucked into tailoring or loose over denim.',
     'Silk', 'Regular', 'Evening', 'All season', '{minimal,classic}'::text[]),
    ('off-shoulder-blouse', 'Off-Shoulder Blouse', 'tops',
     'A breezy off-the-shoulder blouse in white cotton with blue embroidered detailing and a gathered neckline.',
     'Cotton', 'Relaxed', 'Holiday', 'Summer', '{romantic,boho}'::text[]),
    ('fine-knit-sweater', 'Fine Knit Sweater', 'tops',
     'A soft, relaxed sweater in a cotton-cashmere blend with a ruffled stand-up neck, made for cool Nairobi mornings.',
     'Cotton cashmere', 'Relaxed', 'Casual', 'Cool season', '{minimal,classic}'::text[]),
    ('kitenge-smocked-top', 'Kitenge Smocked Top', 'tops',
     'An off-the-shoulder top in bright green and rust kitenge print with a stretchy smocked body. Pairs beautifully with white trousers.',
     'Cotton wax print', 'Fitted', 'Holiday', 'All season', '{statement,print,african-print}'::text[]),
    -- Bottoms
    ('wide-leg-linen-trousers', 'Wide-Leg Linen Trousers', 'bottoms',
     'High-waisted wide-leg trousers in white washed linen with a gently flared leg and side pockets.',
     'Linen', 'Relaxed', 'Casual', 'Summer', '{minimal,boho}'::text[]),
    ('pleated-midi-skirt', 'Pleated Midi Skirt', 'bottoms',
     'A fluid ivory pleated midi skirt that moves beautifully, with a comfortable elasticated waist.',
     'Recycled polyester', 'Regular', 'Work', 'All season', '{classic,romantic}'::text[]),
    ('high-waist-jeans', 'High-Waist Jeans', 'bottoms',
     'Slim-fit jeans in light-wash stretch cotton denim with a high rise and a full-length leg.',
     'Cotton denim', 'Regular', 'Casual', 'All season', '{classic,casual}'::text[]),
    ('tailored-trousers', 'Tailored Trousers', 'bottoms',
     'Sharp high-waisted wide-leg trousers with front pleats and a matching belt, cut from a fluid black crepe.',
     'Crepe', 'Tailored', 'Work', 'All season', '{classic,minimal}'::text[]),
    -- Outerwear
    ('oversized-blazer', 'Oversized Blazer', 'outerwear',
     'A relaxed single-breasted blazer with padded shoulders and flap pockets. The finishing piece for any outfit.',
     'Wool blend', 'Oversized', 'Work', 'All season', '{classic,minimal}'::text[]),
    ('printed-kimono', 'Printed Kimono', 'outerwear',
     'A flowing open-front kimono in a lightweight printed fabric. Throw it over a swimsuit or jeans and a tee.',
     'Viscose', 'Relaxed', 'Holiday', 'Summer', '{boho,print}'::text[]),
    ('denim-jacket', 'Denim Jacket', 'outerwear',
     'A classic trucker jacket in mid-wash cotton denim with chest pockets and metal buttons.',
     'Cotton denim', 'Regular', 'Casual', 'All season', '{classic,casual}'::text[]),
    ('trench-coat', 'Trench Coat', 'outerwear',
     'A timeless double-breasted trench with a storm flap, belted waist and water-resistant finish, made for the long rains.',
     'Cotton gabardine', 'Regular', 'Work', 'Rainy season', '{classic}'::text[]),
    -- Shoes
    ('crochet-leather-slides', 'Crochet Leather Slides', 'shoes',
     'Flat slides with a tan leather footbed and a hand-crocheted cotton strap with fringe trim. Handmade in Kenya.',
     'Leather', NULL, 'Casual', 'Summer', '{minimal,boho}'::text[]),
    ('strappy-heeled-sandals', 'Strappy Heeled Sandals', 'shoes',
     'Glossy black strappy sandals on a slim 10 cm heel, with an ankle strap and back zip.',
     'Leather', NULL, 'Evening', 'All season', '{classic}'::text[]),
    ('white-leather-sneakers', 'White Leather Sneakers', 'shoes',
     'Clean low-top sneakers in smooth white leather with a cushioned sole. Go with everything.',
     'Leather', NULL, 'Casual', 'All season', '{minimal,casual}'::text[]),
    -- Accessories
    ('sisal-market-tote', 'Sisal Market Tote', 'accessories',
     'A roomy hand-woven sisal tote with tan leather handles, made by women''s cooperatives in Machakos. Choose from five bright colours.',
     'Sisal', NULL, 'Holiday', 'All season', '{boho,african-print}'::text[]),
    ('beaded-statement-necklace', 'Beaded Statement Necklace', 'accessories',
     'A bold collar necklace hand-beaded by Maasai artisans in Kajiado, in bright traditional colours.',
     'Glass beads', NULL, 'Wedding guest', 'All season', '{statement,african-print}'::text[]),
    ('gold-hoop-earrings', 'Gold Hoop Earrings', 'accessories',
     'Everyday medium hoops in 18k gold-plated brass. Lightweight, hypoallergenic and tarnish-resistant.',
     'Gold-plated brass', NULL, 'Everyday', 'All season', '{classic,minimal}'::text[]),
    ('leather-crossbody-bag', 'Leather Crossbody Bag', 'accessories',
     'A compact crossbody bag in brown croc-embossed leather with a long strap and a clasp-front flap.',
     'Leather', NULL, 'Everyday', 'All season', '{classic,minimal}'::text[])
) AS p(slug, title, category, description, material, fit, occasion, season, tags)
JOIN categories c ON c.slug = p.category
ON CONFLICT (slug) DO NOTHING;

-- Sizes, colours and prices (KSh). compare_at = was-price for items on sale.
INSERT INTO product_variants (product_id, sku, size, colour, price, compare_at_price, currency, position)
SELECT p.id, v.sku, v.size, v.colour, v.price, v.compare_at, 'KES', v.pos
FROM (VALUES
    ('ankara-print-mini-dress', 'APD-TRQ-S', 'S', 'Turquoise', 9800, NULL::numeric, 1),
    ('ankara-print-mini-dress', 'APD-TRQ-M', 'M', 'Turquoise', 9800, NULL, 2),
    ('ankara-print-mini-dress', 'APD-TRQ-L', 'L', 'Turquoise', 9800, NULL, 3),
    ('ankara-print-mini-dress', 'APD-TRQ-XL', 'XL', 'Turquoise', 9800, NULL, 4),
    ('ribbed-knit-midi-dress', 'RKM-CML-S', 'S', 'Camel', 8500, NULL, 1),
    ('ribbed-knit-midi-dress', 'RKM-CML-M', 'M', 'Camel', 8500, NULL, 2),
    ('ribbed-knit-midi-dress', 'RKM-CML-L', 'L', 'Camel', 8500, NULL, 3),
    ('floral-tea-dress', 'FTD-BLU-XS', 'XS', 'Blue floral', 7200, 8900, 1),
    ('floral-tea-dress', 'FTD-BLU-S', 'S', 'Blue floral', 7200, 8900, 2),
    ('floral-tea-dress', 'FTD-BLU-M', 'M', 'Blue floral', 7200, 8900, 3),
    ('floral-tea-dress', 'FTD-BLU-L', 'L', 'Blue floral', 7200, 8900, 4),
    ('silk-sleeveless-blouse', 'SSB-IVR-S', 'S', 'Ivory', 6900, NULL, 1),
    ('silk-sleeveless-blouse', 'SSB-IVR-M', 'M', 'Ivory', 6900, NULL, 2),
    ('silk-sleeveless-blouse', 'SSB-IVR-L', 'L', 'Ivory', 6900, NULL, 3),
    ('off-shoulder-blouse', 'OSB-WHT-S', 'S', 'White', 4800, NULL, 1),
    ('off-shoulder-blouse', 'OSB-WHT-M', 'M', 'White', 4800, NULL, 2),
    ('off-shoulder-blouse', 'OSB-WHT-L', 'L', 'White', 4800, NULL, 3),
    ('fine-knit-sweater', 'FKS-CRM-S', 'S', 'Cream', 7500, NULL, 1),
    ('fine-knit-sweater', 'FKS-CRM-M', 'M', 'Cream', 7500, NULL, 2),
    ('fine-knit-sweater', 'FKS-CRM-L', 'L', 'Cream', 7500, NULL, 3),
    ('kitenge-smocked-top', 'KST-GRN-S', 'S', 'Green', 4500, NULL, 1),
    ('kitenge-smocked-top', 'KST-GRN-M', 'M', 'Green', 4500, NULL, 2),
    ('kitenge-smocked-top', 'KST-GRN-L', 'L', 'Green', 4500, NULL, 3),
    ('kitenge-smocked-top', 'KST-GRN-XL', 'XL', 'Green', 4500, NULL, 4),
    ('wide-leg-linen-trousers', 'WLT-WHT-S', 'S', 'White', 6800, NULL, 1),
    ('wide-leg-linen-trousers', 'WLT-WHT-M', 'M', 'White', 6800, NULL, 2),
    ('wide-leg-linen-trousers', 'WLT-WHT-L', 'L', 'White', 6800, NULL, 3),
    ('pleated-midi-skirt', 'PMS-IVR-S', 'S', 'Ivory', 5900, NULL, 1),
    ('pleated-midi-skirt', 'PMS-IVR-M', 'M', 'Ivory', 5900, NULL, 2),
    ('pleated-midi-skirt', 'PMS-IVR-L', 'L', 'Ivory', 5900, NULL, 3),
    ('high-waist-jeans', 'HWJ-LBL-26', '26', 'Light blue', 6500, NULL, 1),
    ('high-waist-jeans', 'HWJ-LBL-28', '28', 'Light blue', 6500, NULL, 2),
    ('high-waist-jeans', 'HWJ-LBL-30', '30', 'Light blue', 6500, NULL, 3),
    ('high-waist-jeans', 'HWJ-LBL-32', '32', 'Light blue', 6500, NULL, 4),
    ('tailored-trousers', 'TLT-BLK-S', 'S', 'Black', 7900, NULL, 1),
    ('tailored-trousers', 'TLT-BLK-M', 'M', 'Black', 7900, NULL, 2),
    ('tailored-trousers', 'TLT-BLK-L', 'L', 'Black', 7900, NULL, 3),
    ('oversized-blazer', 'OVB-BGE-S', 'S', 'Beige', 13500, NULL, 1),
    ('oversized-blazer', 'OVB-BGE-M', 'M', 'Beige', 13500, NULL, 2),
    ('oversized-blazer', 'OVB-BGE-L', 'L', 'Beige', 13500, NULL, 3),
    ('printed-kimono', 'PKM-MLT-OS', 'One size', 'Multicolour', 5500, 6900, 1),
    ('denim-jacket', 'DNJ-BLU-S', 'S', 'Mid blue', 8900, NULL, 1),
    ('denim-jacket', 'DNJ-BLU-M', 'M', 'Mid blue', 8900, NULL, 2),
    ('denim-jacket', 'DNJ-BLU-L', 'L', 'Mid blue', 8900, NULL, 3),
    ('trench-coat', 'TRC-KHK-S', 'S', 'Khaki', 16500, NULL, 1),
    ('trench-coat', 'TRC-KHK-M', 'M', 'Khaki', 16500, NULL, 2),
    ('trench-coat', 'TRC-KHK-L', 'L', 'Khaki', 16500, NULL, 3),
    ('crochet-leather-slides', 'CLS-TAN-37', '37', 'Tan', 4200, NULL, 1),
    ('crochet-leather-slides', 'CLS-TAN-38', '38', 'Tan', 4200, NULL, 2),
    ('crochet-leather-slides', 'CLS-TAN-39', '39', 'Tan', 4200, NULL, 3),
    ('crochet-leather-slides', 'CLS-TAN-40', '40', 'Tan', 4200, NULL, 4),
    ('crochet-leather-slides', 'CLS-TAN-41', '41', 'Tan', 4200, NULL, 5),
    ('strappy-heeled-sandals', 'SHS-BLK-37', '37', 'Black', 7800, NULL, 1),
    ('strappy-heeled-sandals', 'SHS-BLK-38', '38', 'Black', 7800, NULL, 2),
    ('strappy-heeled-sandals', 'SHS-BLK-39', '39', 'Black', 7800, NULL, 3),
    ('strappy-heeled-sandals', 'SHS-BLK-40', '40', 'Black', 7800, NULL, 4),
    ('white-leather-sneakers', 'WLS-WHT-37', '37', 'White', 9500, NULL, 1),
    ('white-leather-sneakers', 'WLS-WHT-38', '38', 'White', 9500, NULL, 2),
    ('white-leather-sneakers', 'WLS-WHT-39', '39', 'White', 9500, NULL, 3),
    ('white-leather-sneakers', 'WLS-WHT-40', '40', 'White', 9500, NULL, 4),
    ('white-leather-sneakers', 'WLS-WHT-41', '41', 'White', 9500, NULL, 5),
    ('sisal-market-tote', 'SMT-CRL-OS', 'One size', 'Coral', 3800, NULL, 1),
    ('sisal-market-tote', 'SMT-PLM-OS', 'One size', 'Plum', 3800, NULL, 2),
    ('sisal-market-tote', 'SMT-FCH-OS', 'One size', 'Fuchsia', 3800, NULL, 3),
    ('sisal-market-tote', 'SMT-YLW-OS', 'One size', 'Yellow', 3800, NULL, 4),
    ('sisal-market-tote', 'SMT-WHT-OS', 'One size', 'White', 3800, NULL, 5),
    ('beaded-statement-necklace', 'BSN-MLT-OS', 'One size', 'Multicolour', 3200, NULL, 1),
    ('gold-hoop-earrings', 'GHE-GLD-OS', 'One size', 'Gold', 2900, NULL, 1),
    ('leather-crossbody-bag', 'LCB-BRN-OS', 'One size', 'Brown', 8800, 10500, 1)
) AS v(slug, sku, size, colour, price, compare_at, pos)
JOIN products p ON p.slug = v.slug
ON CONFLICT (sku) DO NOTHING;

-- Stock: mostly healthy, a few "only N left", one size sold out.
INSERT INTO inventory_levels (variant_id, on_hand)
SELECT v.id,
       CASE v.sku
           WHEN 'APD-TRQ-XL' THEN 2
           WHEN 'RKM-CML-S' THEN 3
           WHEN 'SSB-IVR-S' THEN 0
           WHEN 'TRC-KHK-L' THEN 1
           WHEN 'SHS-BLK-40' THEN 0
           WHEN 'BSN-MLT-OS' THEN 4
           ELSE 8 + (v.id % 9)::int
       END
FROM product_variants v
WHERE NOT EXISTS (SELECT 1 FROM inventory_levels i WHERE i.variant_id = v.id);

-- Photos for every product (replaces the original placeholder images).
DELETE FROM product_images
WHERE product_id IN (SELECT id FROM products WHERE slug IN
    ('linen-wrap-dress', 'silk-slip-dress', 'cotton-poplin-shirt', 'woven-straw-tote'))
  AND url LIKE 'https://placehold.co/%';

INSERT INTO product_images (product_id, url, alt_text, position)
SELECT p.id, i.url, i.alt, 1
FROM (VALUES
    ('linen-wrap-dress', 'https://images.unsplash.com/photo-1600187723041-97a0ff3ecd7f?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Woman in a taupe linen dress with a tie waist walking up stone steps'),
    ('silk-slip-dress', 'https://images.unsplash.com/photo-1750064161409-5cc34e5f7367?w=1200&h=1500&fit=crop&q=80&auto=format&crop=focalpoint&fp-x=0.62&fp-y=0.5', 'Black silk slip dress with a cowl neckline'),
    ('ankara-print-mini-dress', 'https://images.unsplash.com/photo-1663044022903-caa195cb5b2e?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Woman in a turquoise Ankara print off-shoulder mini dress by the sea'),
    ('ribbed-knit-midi-dress', 'https://images.unsplash.com/photo-1726606118470-5d4b220d54a0?w=1200&h=1500&fit=crop&q=80&auto=format&crop=focalpoint&fp-x=0.36&fp-y=0.5', 'Woman in a camel ribbed knit off-shoulder midi dress'),
    ('floral-tea-dress', 'https://images.unsplash.com/photo-1517970640957-23d07d5ed08c?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Woman in a white and blue floral tea dress in a meadow'),
    ('cotton-poplin-shirt', 'https://images.unsplash.com/photo-1686485237008-732142b6fcec?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Oversized white cotton poplin shirt'),
    ('silk-sleeveless-blouse', 'https://images.unsplash.com/photo-1770294758906-c8762abb2c8b?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Ivory silk sleeveless blouse with a collar'),
    ('off-shoulder-blouse', 'https://images.unsplash.com/photo-1642031156953-8d300ffcd687?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Woman in a white embroidered off-shoulder blouse'),
    ('fine-knit-sweater', 'https://images.unsplash.com/photo-1759229874779-740df3e9a8de?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Cream knit sweater with a ruffled neck'),
    ('kitenge-smocked-top', 'https://images.unsplash.com/photo-1664151099736-1ac6365a25aa?w=1200&h=1500&fit=crop&q=80&auto=format&crop=focalpoint&fp-x=0.72&fp-y=0.55', 'Green and rust kitenge print smocked off-shoulder top'),
    ('wide-leg-linen-trousers', 'https://images.unsplash.com/photo-1704775991545-fc0507822fea?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'White wide-leg linen trousers'),
    ('pleated-midi-skirt', 'https://images.unsplash.com/photo-1762342681114-f8bd608e0ce6?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Ivory pleated midi skirt'),
    ('high-waist-jeans', 'https://images.unsplash.com/photo-1475178626620-a4d074967452?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Light blue high-waist jeans'),
    ('tailored-trousers', 'https://images.unsplash.com/photo-1767631338127-8cd80ee2f9df?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Black belted wide-leg tailored trousers'),
    ('oversized-blazer', 'https://images.unsplash.com/photo-1747817230321-4ad317ac0809?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Woman in a beige oversized blazer'),
    ('printed-kimono', 'https://images.unsplash.com/photo-1575877317669-d037ec99ecf9?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Woman in a long tropical print kimono'),
    ('denim-jacket', 'https://images.unsplash.com/photo-1602693130669-9c9e53cc320c?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Woman in a mid-blue denim jacket'),
    ('trench-coat', 'https://images.unsplash.com/photo-1676716105765-e19fe6a01851?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Woman in a khaki belted trench coat'),
    ('crochet-leather-slides', 'https://images.unsplash.com/photo-1627388484741-74dcc56ec343?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Tan leather slides with cream crochet straps on sand'),
    ('strappy-heeled-sandals', 'https://images.unsplash.com/photo-1789110519471-74ac17dd1cf6?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Black strappy heeled sandals'),
    ('white-leather-sneakers', 'https://images.unsplash.com/photo-1636262900147-fb56689b61c5?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'White leather low-top sneakers'),
    ('woven-straw-tote', 'https://images.unsplash.com/photo-1776219189008-b80cc6081517?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Natural woven straw tote bag hanging on a hook'),
    ('sisal-market-tote', 'https://images.unsplash.com/photo-1768734837394-182f596e84ac?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Sisal market totes with leather handles in coral, plum, fuchsia, yellow and white'),
    ('beaded-statement-necklace', 'https://images.unsplash.com/photo-1606094092594-93c09d76b721?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Maasai woman wearing a colourful beaded collar necklace'),
    ('gold-hoop-earrings', 'https://images.unsplash.com/photo-1708220040824-b273dd0a17cc?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Chunky gold hoop earrings'),
    ('leather-crossbody-bag', 'https://images.unsplash.com/photo-1594633313593-bab3825d0caf?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy', 'Brown croc-embossed leather crossbody bag')
) AS i(slug, url, alt)
JOIN products p ON p.slug = i.slug
WHERE NOT EXISTS (SELECT 1 FROM product_images x WHERE x.product_id = p.id);

-- "Complete the look" pairings.
INSERT INTO product_relations (product_id, related_product_id, position)
SELECT a.id, b.id, r.pos
FROM (VALUES
    ('ankara-print-mini-dress', 'beaded-statement-necklace', 0),
    ('ankara-print-mini-dress', 'strappy-heeled-sandals', 1),
    ('ribbed-knit-midi-dress', 'white-leather-sneakers', 0),
    ('ribbed-knit-midi-dress', 'denim-jacket', 1),
    ('floral-tea-dress', 'crochet-leather-slides', 0),
    ('floral-tea-dress', 'sisal-market-tote', 1),
    ('silk-sleeveless-blouse', 'tailored-trousers', 0),
    ('silk-sleeveless-blouse', 'oversized-blazer', 1),
    ('kitenge-smocked-top', 'tailored-trousers', 0),
    ('kitenge-smocked-top', 'gold-hoop-earrings', 1),
    ('off-shoulder-blouse', 'wide-leg-linen-trousers', 0),
    ('wide-leg-linen-trousers', 'crochet-leather-slides', 0),
    ('pleated-midi-skirt', 'fine-knit-sweater', 0),
    ('high-waist-jeans', 'cotton-poplin-shirt', 0),
    ('oversized-blazer', 'tailored-trousers', 0),
    ('trench-coat', 'leather-crossbody-bag', 0),
    ('silk-slip-dress', 'strappy-heeled-sandals', 1),
    ('linen-wrap-dress', 'crochet-leather-slides', 1)
) AS r(a, b, pos)
JOIN products a ON a.slug = r.a
JOIN products b ON b.slug = r.b
ON CONFLICT DO NOTHING;
