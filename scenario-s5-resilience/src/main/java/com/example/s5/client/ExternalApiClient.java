package com.example.s5.client;

import com.example.s5.client.dto.CreditCheckResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for external credit check API.
 * Implements resilience patterns using Resilience4j annotations:
 * - Circuit Breaker: Prevents cascade failures
 * - Retry: Handles transient failures
 * - Fallback: Provides graceful degradation
 */
@Component
public class ExternalApiClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiClient.class);

    private final RestTemplate restTemplate;
    private final String externalApiBaseUrl;

    public ExternalApiClient(
            RestTemplate restTemplate,
            @Value("${external.api.base-url:http://localhost:8080}") String externalApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.externalApiBaseUrl = externalApiBaseUrl;
    }

    /**
     * Checks credit for a customer.
     * Uses circuit breaker and retry patterns for resilience.
     *
     * @param customerId the customer ID to check
     * @return the credit check response
     * @throws ExternalServiceException if the external service fails
     */
    @CircuitBreaker(name = "creditCheck", fallbackMethod = "creditCheckFallback")
    @Retry(name = "creditCheck")
    public CreditCheckResponse checkCredit(String customerId) {
        log.debug("Checking credit for customer: {}", customerId);

        String url = externalApiBaseUrl + "/api/credit-check/" + customerId;

        try {
            ResponseEntity<CreditCheckResponse> response = restTemplate.getForEntity(
                    url, CreditCheckResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Credit check successful for customer: {}", customerId);
                return response.getBody();
            }

            throw new ExternalServiceException("Unexpected response from credit check API");
        } catch (RestClientException e) {
            log.error("Credit check failed for customer {}: {}", customerId, e.getMessage());
            throw new ExternalServiceException("Credit check API call failed", e);
        }
    }

    /**
     * Fallback method when credit check fails.
     * Returns a conservative response to prevent cascade failures.
     *
     * @param customerId the customer ID
     * @param throwable the exception that triggered the fallback
     * @return a fallback credit check response
     */
    public CreditCheckResponse creditCheckFallback(String customerId, Throwable throwable) {
        log.warn("Credit check fallback triggered for customer {}: {}", customerId, throwable.getMessage());

        return new CreditCheckResponse(
                customerId,
                false,  // Conservative: deny credit on failure
                0,      // Zero credit limit
                "FALLBACK",
                "Service temporarily unavailable - using fallback response"
        );
    }

    /**
     * Checks if the external API is healthy.
     * Used for health checks and circuit breaker testing.
     *
     * @return true if the API is healthy
     */
    @CircuitBreaker(name = "healthCheck", fallbackMethod = "healthCheckFallback")
    @Retry(name = "healthCheck")
    public boolean isHealthy() {
        log.debug("Checking external API health");

        String url = externalApiBaseUrl + "/health";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.error("Health check failed: {}", e.getMessage());
            throw new ExternalServiceException("Health check failed", e);
        }
    }

    /**
     * Fallback for health check.
     *
     * @param throwable the exception that triggered the fallback
     * @return false indicating unhealthy
     */
    public boolean healthCheckFallback(Throwable throwable) {
        log.warn("Health check fallback triggered: {}", throwable.getMessage());
        return false;
    }
}
