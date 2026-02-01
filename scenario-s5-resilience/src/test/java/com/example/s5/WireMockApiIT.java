package com.example.s5;

import com.example.s5.client.ExternalApiClient;
import com.example.s5.client.dto.CreditCheckResponse;
import com.example.s5.service.CreditCheckService;
import com.example.s5.service.dto.CreditDecision;
import com.example.tc.base.IntegrationTestBase;
import com.example.tc.containers.WireMockContainerFactory;
import com.github.tomakehurst.wiremock.client.WireMock;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for external API mocking with WireMock.
 * Validates US8: 外部系統故障韌性測試
 *
 * Tests cover:
 * - Success scenarios: External API returns valid responses
 * - Error scenarios: External API returns errors (4xx, 5xx)
 * - Delay scenarios: External API responds slowly
 * - Fallback behavior on failures
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class WireMockApiIT extends IntegrationTestBase {

    @Container
    static GenericContainer<?> wireMockContainer = WireMockContainerFactory.createNew();

    @Autowired
    private ExternalApiClient externalApiClient;

    @Autowired
    private CreditCheckService creditCheckService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private WireMock wireMock;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("external.api.base-url", () ->
                "http://" + wireMockContainer.getHost() + ":" + wireMockContainer.getMappedPort(8080));
    }

    @BeforeEach
    void setUp() {
        // Configure WireMock client
        wireMock = new WireMock(wireMockContainer.getHost(), wireMockContainer.getMappedPort(8080));
        WireMock.configureFor(wireMockContainer.getHost(), wireMockContainer.getMappedPort(8080));
        wireMock.resetMappings();

        // Reset circuit breaker state before each test
        circuitBreakerRegistry.circuitBreaker("creditCheck").reset();
    }

    @AfterEach
    void tearDown() {
        wireMock.resetMappings();
    }

    @Test
    @DisplayName("Should return approved credit when external API returns success")
    void shouldReturnApprovedCreditOnSuccess() {
        // Given - WireMock returns a successful credit check response
        String customerId = "customer-001";
        stubFor(get(urlPathEqualTo("/api/credit-check/" + customerId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "customerId": "customer-001",
                                    "approved": true,
                                    "creditLimit": 50000,
                                    "status": "APPROVED",
                                    "message": "Credit approved"
                                }
                                """)));

        // When
        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.approved()).isTrue();
        assertThat(response.creditLimit()).isEqualTo(50000);
        assertThat(response.status()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Should return denied credit when external API returns denied status")
    void shouldReturnDeniedCreditWhenApiReturnsDenied() {
        // Given
        String customerId = "customer-002";
        stubFor(get(urlPathEqualTo("/api/credit-check/" + customerId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "customerId": "customer-002",
                                    "approved": false,
                                    "creditLimit": 0,
                                    "status": "DENIED",
                                    "message": "Insufficient credit history"
                                }
                                """)));

        // When
        CreditDecision decision = creditCheckService.performCreditCheck(customerId, 10000);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.approved()).isFalse();
        assertThat(decision.approvedLimit()).isEqualTo(0);
        assertThat(decision.usedFallback()).isFalse();
    }

    @Test
    @DisplayName("Should use fallback when external API returns 500 error")
    void shouldUseFallbackOnServerError() {
        // Given - WireMock returns a server error
        String customerId = "customer-003";
        stubFor(get(urlPathEqualTo("/api/credit-check/" + customerId))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // When - After retries, fallback should be triggered
        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        // Then - Should receive fallback response
        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.approved()).isFalse();
        assertThat(response.status()).isEqualTo("FALLBACK");
    }

    @Test
    @DisplayName("Should use fallback when external API returns 503 Service Unavailable")
    void shouldUseFallbackOnServiceUnavailable() {
        // Given
        String customerId = "customer-004";
        stubFor(get(urlPathEqualTo("/api/credit-check/" + customerId))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("Service Unavailable")));

        // When
        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("FALLBACK");
        assertThat(response.approved()).isFalse();
    }

    @Test
    @DisplayName("Should handle delay and eventually use fallback on timeout")
    void shouldHandleDelayAndUseFallback() {
        // Given - WireMock responds with a delay longer than timeout
        String customerId = "customer-005";
        stubFor(get(urlPathEqualTo("/api/credit-check/" + customerId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "customerId": "customer-005",
                                    "approved": true,
                                    "creditLimit": 30000,
                                    "status": "APPROVED",
                                    "message": "Credit approved"
                                }
                                """)
                        .withFixedDelay(10000))); // 10 second delay

        // When - Should timeout and use fallback
        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("FALLBACK");
    }

    @Test
    @DisplayName("Should handle 404 Not Found as an error")
    void shouldHandle404AsError() {
        // Given
        String customerId = "unknown-customer";
        stubFor(get(urlPathEqualTo("/api/credit-check/" + customerId))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Customer not found")));

        // When
        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        // Then - Should use fallback
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("FALLBACK");
    }

    @Test
    @DisplayName("Should correctly evaluate credit decision based on requested amount")
    void shouldEvaluateCreditDecisionBasedOnAmount() {
        // Given
        String customerId = "customer-006";
        stubFor(get(urlPathEqualTo("/api/credit-check/" + customerId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "customerId": "customer-006",
                                    "approved": true,
                                    "creditLimit": 20000,
                                    "status": "APPROVED",
                                    "message": "Credit approved"
                                }
                                """)));

        // When - Request amount exceeds credit limit
        CreditDecision decision = creditCheckService.performCreditCheck(customerId, 25000);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.approved()).isFalse();
        assertThat(decision.approvedLimit()).isEqualTo(20000);
        assertThat(decision.reason()).contains("exceeds credit limit");
    }

    @Test
    @DisplayName("Should approve credit when requested amount is within limit")
    void shouldApproveCreditWhenAmountWithinLimit() {
        // Given
        String customerId = "customer-007";
        stubFor(get(urlPathEqualTo("/api/credit-check/" + customerId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "customerId": "customer-007",
                                    "approved": true,
                                    "creditLimit": 50000,
                                    "status": "APPROVED",
                                    "message": "Credit approved"
                                }
                                """)));

        // When
        CreditDecision decision = creditCheckService.performCreditCheck(customerId, 30000);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.approved()).isTrue();
        assertThat(decision.approvedLimit()).isEqualTo(50000);
        assertThat(decision.usedFallback()).isFalse();
    }
}
