package com.example.s4;

import com.example.tc.containers.KafkaContainerFactory;
import com.example.tc.containers.PostgresContainerFactory;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Test configuration for S4 CDC scenario.
 * Provides PostgreSQL and Kafka containers using @ServiceConnection.
 */
@TestConfiguration(proxyBeanMethods = false)
public class S4TestApplication {

    private static final String CDC_TOPIC = "cdc.transactions";

    /**
     * PostgreSQL container with automatic Spring Boot configuration.
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> container = PostgresContainerFactory.getInstance();
        container.start();
        return container;
    }

    /**
     * Kafka container with automatic Spring Boot configuration.
     * Creates the CDC topic on startup.
     */
    @Bean
    @ServiceConnection
    public KafkaContainer kafkaContainer() {
        KafkaContainer container = KafkaContainerFactory.getInstance();
        container.start();

        // Create CDC topic
        createCdcTopic(container.getBootstrapServers());

        return container;
    }

    /**
     * Creates the CDC topic in Kafka.
     *
     * @param bootstrapServers the Kafka bootstrap servers
     */
    private void createCdcTopic(String bootstrapServers) {
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers
        );

        try (AdminClient adminClient = AdminClient.create(config)) {
            NewTopic topic = new NewTopic(CDC_TOPIC, 1, (short) 1);
            adminClient.createTopics(Collections.singletonList(topic)).all().get();
        } catch (ExecutionException e) {
            // Topic might already exist, which is fine
            if (!e.getMessage().contains("TopicExistsException")) {
                throw new RuntimeException("Failed to create CDC topic", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating CDC topic", e);
        }
    }
}
