ALTER TABLE payments
    ADD COLUMN stripe_payment_intent_id VARCHAR(64);

CREATE UNIQUE INDEX idx_payments_stripe_payment_intent_id
    ON payments (stripe_payment_intent_id)
    WHERE stripe_payment_intent_id IS NOT NULL;

CREATE TABLE reconciliation_runs (
    id UUID PRIMARY KEY,
    run_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    internal_count INT NOT NULL DEFAULT 0,
    partner_count INT NOT NULL DEFAULT 0,
    matched_count INT NOT NULL DEFAULT 0,
    discrepancy_count INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    CONSTRAINT uq_reconciliation_runs_run_date UNIQUE (run_date)
);

CREATE TABLE stripe_reconciliation_records (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES reconciliation_runs (id),
    stripe_payment_intent_id VARCHAR(64) NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    status VARCHAR(32) NOT NULL,
    wallet_id UUID,
    user_id UUID,
    stripe_created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_stripe_reconciliation_records_run_id ON stripe_reconciliation_records (run_id);

CREATE TABLE reconciliation_discrepancies (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES reconciliation_runs (id),
    discrepancy_type VARCHAR(32) NOT NULL,
    internal_payment_id UUID,
    stripe_payment_intent_id VARCHAR(64),
    internal_amount NUMERIC(19, 4),
    partner_amount NUMERIC(19, 4),
    internal_status VARCHAR(32),
    partner_status VARCHAR(32),
    detected_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reconciliation_discrepancies_run_id ON reconciliation_discrepancies (run_id);
