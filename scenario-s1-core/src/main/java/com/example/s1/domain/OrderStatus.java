package com.example.s1.domain;

/**
 * Order status enumeration.
 * Represents the lifecycle states of an order.
 *
 * State transitions:
 * PENDING → CONFIRMED → SHIPPED → DELIVERED
 *     ↓
 * CANCELLED
 */
public enum OrderStatus {
    /**
     * Order has been created but not yet confirmed.
     */
    PENDING,

    /**
     * Order has been confirmed and is being processed.
     */
    CONFIRMED,

    /**
     * Order has been shipped.
     */
    SHIPPED,

    /**
     * Order has been delivered to the customer.
     */
    DELIVERED,

    /**
     * Order has been cancelled.
     */
    CANCELLED
}
