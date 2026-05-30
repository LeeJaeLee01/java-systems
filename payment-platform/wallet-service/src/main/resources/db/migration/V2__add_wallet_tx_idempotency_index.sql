CREATE UNIQUE INDEX idx_wallet_tx_wallet_ref_type
    ON wallet_transactions (wallet_id, reference_id, transaction_type);
