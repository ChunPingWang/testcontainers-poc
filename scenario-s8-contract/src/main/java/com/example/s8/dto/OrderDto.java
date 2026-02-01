package com.example.s8.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Order DTO matching the Pact contract.
 * Contains: id, customerName, status
 */
public record OrderDto(
    UUID id,
    String customerName,
    String status,
    Instant createdAt,
    Instant updatedAt
) {

    /**
     * Creates a sample order for testing.
     *
     * @param id the order ID
     * @param customerName the customer name
     * @return a new OrderDto with PENDING status
     */
    public static OrderDto sample(UUID id, String customerName) {
        Instant now = Instant.now();
        return new OrderDto(id, customerName, "PENDING", now, now);
    }
}
