CREATE TABLE inbox_events (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE fraud_alerts (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    user_id UUID NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
