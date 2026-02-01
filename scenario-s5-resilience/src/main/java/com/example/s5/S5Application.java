package com.example.s5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * S5 Resilience Application - WireMock + Toxiproxy Integration Testing Scenario.
 * Demonstrates resilience patterns (circuit breaker, retry, fallback) with Testcontainers.
 *
 * This scenario validates:
 * - External service mocking with WireMock
 * - Network fault injection with Toxiproxy
 * - Circuit breaker state transitions (CLOSED -> OPEN -> HALF_OPEN -> CLOSED)
 * - Retry mechanism with exponential backoff
 * - Fallback responses on failure
 */
@SpringBootApplication
public class S5Application {

    public static void main(String[] args) {
        SpringApplication.run(S5Application.class, args);
    }
}
