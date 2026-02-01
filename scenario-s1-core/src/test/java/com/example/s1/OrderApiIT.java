package com.example.s1;

import com.example.s1.domain.OrderStatus;
import com.example.tc.base.IntegrationTestBase;
import com.example.tc.dto.CreateOrderRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Order REST API.
 * Validates US2: 訂單處理端對端測試
 *
 * Given 系統已啟動測試容器
 * When 透過 API 建立訂單
 * Then 訂單成功儲存至資料庫並回傳成功狀態
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(S1TestApplication.class)
@ActiveProfiles("test")
class OrderApiIT extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/orders";
    }

    @Test
    void shouldCreateOrder() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
            "金控客戶",
            "信用卡服務",
            2,
            new BigDecimal("25000.00")
        );

        // When & Then
        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("customerName", equalTo("金控客戶"))
            .body("productName", equalTo("信用卡服務"))
            .body("quantity", equalTo(2))
            .body("status", equalTo(OrderStatus.PENDING.name()));
    }

    @Test
    void shouldGetOrderById() {
        // Given - Create an order first
        CreateOrderRequest request = CreateOrderRequest.sample();

        String orderId = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .path("id");

        // When & Then
        given()
        .when()
            .get("/{id}", orderId)
        .then()
            .statusCode(200)
            .body("id", equalTo(orderId))
            .body("customerName", equalTo(request.customerName()));
    }

    @Test
    void shouldReturn404ForNonExistentOrder() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        given()
        .when()
            .get("/{id}", nonExistentId)
        .then()
            .statusCode(404);
    }

    @Test
    void shouldListOrders() {
        // Given - Create a few orders
        given()
            .contentType(ContentType.JSON)
            .body(CreateOrderRequest.sample())
        .when()
            .post()
        .then()
            .statusCode(201);

        // When & Then
        given()
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", not(empty()))
            .body("totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    void shouldFilterOrdersByStatus() {
        // Given - Create an order
        given()
            .contentType(ContentType.JSON)
            .body(CreateOrderRequest.sample())
        .when()
            .post()
        .then()
            .statusCode(201);

        // When & Then
        given()
            .queryParam("status", OrderStatus.PENDING.name())
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content.findAll { it.status == 'PENDING' }.size()", greaterThanOrEqualTo(1));
    }
}
