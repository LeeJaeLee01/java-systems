CREATE TABLE inbox_events (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
