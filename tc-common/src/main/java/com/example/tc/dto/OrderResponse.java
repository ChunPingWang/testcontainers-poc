package com.example.tc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for order response.
 * Used across scenario modules for API testing.
 */
public record OrderResponse(
    UUID id,
    String customerName,
    String productName,
    int quantity,
    BigDecimal amount,
    String status,
    Instant createdAt,
    Instant updatedAt
) {

    /**
     * Creates a sample order response for testing.
     *
     * @return a sample OrderResponse
     */
    public static OrderResponse sample() {
        Instant now = Instant.now();
        return new OrderResponse(
            UUID.randomUUID(),
            "Test Customer",
            "Test Product",
            1,
            new BigDecimal("100.00"),
            "PENDING",
            now,
            now
        );
    }
}
