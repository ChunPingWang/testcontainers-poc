package com.example.s5.client.dto;

/**
 * Response from the external credit check API.
 *
 * @param customerId the customer ID
 * @param approved whether the credit is approved
 * @param creditLimit the approved credit limit
 * @param status the status code (e.g., "APPROVED", "DENIED", "FALLBACK")
 * @param message additional message
 */
public record CreditCheckResponse(
        String customerId,
        boolean approved,
        int creditLimit,
        String status,
        String message
) {

    /**
     * Creates a sample approved response for testing.
     *
     * @param customerId the customer ID
     * @return an approved credit check response
     */
    public static CreditCheckResponse approved(String customerId) {
        return new CreditCheckResponse(
                customerId,
                true,
                50000,
                "APPROVED",
                "Credit approved"
        );
    }

    /**
     * Creates a sample denied response for testing.
     *
     * @param customerId the customer ID
     * @return a denied credit check response
     */
    public static CreditCheckResponse denied(String customerId) {
        return new CreditCheckResponse(
                customerId,
                false,
                0,
                "DENIED",
                "Credit denied due to insufficient history"
        );
    }
}
