package com.example.s8.dto;

/**
 * Request DTO for creating a new order.
 */
public record CreateOrderRequest(
    String customerName
) {

    /**
     * Creates a sample request for testing.
     *
     * @return a sample CreateOrderRequest
     */
    public static CreateOrderRequest sample() {
        return new CreateOrderRequest("Test Customer");
    }
}
