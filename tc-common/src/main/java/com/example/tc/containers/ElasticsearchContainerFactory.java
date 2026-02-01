package com.example.tc.containers;

import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating Elasticsearch test containers.
 * Uses singleton pattern for container reuse within test classes.
 */
public final class ElasticsearchContainerFactory {

    private static final String IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.13.0";

    private static final ElasticsearchContainer INSTANCE = createContainer();

    private ElasticsearchContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton Elasticsearch container instance.
     *
     * @return the Elasticsearch container
     */
    public static ElasticsearchContainer getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new Elasticsearch container instance.
     *
     * @return a new Elasticsearch container
     */
    public static ElasticsearchContainer createNew() {
        return createContainer();
    }

    private static ElasticsearchContainer createContainer() {
        return new ElasticsearchContainer(DockerImageName.parse(IMAGE))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withReuse(true);
    }
}
