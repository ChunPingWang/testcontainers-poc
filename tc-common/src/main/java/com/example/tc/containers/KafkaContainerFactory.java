package com.example.tc.containers;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating Kafka test containers.
 * Uses Confluent Kafka image for better compatibility with Docker Desktop.
 */
public final class KafkaContainerFactory {

    private static final String IMAGE = "confluentinc/cp-kafka:7.6.0";

    private static final KafkaContainer INSTANCE = createContainer();

    private KafkaContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton Kafka container instance.
     *
     * @return the Kafka container
     */
    public static KafkaContainer getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new Kafka container instance.
     *
     * @return a new Kafka container
     */
    public static KafkaContainer createNew() {
        return createContainer();
    }

    private static KafkaContainer createContainer() {
        return new KafkaContainer(DockerImageName.parse(IMAGE))
            .withKraft()
            .withReuse(true);
    }
}
