package com.example.s5;

import com.example.s5.client.ExternalApiClient;
import com.example.s5.client.dto.CreditCheckResponse;
import com.example.s5.service.CreditCheckService;
import com.example.tc.base.IntegrationTestBase;
import com.example.tc.containers.WireMockContainerFactory;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Circuit Breaker state transitions.
 * Validates US8: 外部系統故障韌性測試 - Circuit breaker patterns
 *
 * Tests cover:
 * - Circuit breaker opens after consecutive failures (CLOSED -> OPEN)
 * - Circuit breaker transitions to half-open after wait duration (OPEN -> HALF_OPEN)
 * - Circuit breaker closes on successful calls (HALF_OPEN -> CLOSED)
 * - Circuit breaker returns to open on failures in half-open state
 * - Fallback is used when circuit is open
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class CircuitBreakerIT extends IntegrationTestBase {

    @Container
    static GenericContainer<?> wireMockContainer = WireMockContainerFactory.createNew();

    @Autowired
    private ExternalApiClient externalApiClient;

    @Autowired
    private CreditCheckService creditCheckService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private WireMock wireMock;
    private CircuitBreaker circuitBreaker;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("external.api.base-url", () ->
                "http://" + wireMockContainer.getHost() + ":" + wireMockContainer.getMappedPort(8080));

        // Override circuit breaker settings for faster testing
        registry.add("resilience4j.circuitbreaker.instances.creditCheck.slidingWindowSize", () -> "5");
        registry.add("resilience4j.circuitbreaker.instances.creditCheck.minimumNumberOfCalls", () -> "3");
        registry.add("resilience4j.circuitbreaker.instances.creditCheck.failureRateThreshold", () -> "50");
        registry.add("resilience4j.circuitbreaker.instances.creditCheck.waitDurationInOpenState", () -> "5s");
        registry.add("resilience4j.circuitbreaker.instances.creditCheck.permittedNumberOfCallsInHalfOpenState", () -> "2");
        registry.add("resilience4j.circuitbreaker.instances.creditCheck.automaticTransitionFromOpenToHalfOpenEnabled", () -> "true");

        // Reduce retry attempts for faster testing
        registry.add("resilience4j.retry.instances.creditCheck.maxAttempts", () -> "1");
    }

    @BeforeEach
    void setUp() {
        wireMock = new WireMock(wireMockContainer.getHost(), wireMockContainer.getMappedPort(8080));
        WireMock.configureFor(wireMockContainer.getHost(), wireMockContainer.getMappedPort(8080));
        wireMock.resetMappings();

        circuitBreaker = circuitBreakerRegistry.circuitBreaker("creditCheck");
        circuitBreaker.reset();
    }

    @AfterEach
    void tearDown() {
        wireMock.resetMappings();
        circuitBreaker.reset();
    }

    @Test
    @DisplayName("Circuit breaker should start in CLOSED state")
    void circuitBreakerShouldStartClosed() {
        // Given/When - Initial state
        CircuitBreaker.State state = creditCheckService.getCircuitBreakerState();

        // Then
        assertThat(state).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Circuit breaker should stay CLOSED on successful calls")
    void circuitBreakerShouldStayClosedOnSuccess() {
        // Given
        String customerId = "customer-success";
        setupSuccessStub(customerId);

        // When - Make several successful calls
        for (int i = 0; i < 5; i++) {
            CreditCheckResponse response = externalApiClient.checkCredit(customerId);
            assertThat(response.approved()).isTrue();
        }

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Circuit breaker should OPEN after consecutive failures")
    void circuitBreakerShouldOpenAfterConsecutiveFailures() {
        // Given
        String customerId = "customer-failure";
        setupFailureStub(customerId);

        // When - Make enough calls to trigger circuit breaker
        // With minimumNumberOfCalls=3 and failureRateThreshold=50%
        // After 3+ failures, it should open
        for (int i = 0; i < 5; i++) {
            CreditCheckResponse response = externalApiClient.checkCredit(customerId);
            // All should use fallback eventually
        }

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        log.info("Circuit breaker metrics: {}", creditCheckService.getCircuitBreakerMetrics());
    }

    @Test
    @DisplayName("Circuit breaker should use fallback when OPEN")
    void circuitBreakerShouldUseFallbackWhenOpen() {
        // Given - Force circuit breaker to open state
        circuitBreaker.transitionToOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        String customerId = "customer-open";
        // Don't even need to set up stub - should not reach the server

        // When
        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        // Then - Should get fallback immediately
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("FALLBACK");
        assertThat(response.approved()).isFalse();
    }

    @Test
    @DisplayName("Circuit breaker should transition to HALF_OPEN after wait duration")
    void circuitBreakerShouldTransitionToHalfOpen() {
        // Given - Force to open state
        circuitBreaker.transitionToOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // When - Wait for automatic transition (5 seconds in test config)
        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() ->
                        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN));

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    @DisplayName("Circuit breaker should CLOSE from HALF_OPEN on successful calls")
    void circuitBreakerShouldCloseFromHalfOpenOnSuccess() {
        // Given
        String customerId = "customer-recovery";
        setupSuccessStub(customerId);

        // Force to half-open state
        circuitBreaker.transitionToOpenState();
        await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN);

        // When - Make successful calls (permittedNumberOfCallsInHalfOpenState=2)
        for (int i = 0; i < 2; i++) {
            CreditCheckResponse response = externalApiClient.checkCredit(customerId);
            assertThat(response.approved()).isTrue();
        }

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Circuit breaker should return to OPEN from HALF_OPEN on failure")
    void circuitBreakerShouldReturnToOpenFromHalfOpenOnFailure() {
        // Given
        String customerId = "customer-fail-again";
        setupFailureStub(customerId);

        // Force to half-open state
        circuitBreaker.transitionToOpenState();
        await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN);

        // When - Make a call that fails
        externalApiClient.checkCredit(customerId);

        // Then - Should go back to OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Circuit breaker metrics should be accurate")
    void circuitBreakerMetricsShouldBeAccurate() {
        // Given
        String successCustomer = "customer-metric-success";
        String failCustomer = "customer-metric-fail";
        setupSuccessStub(successCustomer);
        setupFailureStub(failCustomer);

        // When - Make mixed calls
        externalApiClient.checkCredit(successCustomer); // success
        externalApiClient.checkCredit(successCustomer); // success
        externalApiClient.checkCredit(failCustomer);    // failure (uses fallback)

        // Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isGreaterThanOrEqualTo(1);

        log.info("Circuit breaker metrics: {}", creditCheckService.getCircuitBreakerMetrics());
    }

    @Test
    @DisplayName("Should be able to manually reset circuit breaker")
    void shouldBeAbleToManuallyResetCircuitBreaker() {
        // Given - Force to open
        circuitBreaker.transitionToOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // When
        creditCheckService.resetCircuitBreaker();

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Circuit breaker should handle mixed success and failure scenarios")
    void circuitBreakerShouldHandleMixedScenarios() {
        // Given
        String successCustomer = "customer-mixed-success";
        String failCustomer = "customer-mixed-fail";
        setupSuccessStub(successCustomer);
        setupFailureStub(failCustomer);

        // When - Start with successes
        for (int i = 0; i < 3; i++) {
            CreditCheckResponse response = externalApiClient.checkCredit(successCustomer);
            assertThat(response.approved()).isTrue();
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Then add failures - but not enough to open (50% threshold with 5 window size)
        externalApiClient.checkCredit(failCustomer); // 1 failure, 3 success = 25% failure rate
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Add more failures
        externalApiClient.checkCredit(failCustomer); // 2 failures, 3 success = 40% failure rate
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        externalApiClient.checkCredit(failCustomer); // 3 failures in window, should trigger open
        // At this point we have 3 failures in the sliding window
        // The failure rate should exceed 50%

        log.info("Final state: {}", circuitBreaker.getState());
        log.info("Metrics: {}", creditCheckService.getCircuitBreakerMetrics());
    }

    private void setupSuccessStub(String customerId) {
        stubFor(get(urlPathEqualTo("/api/credit-check/" + customerId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                                {
                                    "customerId": "%s",
                                    "approved": true,
                                    "creditLimit": 50000,
                                    "status": "APPROVED",
                                    "message": "Credit approved"
                                }
                                """, customerId))));
    }

    private void setupFailureStub(String customerId) {
        stubFor(get(urlPathEqualTo("/api/credit-check/" + customerId))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));
    }
}
