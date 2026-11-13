-- Demo "complete the look" pairs (local development only).
INSERT INTO product_relations (product_id, related_product_id, position)
SELECT a.id, b.id, r.pos
FROM (VALUES
    ('linen-wrap-dress', 'woven-straw-tote', 0),
    ('silk-slip-dress', 'cotton-poplin-shirt', 0),
    ('cotton-poplin-shirt', 'woven-straw-tote', 0)
) AS r(a, b, pos)
JOIN products a ON a.slug = r.a
JOIN products b ON b.slug = r.b;
