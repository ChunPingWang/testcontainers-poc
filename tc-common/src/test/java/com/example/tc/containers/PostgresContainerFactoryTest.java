package com.example.tc.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PostgresContainerFactory.
 * Verifies singleton pattern and container configuration.
 */
class PostgresContainerFactoryTest {

    @Test
    void getInstance_shouldReturnSameInstance() {
        // Given
        PostgreSQLContainer<?> first = PostgresContainerFactory.getInstance();

        // When
        PostgreSQLContainer<?> second = PostgresContainerFactory.getInstance();

        // Then
        assertSame(first, second, "Should return the same singleton instance");
    }

    @Test
    void getInstance_shouldReturnConfiguredContainer() {
        // When
        PostgreSQLContainer<?> container = PostgresContainerFactory.getInstance();

        // Then
        assertNotNull(container, "Container should not be null");
        assertTrue(container.getDockerImageName().contains("postgres"),
            "Should use PostgreSQL image");
    }

    @Test
    void getInstance_shouldHaveCorrectDatabaseName() {
        // When
        PostgreSQLContainer<?> container = PostgresContainerFactory.getInstance();

        // Then
        assertEquals("testdb", container.getDatabaseName(),
            "Database name should be 'testdb'");
    }

    @Test
    void getInstance_shouldHaveCorrectCredentials() {
        // When
        PostgreSQLContainer<?> container = PostgresContainerFactory.getInstance();

        // Then
        assertEquals("test", container.getUsername(), "Username should be 'test'");
        assertEquals("test", container.getPassword(), "Password should be 'test'");
    }
}
