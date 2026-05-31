ALTER TABLE fraud_alerts
    ADD COLUMN payment_id UUID,
    ADD COLUMN alert_type VARCHAR(64) NOT NULL DEFAULT 'HIGH_FREQUENCY',
    ADD COLUMN transaction_count INT;

CREATE INDEX idx_fraud_alerts_user_id_created_at ON fraud_alerts(user_id, created_at);
