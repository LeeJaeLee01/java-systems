ALTER TABLE payment_transactions RENAME TO payments;

ALTER TABLE outbox_events
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_error TEXT,
    ADD COLUMN next_retry_at TIMESTAMPTZ,
    ADD COLUMN claimed_at TIMESTAMPTZ;

CREATE INDEX idx_outbox_events_pending_dispatch
    ON outbox_events (status, next_retry_at, created_at)
    WHERE status = 'PENDING';
