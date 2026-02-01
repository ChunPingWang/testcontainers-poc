package com.example.tc.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RabbitMqContainerFactory.
 * Verifies singleton pattern and container configuration.
 */
class RabbitMqContainerFactoryTest {

    @Test
    void getInstance_shouldReturnSameInstance() {
        // Given
        RabbitMQContainer first = RabbitMqContainerFactory.getInstance();

        // When
        RabbitMQContainer second = RabbitMqContainerFactory.getInstance();

        // Then
        assertSame(first, second, "Should return the same singleton instance");
    }

    @Test
    void getInstance_shouldReturnConfiguredContainer() {
        // When
        RabbitMQContainer container = RabbitMqContainerFactory.getInstance();

        // Then
        assertNotNull(container, "Container should not be null");
        assertTrue(container.getDockerImageName().contains("rabbitmq"),
            "Should use RabbitMQ image");
    }

    @Test
    void getInstance_shouldHaveManagementEnabled() {
        // When
        RabbitMQContainer container = RabbitMqContainerFactory.getInstance();

        // Then
        assertTrue(container.getDockerImageName().contains("management"),
            "Should use management image for admin console access");
    }
}
