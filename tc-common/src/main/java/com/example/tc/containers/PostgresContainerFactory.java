package com.example.tc.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating PostgreSQL test containers.
 * Uses singleton pattern for container reuse within test classes.
 */
public final class PostgresContainerFactory {

    private static final String IMAGE = "postgres:16-alpine";
    private static final String DATABASE_NAME = "testdb";
    private static final String USERNAME = "test";
    private static final String PASSWORD = "test";

    private static final PostgreSQLContainer<?> INSTANCE = createContainer();

    private PostgresContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton PostgreSQL container instance.
     *
     * @return the PostgreSQL container
     */
    public static PostgreSQLContainer<?> getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new PostgreSQL container instance.
     * Used for scenarios requiring isolated containers.
     *
     * @return a new PostgreSQL container
     */
    public static PostgreSQLContainer<?> createNew() {
        return createContainer();
    }

    private static PostgreSQLContainer<?> createContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse(IMAGE))
            .withDatabaseName(DATABASE_NAME)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withReuse(true);
    }
}
