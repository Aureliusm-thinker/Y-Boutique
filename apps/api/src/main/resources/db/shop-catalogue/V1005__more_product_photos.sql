-- Extra gallery photos for products where more shots of the very same item exist (same photoshoot).
-- Photos: Unsplash (https://unsplash.com/license), free for commercial use.

-- Kitenge Smocked Top: the front-facing shot becomes the main photo; the earlier close crop moves to the end.
UPDATE product_images SET position = 3
WHERE product_id = (SELECT id FROM products WHERE slug = 'kitenge-smocked-top')
  AND url LIKE 'https://images.unsplash.com/photo-1664151099736-1ac6365a25aa%';

INSERT INTO product_images (product_id, url, alt_text, position)
SELECT p.id, i.url, i.alt, i.pos
FROM (VALUES
    ('kitenge-smocked-top',
     'https://images.unsplash.com/photo-1664151100152-333a5c85efbe?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy',
     'Woman seated in a garden wearing the green and rust kitenge smocked off-shoulder top', 1),
    ('kitenge-smocked-top',
     'https://images.unsplash.com/photo-1664151101047-4a82977348fb?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy',
     'Close-up of the smocked kitenge print and frilled off-shoulder edge', 2),
    ('ankara-print-mini-dress',
     'https://images.unsplash.com/photo-1663044022894-68a5e6bccd64?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy',
     'The turquoise Ankara print mini dress worn on the rocks by the sea', 2),
    ('oversized-blazer',
     'https://images.unsplash.com/photo-1747817330506-21f40b1d4400?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy',
     'Full-length front view of the beige oversized blazer', 2),
    ('oversized-blazer',
     'https://images.unsplash.com/photo-1747815065172-a3234582223e?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy',
     'Side view of the beige oversized blazer showing its length', 3),
    ('oversized-blazer',
     'https://images.unsplash.com/photo-1747817230318-f17da1d7b766?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy',
     'Close-up of the blazer lapel, flower pin and chest pocket', 4),
    ('printed-kimono',
     'https://images.unsplash.com/photo-1575877317157-f95d83dbffca?w=1200&h=1500&fit=crop&q=80&auto=format&crop=entropy',
     'Full-length view of the tropical print kimono worn walking down a corridor', 2)
) AS i(slug, url, alt, pos)
JOIN products p ON p.slug = i.slug
WHERE NOT EXISTS (SELECT 1 FROM product_images x WHERE x.product_id = p.id AND x.url = i.url);
