package com.example.tc.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating Schema Registry test containers.
 * Requires a running Kafka container for bootstrap servers.
 */
public final class SchemaRegistryContainerFactory {

    private static final String IMAGE = "confluentinc/cp-schema-registry:7.6.0";
    private static final int SCHEMA_REGISTRY_PORT = 8081;

    private SchemaRegistryContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a Schema Registry container connected to the given Kafka container.
     *
     * @param kafka  the Kafka container to connect to
     * @param network the Docker network to use
     * @return a configured Schema Registry container
     */
    public static GenericContainer<?> create(KafkaContainer kafka, Network network) {
        return new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withNetwork(network)
            .withNetworkAliases("schema-registry")
            .withExposedPorts(SCHEMA_REGISTRY_PORT)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + SCHEMA_REGISTRY_PORT)
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                "PLAINTEXT://" + kafka.getNetworkAliases().get(0) + ":9092")
            .dependsOn(kafka);
    }
}
