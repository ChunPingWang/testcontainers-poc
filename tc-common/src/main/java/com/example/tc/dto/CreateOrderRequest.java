package com.example.tc.dto;

import java.math.BigDecimal;

/**
 * DTO for creating a new order.
 * Used across scenario modules for API testing.
 */
public record CreateOrderRequest(
    String customerName,
    String productName,
    int quantity,
    BigDecimal amount
) {

    /**
     * Creates a sample order request for testing.
     *
     * @return a sample CreateOrderRequest
     */
    public static CreateOrderRequest sample() {
        return new CreateOrderRequest(
            "Test Customer",
            "Test Product",
            1,
            new BigDecimal("100.00")
        );
    }

    /**
     * Creates a custom order request.
     *
     * @param customerName the customer name
     * @param productName  the product name
     * @param quantity     the quantity
     * @param amount       the amount
     * @return a new CreateOrderRequest
     */
    public static CreateOrderRequest of(
            String customerName,
            String productName,
            int quantity,
            BigDecimal amount) {
        return new CreateOrderRequest(customerName, productName, quantity, amount);
    }
}
