package com.example.s2;

import com.example.tc.base.IntegrationTestBase;
import com.example.tc.containers.ElasticsearchContainerFactory;
import com.example.tc.containers.PostgresContainerFactory;
import com.example.tc.containers.RedisContainerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Base class for S2 Multi-Store integration tests.
 * Provides container configuration with @DynamicPropertySource.
 */
public abstract class S2IntegrationTestBase extends IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES_CONTAINER;
    static final GenericContainer<?> REDIS_CONTAINER;
    static final ElasticsearchContainer ELASTICSEARCH_CONTAINER;

    static {
        POSTGRES_CONTAINER = PostgresContainerFactory.getInstance();
        POSTGRES_CONTAINER.start();

        REDIS_CONTAINER = RedisContainerFactory.getInstance();
        REDIS_CONTAINER.start();

        ELASTICSEARCH_CONTAINER = ElasticsearchContainerFactory.getInstance();
        ELASTICSEARCH_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL properties
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

        // Redis properties
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));

        // Elasticsearch properties
        registry.add("spring.elasticsearch.uris", () ->
            "http://" + ELASTICSEARCH_CONTAINER.getHost() + ":" + ELASTICSEARCH_CONTAINER.getMappedPort(9200));
    }
}
