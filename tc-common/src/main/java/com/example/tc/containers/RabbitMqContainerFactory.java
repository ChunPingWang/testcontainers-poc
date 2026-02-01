package com.example.tc.containers;

import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating RabbitMQ test containers.
 * Uses singleton pattern for container reuse within test classes.
 */
public final class RabbitMqContainerFactory {

    private static final String IMAGE = "rabbitmq:3.13-management-alpine";

    private static final RabbitMQContainer INSTANCE = createContainer();

    private RabbitMqContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton RabbitMQ container instance.
     *
     * @return the RabbitMQ container
     */
    public static RabbitMQContainer getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new RabbitMQ container instance.
     *
     * @return a new RabbitMQ container
     */
    public static RabbitMQContainer createNew() {
        return createContainer();
    }

    private static RabbitMQContainer createContainer() {
        return new RabbitMQContainer(DockerImageName.parse(IMAGE))
            .withReuse(true);
    }
}
