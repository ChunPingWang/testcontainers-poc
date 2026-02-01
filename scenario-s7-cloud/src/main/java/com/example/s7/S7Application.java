package com.example.s7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * S7 Cloud Application - Cloud Service Integration Testing Scenario.
 * Demonstrates LocalStack (AWS S3/SQS/DynamoDB) and Azurite (Azure Blob)
 * integration with Testcontainers.
 */
@SpringBootApplication
public class S7Application {

    public static void main(String[] args) {
        SpringApplication.run(S7Application.class, args);
    }
}
