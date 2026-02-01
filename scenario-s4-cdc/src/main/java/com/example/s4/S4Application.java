package com.example.s4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * S4 CDC Application - Change Data Capture Scenario.
 * Demonstrates PostgreSQL CDC with Debezium and Kafka using Testcontainers.
 */
@SpringBootApplication
public class S4Application {

    public static void main(String[] args) {
        SpringApplication.run(S4Application.class, args);
    }
}
