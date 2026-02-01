-- V1__create_orders_table.sql
-- Creates the orders table for S1 scenario

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    amount DECIMAL(19,2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Index for status-based queries
CREATE INDEX idx_orders_status ON orders(status);

-- Index for time-based queries
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- Add comments
COMMENT ON TABLE orders IS 'Order table for S1 integration testing scenario';
COMMENT ON COLUMN orders.id IS 'Unique order identifier';
COMMENT ON COLUMN orders.customer_name IS 'Name of the customer placing the order';
COMMENT ON COLUMN orders.product_name IS 'Name of the ordered product';
COMMENT ON COLUMN orders.quantity IS 'Quantity ordered (must be positive)';
COMMENT ON COLUMN orders.amount IS 'Total amount (must be non-negative)';
COMMENT ON COLUMN orders.status IS 'Current order status (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)';
