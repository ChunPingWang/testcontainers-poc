package com.example.s3;

import com.example.tc.base.IntegrationTestBase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for Kafka integration tests.
 * Provides dynamic property configuration for Kafka and Schema Registry.
 */
public abstract class BaseKafkaIT extends IntegrationTestBase {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", S3TestApplication::getBootstrapServers);
        registry.add("spring.kafka.properties.schema.registry.url", S3TestApplication::getSchemaRegistryUrl);
        registry.add("app.kafka.topics.order-events", () -> "order-events");
    }
}
