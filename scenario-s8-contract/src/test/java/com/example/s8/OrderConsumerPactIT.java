package com.example.s8;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer Pact test that generates the contract (pact file).
 * This test simulates the consumer's expectations of the Order API.
 *
 * The generated pact file will be used to verify the provider implementation.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "OrderProvider")
class OrderConsumerPactIT {

    private static final String ORDER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String CUSTOMER_NAME = "John Doe";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Defines the contract for GET /api/orders/{id}.
     * Consumer expects: id (UUID), customerName (String), status (String)
     */
    @Pact(consumer = "OrderConsumer")
    public V4Pact getOrderPact(PactDslWithProvider builder) {
        return builder
            .given("an order with ID " + ORDER_ID + " exists")
            .uponReceiving("a request to get an order by ID")
                .path("/api/orders/" + ORDER_ID)
                .method("GET")
            .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                    .uuid("id", UUID.fromString(ORDER_ID))
                    .stringType("customerName", CUSTOMER_NAME)
                    .stringType("status", "PENDING")
                )
            .toPact(V4Pact.class);
    }

    /**
     * Defines the contract for POST /api/orders.
     * Consumer sends: customerName (String)
     * Consumer expects: id (UUID), customerName (String), status (String)
     */
    @Pact(consumer = "OrderConsumer")
    public V4Pact createOrderPact(PactDslWithProvider builder) {
        return builder
            .given("the order service is available")
            .uponReceiving("a request to create a new order")
                .path("/api/orders")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                    .stringType("customerName", CUSTOMER_NAME)
                )
            .willRespondWith()
                .status(201)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                    .uuid("id")
                    .stringType("customerName", CUSTOMER_NAME)
                    .stringType("status", "PENDING")
                )
            .toPact(V4Pact.class);
    }

    /**
     * Defines the contract for GET /api/orders/{id} when order not found.
     */
    @Pact(consumer = "OrderConsumer")
    public V4Pact getOrderNotFoundPact(PactDslWithProvider builder) {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        return builder
            .given("no order exists with ID " + nonExistentId)
            .uponReceiving("a request to get a non-existent order")
                .path("/api/orders/" + nonExistentId)
                .method("GET")
            .willRespondWith()
                .status(404)
            .toPact(V4Pact.class);
    }

    /**
     * Test for GET /api/orders/{id} - verifies the consumer can parse the response.
     */
    @Test
    @PactTestFor(pactMethod = "getOrderPact")
    void testGetOrder(MockServer mockServer) {
        String url = mockServer.getUrl() + "/api/orders/" + ORDER_ID;

        ResponseEntity<OrderResponse> response = restTemplate.getForEntity(url, OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(UUID.fromString(ORDER_ID));
        assertThat(response.getBody().customerName()).isEqualTo(CUSTOMER_NAME);
        assertThat(response.getBody().status()).isEqualTo("PENDING");
    }

    /**
     * Test for POST /api/orders - verifies the consumer can create an order.
     */
    @Test
    @PactTestFor(pactMethod = "createOrderPact")
    void testCreateOrder(MockServer mockServer) {
        String url = mockServer.getUrl() + "/api/orders";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = """
            {
                "customerName": "%s"
            }
            """.formatted(CUSTOMER_NAME);

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(url, request, OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().customerName()).isEqualTo(CUSTOMER_NAME);
        assertThat(response.getBody().status()).isEqualTo("PENDING");
    }

    /**
     * Test for GET /api/orders/{id} when order not found.
     */
    @Test
    @PactTestFor(pactMethod = "getOrderNotFoundPact")
    void testGetOrderNotFound(MockServer mockServer) {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        String url = mockServer.getUrl() + "/api/orders/" + nonExistentId;

        try {
            restTemplate.getForEntity(url, OrderResponse.class);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Internal record to deserialize order responses in consumer tests.
     */
    private record OrderResponse(
        UUID id,
        String customerName,
        String status
    ) {}
}
