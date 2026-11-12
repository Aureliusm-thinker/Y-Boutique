-- First-party behavioural events: the data foundation for AI/ML (docs/04 §3.1).
-- Append-only. Only written when the visitor has given analytics consent.

CREATE TABLE events (
    id             BIGSERIAL PRIMARY KEY,
    event_id       UUID NOT NULL UNIQUE,
    name           VARCHAR(64) NOT NULL,
    anonymous_id   VARCHAR(64) NOT NULL,
    user_id        BIGINT,
    session_id     VARCHAR(64),
    properties     JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at    TIMESTAMPTZ NOT NULL,
    received_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_name_time ON events (name, occurred_at);
CREATE INDEX idx_events_anonymous ON events (anonymous_id);
