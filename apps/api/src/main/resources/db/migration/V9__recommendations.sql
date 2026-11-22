-- Recommendations, step 1 (docs/04 AI-1, rule-based + co-occurrence; no ML yet).

-- "For you" reads one visitor's recent events, newest first.
CREATE INDEX idx_events_anonymous_time ON events (anonymous_id, occurred_at DESC);

-- "Customers also viewed / bought": how often two products are looked at by the same visitor or
-- bought in the same order. Rebuilt in full by a nightly job (RecommendationJob), so it holds both
-- directions of every pair and can be read with one indexed lookup.
CREATE TABLE product_co_occurrence (
    product_id  BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    other_id    BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    score       DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (product_id, other_id),
    CHECK (product_id <> other_id)
);

CREATE INDEX idx_co_occurrence_lookup ON product_co_occurrence (product_id, score DESC);
