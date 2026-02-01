package com.example.s2;

import com.example.tc.containers.ElasticsearchContainerFactory;
import com.example.tc.containers.PostgresContainerFactory;
import com.example.tc.containers.RedisContainerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Test configuration for S2 Multi-Store scenario.
 * Provides PostgreSQL, Redis, and Elasticsearch containers.
 *
 * Note: Dynamic properties for Redis and Elasticsearch are configured
 * via the static initializer which starts the containers before
 * Spring context initialization.
 */
@TestConfiguration(proxyBeanMethods = false)
public class S2TestApplication {

    static final GenericContainer<?> REDIS_CONTAINER;
    static final ElasticsearchContainer ELASTICSEARCH_CONTAINER;
    static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        // Initialize and start all containers
        POSTGRES_CONTAINER = PostgresContainerFactory.getInstance();
        POSTGRES_CONTAINER.start();

        REDIS_CONTAINER = RedisContainerFactory.getInstance();
        REDIS_CONTAINER.start();

        ELASTICSEARCH_CONTAINER = ElasticsearchContainerFactory.getInstance();
        ELASTICSEARCH_CONTAINER.start();

        // Set system properties for Redis and Elasticsearch
        System.setProperty("spring.data.redis.host", REDIS_CONTAINER.getHost());
        System.setProperty("spring.data.redis.port", String.valueOf(REDIS_CONTAINER.getMappedPort(6379)));
        System.setProperty("spring.elasticsearch.uris",
            "http://" + ELASTICSEARCH_CONTAINER.getHost() + ":" + ELASTICSEARCH_CONTAINER.getMappedPort(9200));
    }

    /**
     * PostgreSQL container with automatic Spring Boot configuration.
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES_CONTAINER;
    }

    /**
     * Redis container bean for injection if needed.
     */
    @Bean
    public GenericContainer<?> redisContainer() {
        return REDIS_CONTAINER;
    }

    /**
     * Elasticsearch container bean for injection if needed.
     */
    @Bean
    public ElasticsearchContainer elasticsearchContainer() {
        return ELASTICSEARCH_CONTAINER;
    }
}
