package com.example.s5.service;

import com.example.s5.client.ExternalApiClient;
import com.example.s5.client.dto.CreditCheckResponse;
import com.example.s5.service.dto.CreditDecision;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for credit check operations.
 * Orchestrates calls to external credit check API with resilience patterns.
 *
 * This service demonstrates:
 * - Integration with Resilience4j circuit breaker
 * - Fallback handling for graceful degradation
 * - Timeout handling
 */
@Service
public class CreditCheckService {

    private static final Logger log = LoggerFactory.getLogger(CreditCheckService.class);

    private final ExternalApiClient externalApiClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CreditCheckService(
            ExternalApiClient externalApiClient,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.externalApiClient = externalApiClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Performs a credit check for a customer and returns a decision.
     *
     * @param customerId the customer ID
     * @param requestedAmount the requested credit amount
     * @return the credit decision
     */
    public CreditDecision performCreditCheck(String customerId, int requestedAmount) {
        log.info("Performing credit check for customer {} with requested amount {}", customerId, requestedAmount);

        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        return evaluateCreditDecision(response, requestedAmount);
    }

    /**
     * Evaluates the credit check response and makes a decision.
     *
     * @param response the credit check response from external API
     * @param requestedAmount the requested credit amount
     * @return the credit decision
     */
    private CreditDecision evaluateCreditDecision(CreditCheckResponse response, int requestedAmount) {
        if (!response.approved()) {
            return new CreditDecision(
                    response.customerId(),
                    false,
                    0,
                    "Credit check denied: " + response.message(),
                    isFallbackResponse(response)
            );
        }

        if (requestedAmount > response.creditLimit()) {
            return new CreditDecision(
                    response.customerId(),
                    false,
                    response.creditLimit(),
                    "Requested amount exceeds credit limit",
                    isFallbackResponse(response)
            );
        }

        return new CreditDecision(
                response.customerId(),
                true,
                response.creditLimit(),
                "Credit approved",
                isFallbackResponse(response)
        );
    }

    /**
     * Checks if the response is from the fallback method.
     *
     * @param response the credit check response
     * @return true if this is a fallback response
     */
    private boolean isFallbackResponse(CreditCheckResponse response) {
        return "FALLBACK".equals(response.status());
    }

    /**
     * Gets the current state of the credit check circuit breaker.
     *
     * @return the circuit breaker state
     */
    public CircuitBreaker.State getCircuitBreakerState() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("creditCheck");
        return circuitBreaker.getState();
    }

    /**
     * Gets the circuit breaker metrics.
     *
     * @return circuit breaker metrics as a string
     */
    public String getCircuitBreakerMetrics() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("creditCheck");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        return String.format(
                "State=%s, FailureRate=%.2f%%, SlowCallRate=%.2f%%, BufferedCalls=%d, FailedCalls=%d",
                circuitBreaker.getState(),
                metrics.getFailureRate(),
                metrics.getSlowCallRate(),
                metrics.getNumberOfBufferedCalls(),
                metrics.getNumberOfFailedCalls()
        );
    }

    /**
     * Resets the circuit breaker to closed state.
     * Used for testing purposes.
     */
    public void resetCircuitBreaker() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("creditCheck");
        circuitBreaker.reset();
        log.info("Circuit breaker reset to CLOSED state");
    }

    /**
     * Checks the health of the external API.
     *
     * @return true if the external API is healthy
     */
    public boolean isExternalApiHealthy() {
        return externalApiClient.isHealthy();
    }
}
