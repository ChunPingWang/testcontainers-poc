package com.example.tc.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating Debezium Connect test containers.
 * Requires both Kafka and PostgreSQL containers.
 */
public final class DebeziumContainerFactory {

    private static final String IMAGE = "debezium/connect:2.6";
    private static final int CONNECT_PORT = 8083;

    private DebeziumContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a Debezium Connect container connected to Kafka and PostgreSQL.
     *
     * @param kafka    the Kafka container
     * @param postgres the PostgreSQL container
     * @param network  the Docker network
     * @return a configured Debezium Connect container
     */
    public static GenericContainer<?> create(
            KafkaContainer kafka,
            PostgreSQLContainer<?> postgres,
            Network network) {

        return new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withNetwork(network)
            .withNetworkAliases("debezium")
            .withExposedPorts(CONNECT_PORT)
            .withEnv("BOOTSTRAP_SERVERS", kafka.getNetworkAliases().get(0) + ":9092")
            .withEnv("GROUP_ID", "1")
            .withEnv("CONFIG_STORAGE_TOPIC", "connect_configs")
            .withEnv("OFFSET_STORAGE_TOPIC", "connect_offsets")
            .withEnv("STATUS_STORAGE_TOPIC", "connect_statuses")
            .withEnv("KEY_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .dependsOn(kafka, postgres);
    }

    /**
     * Gets the Debezium Connect REST API port.
     *
     * @return the Connect port
     */
    public static int getConnectPort() {
        return CONNECT_PORT;
    }
}
