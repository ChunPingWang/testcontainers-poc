package com.example.s5.service.dto;

/**
 * The result of a credit check decision.
 *
 * @param customerId the customer ID
 * @param approved whether the credit request is approved
 * @param approvedLimit the approved credit limit
 * @param reason the reason for the decision
 * @param usedFallback whether the fallback was used
 */
public record CreditDecision(
        String customerId,
        boolean approved,
        int approvedLimit,
        String reason,
        boolean usedFallback
) {

    /**
     * Creates a sample approved decision for testing.
     *
     * @param customerId the customer ID
     * @return an approved credit decision
     */
    public static CreditDecision approved(String customerId) {
        return new CreditDecision(
                customerId,
                true,
                50000,
                "Credit approved",
                false
        );
    }

    /**
     * Creates a sample denied decision for testing.
     *
     * @param customerId the customer ID
     * @return a denied credit decision
     */
    public static CreditDecision denied(String customerId) {
        return new CreditDecision(
                customerId,
                false,
                0,
                "Credit denied",
                false
        );
    }

    /**
     * Creates a fallback decision for testing.
     *
     * @param customerId the customer ID
     * @return a fallback credit decision
     */
    public static CreditDecision fallback(String customerId) {
        return new CreditDecision(
                customerId,
                false,
                0,
                "Credit check denied: Service temporarily unavailable - using fallback response",
                true
        );
    }
}
