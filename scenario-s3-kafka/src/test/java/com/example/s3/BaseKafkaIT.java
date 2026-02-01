package com.example.s3;

import com.example.tc.base.IntegrationTestBase;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Collection;

/**
 * Base class for Kafka integration tests.
 * Provides dynamic property configuration for Kafka and Schema Registry.
 */
public abstract class BaseKafkaIT extends IntegrationTestBase {

    private static final Logger log = LoggerFactory.getLogger(BaseKafkaIT.class);
    private static boolean subjectsCleared = false;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", S3TestApplication::getBootstrapServers);
        registry.add("spring.kafka.properties.schema.registry.url", S3TestApplication::getSchemaRegistryUrl);
        registry.add("app.kafka.topics.order-events", () -> "order-events");
    }

    @BeforeAll
    static void clearSchemaRegistry() {
        if (subjectsCleared) {
            return;
        }

        try {
            String schemaRegistryUrl = S3TestApplication.getSchemaRegistryUrl();
            SchemaRegistryClient client = new CachedSchemaRegistryClient(schemaRegistryUrl, 100);

            Collection<String> subjects = client.getAllSubjects();
            for (String subject : subjects) {
                try {
                    client.deleteSubject(subject, true);
                    log.info("Deleted schema subject: {}", subject);
                } catch (Exception e) {
                    log.warn("Failed to delete subject {}: {}", subject, e.getMessage());
                }
            }
            subjectsCleared = true;
        } catch (Exception e) {
            log.warn("Failed to clear Schema Registry subjects: {}", e.getMessage());
        }
    }
}
