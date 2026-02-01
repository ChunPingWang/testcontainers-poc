-- V1__create_transactions_table.sql
-- Creates the transactions table for S4 CDC scenario
-- Configured with REPLICA IDENTITY FULL for complete CDC support

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    account_id VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL CHECK (amount >= 0),
    balance DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Set REPLICA IDENTITY FULL for CDC (Change Data Capture)
-- This ensures UPDATE and DELETE operations include the full row data
-- Required for Debezium to capture before-state in change events
ALTER TABLE transactions REPLICA IDENTITY FULL;

-- Index for account-based queries
CREATE INDEX idx_transactions_account_id ON transactions(account_id);

-- Index for type-based queries
CREATE INDEX idx_transactions_type ON transactions(type);

-- Index for time-based queries
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

-- Composite index for account and time queries
CREATE INDEX idx_transactions_account_created ON transactions(account_id, created_at DESC);

-- Add comments
COMMENT ON TABLE transactions IS 'Financial transactions table for S4 CDC scenario';
COMMENT ON COLUMN transactions.id IS 'Unique transaction identifier';
COMMENT ON COLUMN transactions.account_id IS 'Account identifier for the transaction';
COMMENT ON COLUMN transactions.type IS 'Transaction type (DEPOSIT, WITHDRAWAL, TRANSFER)';
COMMENT ON COLUMN transactions.amount IS 'Transaction amount (must be non-negative)';
COMMENT ON COLUMN transactions.balance IS 'Account balance after transaction';
COMMENT ON COLUMN transactions.created_at IS 'Timestamp when the transaction was created';
