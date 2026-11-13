-- Sprint 5: returns (Epic E6), editable content and legal pages (E7-2, E8-3).
-- Reviews were created in V7.

CREATE TABLE returns (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    status         VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
                   CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'RECEIVED', 'REFUNDED')),
    comment        VARCHAR(1000),
    staff_comment  VARCHAR(500),
    refund_amount  NUMERIC(12, 2),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_returns_status ON returns (status, created_at);
CREATE INDEX idx_returns_order ON returns (order_id);

CREATE TABLE return_lines (
    id             BIGSERIAL PRIMARY KEY,
    return_id      BIGINT NOT NULL REFERENCES returns (id) ON DELETE CASCADE,
    order_line_id  BIGINT NOT NULL REFERENCES order_lines (id) ON DELETE CASCADE,
    quantity       INT NOT NULL CHECK (quantity > 0),
    -- Size problems are tracked separately: they feed size advice and fit recommendations later (docs/04).
    reason         VARCHAR(20) NOT NULL
                   CHECK (reason IN ('TOO_SMALL', 'TOO_LARGE', 'NOT_AS_DESCRIBED', 'DAMAGED', 'CHANGED_MIND', 'OTHER')),
    restocked      BOOLEAN NOT NULL DEFAULT FALSE
);

-- Pages written by staff: legal pages, delivery info, size guide… Draft and published versions,
-- so changes can be previewed before going live (FR-AD-10).
CREATE TABLE content_pages (
    id                BIGSERIAL PRIMARY KEY,
    slug              VARCHAR(80) NOT NULL UNIQUE,
    title             VARCHAR(200) NOT NULL,
    body              TEXT NOT NULL DEFAULT '',
    draft_title       VARCHAR(200),
    draft_body        TEXT,
    published         BOOLEAN NOT NULL DEFAULT FALSE,
    published_at      TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        BIGINT REFERENCES staff_users (id)
);

-- Home page sections and the announcement bar, as JSON (draft + published).
CREATE TABLE site_content (
    key           VARCHAR(40) PRIMARY KEY,
    draft         JSONB NOT NULL,
    published     JSONB NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by    BIGINT REFERENCES staff_users (id)
);

INSERT INTO site_content (key, draft, published) VALUES
('home', '{
  "heroEyebrow": "New season",
  "heroTitle": "Curated for you.",
  "heroText": "Considered pieces in natural fabrics, chosen to be worn and loved for years.",
  "heroCta": "Shop the collection",
  "heroLink": "/shop",
  "heroImage": null,
  "featuredCategory": null,
  "featuredTitle": "New arrivals"
}', '{
  "heroEyebrow": "New season",
  "heroTitle": "Curated for you.",
  "heroText": "Considered pieces in natural fabrics, chosen to be worn and loved for years.",
  "heroCta": "Shop the collection",
  "heroLink": "/shop",
  "heroImage": null,
  "featuredCategory": null,
  "featuredTitle": "New arrivals"
}'),
('announcement', '{"text": "Free Nairobi delivery on orders over KSh 10,000", "link": "/pages/shipping", "enabled": true}',
                 '{"text": "Free Nairobi delivery on orders over KSh 10,000", "link": "/pages/shipping", "enabled": true}');

INSERT INTO content_pages (slug, title, body, published, published_at) VALUES
('shipping', 'Delivery', '## Where we deliver
We deliver across Kenya.

- **Nairobi delivery** — 1–2 working days, KSh 300, free over KSh 10,000.
- **Countrywide courier** — 2–4 working days, KSh 600, free over KSh 15,000.
- **Same-day Nairobi** — order by 12 noon (Mon–Sat), KSh 800.
- **Collect from the boutique** — free, ready the next working day.

## Tracking
When your order ships we email you the courier and tracking number. You can also follow it from your order page.', TRUE, now()),
('returns', 'Returns & refunds', '## Our promise
If something isn''t right, you can return it within **14 days of delivery**.

- Items must be unworn, with tags attached.
- Start a return from your order page (or [find your order](/order/lookup) if you checked out as a guest).
- Tell us if the size was the problem — it helps us improve our size advice.

## Refunds
Once we receive and check your return, we refund the item price to the way you paid: M-Pesa refunds arrive on your line, card refunds take 5–10 working days.', TRUE, now()),
('privacy', 'Privacy policy', '## Who we are
Yvette is a boutique based in Nairobi, Kenya. We process personal data in line with the **Kenya Data Protection Act, 2019**.

## What we collect
- Details you give us: name, email, phone, delivery addresses, and orders.
- Payment details are handled by M-Pesa (Safaricom) and our card processor; we never see your full card number.
- With your consent, how you browse the shop (pages and products viewed) to improve it and, later, to personalise recommendations.

## Your rights
You can download or delete your data at any time from **My account → Privacy & data**, and change your cookie choice at any time.

## Contact
Questions about your data: privacy@yvette.co.ke', TRUE, now()),
('terms', 'Terms of sale', '## Orders
An order is confirmed once payment is received. Prices are in Kenyan shillings and include VAT where applicable.

## Stock
We hold items for 30 minutes while you pay. If payment isn''t completed in that time, the items are released.

## Delivery and returns
See our [delivery](/pages/shipping) and [returns](/pages/returns) pages.', TRUE, now()),
('cookies', 'Cookies', '## Essential cookies
We use a few cookies the shop needs to work: your bag, your sign-in, and your cookie choice.

## Analytics (only with your consent)
If you accept, we record which pages and products you view, on our own servers. We use this to improve the shop and, later, to recommend pieces you may like. We don''t use third-party advertising cookies.', TRUE, now()),
('size-guide', 'Size guide', '## How to measure
- **Bust:** around the fullest part of your chest.
- **Waist:** around the narrowest part of your waist.
- **Hips:** around the fullest part of your hips.

## Sizes (cm)
- **XS (6):** bust 80–84, waist 62–66, hips 86–90
- **S (8):** bust 84–88, waist 66–70, hips 90–94
- **M (10):** bust 88–94, waist 70–76, hips 94–100
- **L (12):** bust 94–100, waist 76–82, hips 100–106
- **XL (14):** bust 100–106, waist 82–88, hips 106–112

Between sizes? Size up for a relaxed fit. Each product page also shows how others found the fit.', TRUE, now());

UPDATE content_pages SET draft_title = title, draft_body = body;
