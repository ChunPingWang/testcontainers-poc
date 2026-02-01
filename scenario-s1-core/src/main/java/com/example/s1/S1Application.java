package com.example.s1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * S1 Core Application - Basic Integration Testing Scenario.
 * Demonstrates PostgreSQL + RabbitMQ integration with Testcontainers.
 */
@SpringBootApplication
public class S1Application {

    public static void main(String[] args) {
        SpringApplication.run(S1Application.class, args);
    }
}
