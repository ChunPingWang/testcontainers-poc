package com.example.s1;

import com.example.tc.containers.PostgresContainerFactory;
import com.example.tc.containers.RabbitMqContainerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Test configuration for S1 scenario.
 * Provides PostgreSQL and RabbitMQ containers using @ServiceConnection.
 */
@TestConfiguration(proxyBeanMethods = false)
public class S1TestApplication {

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
     * RabbitMQ container with automatic Spring Boot configuration.
     */
    @Bean
    @ServiceConnection
    public RabbitMQContainer rabbitMqContainer() {
        RabbitMQContainer container = RabbitMqContainerFactory.getInstance();
        container.start();
        return container;
    }
}
