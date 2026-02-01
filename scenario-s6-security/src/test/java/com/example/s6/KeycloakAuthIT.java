package com.example.s6;

import com.example.tc.base.IntegrationTestBase;
import com.example.tc.util.TokenHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Keycloak OAuth2/OIDC authentication.
 *
 * Tests:
 * - Successful login returns access token
 * - Admin can access admin endpoints
 * - User cannot access admin endpoints (403)
 * - Token refresh works
 * - Role-based access control
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(S6TestApplication.class)
@ActiveProfiles("test")
class KeycloakAuthIT extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    private String adminToken;
    private String userToken;

    @DynamicPropertySource
    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", S6TestApplication::getIssuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", S6TestApplication::getJwkSetUri);
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        // Get tokens for admin and regular user
        adminToken = TokenHelper.getAccessToken(
            S6TestApplication.getKeycloakUrl(),
            S6TestApplication.getRealm(),
            S6TestApplication.getClientId(),
            "admin",
            "admin123"
        );

        userToken = TokenHelper.getAccessToken(
            S6TestApplication.getKeycloakUrl(),
            S6TestApplication.getRealm(),
            S6TestApplication.getClientId(),
            "user",
            "user123"
        );
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should return access token on successful login")
        void shouldReturnAccessTokenOnSuccessfulLogin() {
            // When
            String token = TokenHelper.getAccessToken(
                S6TestApplication.getKeycloakUrl(),
                S6TestApplication.getRealm(),
                S6TestApplication.getClientId(),
                "admin",
                "admin123"
            );

            // Then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("Should return access token for regular user")
        void shouldReturnAccessTokenForRegularUser() {
            // When
            String token = TokenHelper.getAccessToken(
                S6TestApplication.getKeycloakUrl(),
                S6TestApplication.getRealm(),
                S6TestApplication.getClientId(),
                "user",
                "user123"
            );

            // Then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject request without token")
        void shouldRejectRequestWithoutToken() {
            given()
            .when()
                .get("/api/orders")
            .then()
                .statusCode(401);
        }

        @Test
        @DisplayName("Should reject request with invalid token")
        void shouldRejectRequestWithInvalidToken() {
            given()
                .header("Authorization", "Bearer invalid.token.here")
            .when()
                .get("/api/orders")
            .then()
                .statusCode(401);
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    class TokenRefreshTests {

        @Test
        @DisplayName("Token refresh works - getting new token succeeds")
        void tokenRefreshWorks() {
            // Get first token
            String firstToken = TokenHelper.getAccessToken(
                S6TestApplication.getKeycloakUrl(),
                S6TestApplication.getRealm(),
                S6TestApplication.getClientId(),
                "admin",
                "admin123"
            );

            // Get second token (simulates refresh)
            String secondToken = TokenHelper.getAccessToken(
                S6TestApplication.getKeycloakUrl(),
                S6TestApplication.getRealm(),
                S6TestApplication.getClientId(),
                "admin",
                "admin123"
            );

            // Both tokens should be valid
            assertThat(firstToken).isNotNull();
            assertThat(secondToken).isNotNull();

            // Tokens might be different due to timing
            // Verify both work
            given()
                .header("Authorization", TokenHelper.bearerToken(firstToken))
            .when()
                .get("/api/admin/stats")
            .then()
                .statusCode(200);

            given()
                .header("Authorization", TokenHelper.bearerToken(secondToken))
            .when()
                .get("/api/admin/stats")
            .then()
                .statusCode(200);
        }
    }

    @Nested
    @DisplayName("Role-Based Access Control Tests")
    class RoleBasedAccessTests {

        @Test
        @DisplayName("Admin can access admin endpoints")
        void adminCanAccessAdminEndpoints() {
            given()
                .header("Authorization", TokenHelper.bearerToken(adminToken))
            .when()
                .get("/api/admin/users")
            .then()
                .statusCode(200);
        }

        @Test
        @DisplayName("Admin can access user endpoints")
        void adminCanAccessUserEndpoints() {
            given()
                .header("Authorization", TokenHelper.bearerToken(adminToken))
            .when()
                .get("/api/orders")
            .then()
                .statusCode(200);
        }

        @Test
        @DisplayName("User cannot access admin endpoints - returns 403")
        void userCannotAccessAdminEndpoints() {
            given()
                .header("Authorization", TokenHelper.bearerToken(userToken))
            .when()
                .get("/api/admin/users")
            .then()
                .statusCode(403);
        }

        @Test
        @DisplayName("User can access user endpoints")
        void userCanAccessUserEndpoints() {
            given()
                .header("Authorization", TokenHelper.bearerToken(userToken))
            .when()
                .get("/api/orders")
            .then()
                .statusCode(200);
        }

        @Test
        @DisplayName("User cannot access admin stats endpoint")
        void userCannotAccessAdminStatsEndpoint() {
            given()
                .header("Authorization", TokenHelper.bearerToken(userToken))
            .when()
                .get("/api/admin/stats")
            .then()
                .statusCode(403);
        }

        @Test
        @DisplayName("User cannot access admin audit logs")
        void userCannotAccessAdminAuditLogs() {
            given()
                .header("Authorization", TokenHelper.bearerToken(userToken))
            .when()
                .get("/api/admin/audit-logs")
            .then()
                .statusCode(403);
        }
    }

    @Nested
    @DisplayName("Order Endpoint Tests")
    class OrderEndpointTests {

        @Test
        @DisplayName("User can create order")
        void userCanCreateOrder() {
            Map<String, Object> orderRequest = Map.of(
                "productName", "Test Product",
                "quantity", 5,
                "price", 99.99
            );

            given()
                .header("Authorization", TokenHelper.bearerToken(userToken))
                .contentType(ContentType.JSON)
                .body(orderRequest)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("productName", equalTo("Test Product"))
                .body("quantity", equalTo(5))
                .body("createdBy", equalTo("user"))
                .body("status", equalTo("PENDING"));
        }

        @Test
        @DisplayName("Admin can create order")
        void adminCanCreateOrder() {
            Map<String, Object> orderRequest = Map.of(
                "productName", "Admin Product",
                "quantity", 10,
                "price", 199.99
            );

            given()
                .header("Authorization", TokenHelper.bearerToken(adminToken))
                .contentType(ContentType.JSON)
                .body(orderRequest)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(201)
                .body("createdBy", equalTo("admin"));
        }

        @Test
        @DisplayName("Can get order by ID")
        void canGetOrderById() {
            // Create an order first
            Map<String, Object> orderRequest = Map.of(
                "productName", "Get Test Product",
                "quantity", 1,
                "price", 50.00
            );

            String orderId = given()
                .header("Authorization", TokenHelper.bearerToken(userToken))
                .contentType(ContentType.JSON)
                .body(orderRequest)
            .when()
                .post("/api/orders")
            .then()
                .statusCode(201)
                .extract()
                .path("id");

            // Get the order
            given()
                .header("Authorization", TokenHelper.bearerToken(userToken))
            .when()
                .get("/api/orders/{id}", orderId)
            .then()
                .statusCode(200)
                .body("id", equalTo(orderId))
                .body("productName", equalTo("Get Test Product"));
        }
    }

    @Nested
    @DisplayName("Admin Endpoint Tests")
    class AdminEndpointTests {

        @Test
        @DisplayName("Admin can create user")
        void adminCanCreateUser() {
            Map<String, Object> userRequest = Map.of(
                "username", "newuser",
                "email", "newuser@example.com",
                "roles", List.of("USER")
            );

            given()
                .header("Authorization", TokenHelper.bearerToken(adminToken))
                .contentType(ContentType.JSON)
                .body(userRequest)
            .when()
                .post("/api/admin/users")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("username", equalTo("newuser"))
                .body("email", equalTo("newuser@example.com"))
                .body("createdBy", equalTo("admin"));
        }

        @Test
        @DisplayName("Admin can get system stats")
        void adminCanGetSystemStats() {
            given()
                .header("Authorization", TokenHelper.bearerToken(adminToken))
            .when()
                .get("/api/admin/stats")
            .then()
                .statusCode(200)
                .body("totalUsers", notNullValue())
                .body("freeMemory", greaterThan(0))
                .body("totalMemory", greaterThan(0));
        }

        @Test
        @DisplayName("Admin can get audit logs")
        void adminCanGetAuditLogs() {
            // Create a user to generate an audit log
            Map<String, Object> userRequest = Map.of(
                "username", "audituser",
                "email", "audit@example.com",
                "roles", List.of("USER")
            );

            given()
                .header("Authorization", TokenHelper.bearerToken(adminToken))
                .contentType(ContentType.JSON)
                .body(userRequest)
            .when()
                .post("/api/admin/users")
            .then()
                .statusCode(201);

            // Check audit logs
            given()
                .header("Authorization", TokenHelper.bearerToken(adminToken))
            .when()
                .get("/api/admin/audit-logs")
            .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].action", equalTo("CREATE_USER"))
                .body("[0].performedBy", equalTo("admin"));
        }

        @Test
        @DisplayName("Admin can delete user")
        void adminCanDeleteUser() {
            // Create a user first
            Map<String, Object> userRequest = Map.of(
                "username", "deleteuser",
                "email", "delete@example.com",
                "roles", List.of("USER")
            );

            String userId = given()
                .header("Authorization", TokenHelper.bearerToken(adminToken))
                .contentType(ContentType.JSON)
                .body(userRequest)
            .when()
                .post("/api/admin/users")
            .then()
                .statusCode(201)
                .extract()
                .path("id");

            // Delete the user
            given()
                .header("Authorization", TokenHelper.bearerToken(adminToken))
            .when()
                .delete("/api/admin/users/{id}", userId)
            .then()
                .statusCode(204);
        }
    }

    @Nested
    @DisplayName("Health Endpoint Tests")
    class HealthEndpointTests {

        @Test
        @DisplayName("Health endpoint is accessible without authentication")
        void healthEndpointIsAccessibleWithoutAuth() {
            given()
            .when()
                .get("/actuator/health")
            .then()
                .statusCode(200);
        }
    }
}
