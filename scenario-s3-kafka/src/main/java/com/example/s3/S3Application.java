package com.example.s3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * S3 Kafka Application - Kafka + Schema Registry Scenario.
 * Demonstrates Kafka with Avro serialization and schema evolution using Testcontainers.
 */
@SpringBootApplication
public class S3Application {

    public static void main(String[] args) {
        SpringApplication.run(S3Application.class, args);
    }
}
