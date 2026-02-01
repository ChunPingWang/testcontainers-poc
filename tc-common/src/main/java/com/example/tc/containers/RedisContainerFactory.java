package com.example.tc.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating Redis test containers.
 * Uses singleton pattern for container reuse within test classes.
 */
public final class RedisContainerFactory {

    private static final String IMAGE = "redis:7-alpine";
    private static final int REDIS_PORT = 6379;

    private static final GenericContainer<?> INSTANCE = createContainer();

    private RedisContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton Redis container instance.
     *
     * @return the Redis container
     */
    public static GenericContainer<?> getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new Redis container instance.
     *
     * @return a new Redis container
     */
    public static GenericContainer<?> createNew() {
        return createContainer();
    }

    private static GenericContainer<?> createContainer() {
        return new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withExposedPorts(REDIS_PORT)
            .withReuse(true);
    }
}
