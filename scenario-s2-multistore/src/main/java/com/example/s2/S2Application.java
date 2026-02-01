package com.example.s2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * S2 Multi-Store Application - Multi-Store Integration Testing Scenario.
 * Demonstrates PostgreSQL + Redis + Elasticsearch integration with Testcontainers.
 *
 * This scenario validates:
 * - Cache read-through and write-through patterns
 * - Search index synchronization
 * - Data consistency across multiple stores
 */
@SpringBootApplication
public class S2Application {

    public static void main(String[] args) {
        SpringApplication.run(S2Application.class, args);
    }
}
