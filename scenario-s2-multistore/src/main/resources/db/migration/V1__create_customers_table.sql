-- V1__create_customers_table.sql
-- Creates the customers table for S2 multi-store scenario

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Index for email lookups
CREATE INDEX idx_customers_email ON customers(email);

-- Index for name-based searches
CREATE INDEX idx_customers_name ON customers(name);

-- Index for time-based queries
CREATE INDEX idx_customers_created_at ON customers(created_at);

-- Add comments
COMMENT ON TABLE customers IS 'Customer table for S2 multi-store integration testing scenario';
COMMENT ON COLUMN customers.id IS 'Unique customer identifier (UUID)';
COMMENT ON COLUMN customers.name IS 'Customer name';
COMMENT ON COLUMN customers.email IS 'Customer email address (unique)';
COMMENT ON COLUMN customers.phone IS 'Customer phone number (optional)';
COMMENT ON COLUMN customers.address IS 'Customer address (optional)';
COMMENT ON COLUMN customers.created_at IS 'Timestamp when the customer was created';
COMMENT ON COLUMN customers.updated_at IS 'Timestamp when the customer was last updated';
