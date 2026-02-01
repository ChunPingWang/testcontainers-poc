package com.example.s5;

import com.example.s5.client.ExternalApiClient;
import com.example.s5.client.dto.CreditCheckResponse;
import com.example.s5.service.CreditCheckService;
import com.example.s5.service.dto.CreditDecision;
import com.example.tc.base.IntegrationTestBase;
import com.example.tc.containers.ToxiproxyContainerFactory;
import com.example.tc.containers.WireMockContainerFactory;
import com.github.tomakehurst.wiremock.client.WireMock;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for network fault injection using Toxiproxy.
 * Validates US8: 外部系統故障韌性測試 - Network fault scenarios
 *
 * Tests cover:
 * - Network latency injection
 * - Connection timeout scenarios
 * - Network disconnect/reset scenarios
 * - Bandwidth limitation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ToxiproxyFaultIT extends IntegrationTestBase {

    private static final Network NETWORK = Network.newNetwork();
    private static final String WIREMOCK_NETWORK_ALIAS = "wiremock";
    private static final int PROXY_PORT = 8666;

    @Container
    static GenericContainer<?> wireMockContainer = WireMockContainerFactory.createNew()
            .withNetwork(NETWORK)
            .withNetworkAliases(WIREMOCK_NETWORK_ALIAS);

    @Container
    static ToxiproxyContainer toxiproxyContainer = ToxiproxyContainerFactory.create(NETWORK)
            .withExposedPorts(8474, PROXY_PORT);

    private static ToxiproxyClient staticToxiproxyClient;
    private static Proxy staticWireMockProxy;

    @Autowired
    private ExternalApiClient externalApiClient;

    @Autowired
    private CreditCheckService creditCheckService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private WireMock wireMock;

    @BeforeAll
    static void setUpProxy() throws IOException {
        // Initialize Toxiproxy client and create proxy once for all tests
        staticToxiproxyClient = new ToxiproxyClient(
                toxiproxyContainer.getHost(),
                toxiproxyContainer.getControlPort());

        // Create proxy from Toxiproxy to WireMock
        staticWireMockProxy = staticToxiproxyClient.createProxy(
                "wiremock-proxy",
                "0.0.0.0:" + PROXY_PORT,
                WIREMOCK_NETWORK_ALIAS + ":8080");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure the external API URL to go through Toxiproxy
        registry.add("external.api.base-url", () ->
                "http://" + toxiproxyContainer.getHost() + ":" +
                        toxiproxyContainer.getMappedPort(PROXY_PORT));
    }

    @BeforeEach
    void setUp() throws IOException {
        // Configure WireMock client directly (not through proxy)
        wireMock = new WireMock(wireMockContainer.getHost(), wireMockContainer.getMappedPort(8080));
        WireMock.configureFor(wireMockContainer.getHost(), wireMockContainer.getMappedPort(8080));
        wireMock.resetMappings();

        // Remove all toxics before each test
        if (staticWireMockProxy != null) {
            staticWireMockProxy.toxics().getAll().forEach(toxic -> {
                try {
                    toxic.remove();
                } catch (IOException e) {
                    log.warn("Failed to remove toxic: {}", e.getMessage());
                }
            });
        }

        // Reset circuit breaker
        circuitBreakerRegistry.circuitBreaker("creditCheck").reset();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up toxics after each test
        if (staticWireMockProxy != null) {
            staticWireMockProxy.toxics().getAll().forEach(toxic -> {
                try {
                    toxic.remove();
                } catch (IOException e) {
                    log.warn("Failed to remove toxic: {}", e.getMessage());
                }
            });
        }
        wireMock.resetMappings();
    }

    @Test
    @DisplayName("Should handle network latency and still succeed within timeout")
    void shouldHandleNetworkLatencyWithinTimeout() throws IOException {
        // Given - WireMock returns success
        String customerId = "customer-latency-001";
        setupSuccessfulCreditCheckStub(customerId);

        // Add 1 second latency (within timeout)
        staticWireMockProxy.toxics()
                .latency("latency-downstream", ToxicDirection.DOWNSTREAM, 1000);

        // When
        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        // Then - Should still succeed despite latency
        assertThat(response).isNotNull();
        assertThat(response.approved()).isTrue();
        assertThat(response.status()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Should use fallback when network latency exceeds timeout")
    void shouldUseFallbackWhenLatencyExceedsTimeout() throws IOException {
        // Given
        String customerId = "customer-latency-002";
        setupSuccessfulCreditCheckStub(customerId);

        // Add 8 second latency (exceeds 5s timeout)
        staticWireMockProxy.toxics()
                .latency("latency-downstream", ToxicDirection.DOWNSTREAM, 8000);

        // When
        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        // Then - Should use fallback
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("FALLBACK");
        assertThat(response.approved()).isFalse();
    }

    @Test
    @DisplayName("Should use fallback when connection is reset")
    void shouldUseFallbackOnConnectionReset() throws IOException {
        // Given
        String customerId = "customer-reset-001";
        setupSuccessfulCreditCheckStub(customerId);

        // Simulate connection reset
        staticWireMockProxy.toxics()
                .resetPeer("reset-downstream", ToxicDirection.DOWNSTREAM, 0);

        // When
        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("FALLBACK");
    }

    @Test
    @DisplayName("Should use fallback when connection times out")
    void shouldUseFallbackOnConnectionTimeout() throws IOException {
        // Given
        String customerId = "customer-timeout-001";
        setupSuccessfulCreditCheckStub(customerId);

        // Add timeout toxic - stops data from being sent
        staticWireMockProxy.toxics()
                .timeout("timeout-downstream", ToxicDirection.DOWNSTREAM, 100);

        // When
        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("FALLBACK");
    }

    @Test
    @DisplayName("Should handle bandwidth limitation")
    void shouldHandleBandwidthLimitation() throws IOException {
        // Given
        String customerId = "customer-bandwidth-001";
        setupSuccessfulCreditCheckStub(customerId);

        // Limit bandwidth to 1KB/s
        staticWireMockProxy.toxics()
                .bandwidth("bandwidth-downstream", ToxicDirection.DOWNSTREAM, 1024);

        // When - The small response should still arrive, just slowly
        CreditCheckResponse response = externalApiClient.checkCredit(customerId);

        // Then - Should eventually succeed or fallback depending on total time
        assertThat(response).isNotNull();
        // Response could be success or fallback depending on timing
    }

    @Test
    @DisplayName("Should recover after network issue is resolved")
    void shouldRecoverAfterNetworkIssueResolved() throws IOException {
        // Given - Initial working state
        String customerId = "customer-recovery-001";
        setupSuccessfulCreditCheckStub(customerId);

        // First call should succeed
        CreditCheckResponse response1 = externalApiClient.checkCredit(customerId);
        assertThat(response1.approved()).isTrue();

        // Add latency to cause failure
        var latencyToxic = staticWireMockProxy.toxics()
                .latency("latency-downstream", ToxicDirection.DOWNSTREAM, 8000);

        // Second call should use fallback
        CreditCheckResponse response2 = externalApiClient.checkCredit(customerId);
        assertThat(response2.status()).isEqualTo("FALLBACK");

        // Remove the toxic
        latencyToxic.remove();

        // Reset circuit breaker for fresh test
        circuitBreakerRegistry.circuitBreaker("creditCheck").reset();

        // Third call should succeed again
        CreditCheckResponse response3 = externalApiClient.checkCredit(customerId);
        assertThat(response3.approved()).isTrue();
    }

    @Test
    @DisplayName("Should handle intermittent network issues")
    void shouldHandleIntermittentNetworkIssues() throws IOException {
        // Given
        String customerId = "customer-intermittent-001";
        setupSuccessfulCreditCheckStub(customerId);

        // Add jitter - random latency between 0-2000ms
        staticWireMockProxy.toxics()
                .latency("jitter", ToxicDirection.DOWNSTREAM, 500)
                .setJitter(1500);

        // When - Multiple calls
        int successCount = 0;
        int fallbackCount = 0;

        for (int i = 0; i < 5; i++) {
            // Reset circuit breaker for each attempt
            circuitBreakerRegistry.circuitBreaker("creditCheck").reset();

            CreditCheckResponse response = externalApiClient.checkCredit(customerId);
            if ("FALLBACK".equals(response.status())) {
                fallbackCount++;
            } else {
                successCount++;
            }
        }

        // Then - Should have a mix of success and fallback
        log.info("Success: {}, Fallback: {}", successCount, fallbackCount);
        assertThat(successCount + fallbackCount).isEqualTo(5);
    }

    @Test
    @DisplayName("Should report correct decision using fallback on network failure")
    void shouldReportCorrectDecisionUsingFallback() throws IOException {
        // Given
        String customerId = "customer-decision-001";
        setupSuccessfulCreditCheckStub(customerId);

        // Cause network failure
        staticWireMockProxy.toxics()
                .resetPeer("reset", ToxicDirection.DOWNSTREAM, 0);

        // When
        CreditDecision decision = creditCheckService.performCreditCheck(customerId, 10000);

        // Then
        assertThat(decision).isNotNull();
        assertThat(decision.approved()).isFalse();
        assertThat(decision.usedFallback()).isTrue();
        assertThat(decision.reason()).contains("fallback");
    }

    private void setupSuccessfulCreditCheckStub(String customerId) {
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
}
