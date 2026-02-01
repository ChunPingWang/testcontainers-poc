package com.example.s6;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * S6 Security Application - OAuth2/OIDC Security Testing Scenario.
 * Demonstrates Keycloak + Vault integration with Testcontainers.
 *
 * Features:
 * - OAuth2 Resource Server with JWT validation
 * - Role-based access control (RBAC)
 * - Dynamic credentials from Vault
 */
@SpringBootApplication
public class S6Application {

    public static void main(String[] args) {
        SpringApplication.run(S6Application.class, args);
    }
}
