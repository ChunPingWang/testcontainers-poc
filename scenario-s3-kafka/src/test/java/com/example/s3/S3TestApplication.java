package com.example.s3;

import com.example.tc.containers.SchemaRegistryContainerFactory;
import org.apache.avro.generic.GenericRecord;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

/**
 * Test configuration for S3 Kafka scenario.
 * Provides Kafka and Schema Registry containers using KRaft mode (no ZooKeeper).
 */
@TestConfiguration(proxyBeanMethods = false)
public class S3TestApplication {

    private static final Network NETWORK = Network.newNetwork();
    private static final int SCHEMA_REGISTRY_PORT = 8081;

    private static final KafkaContainer KAFKA;
    private static final GenericContainer<?> SCHEMA_REGISTRY;

    static {
        // Initialize Kafka with network using Confluent Kafka image
        KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
            .withKraft()
            .withNetwork(NETWORK)
            .withNetworkAliases("kafka");
        KAFKA.start();

        // Initialize Schema Registry
        SCHEMA_REGISTRY = SchemaRegistryContainerFactory.create(KAFKA, NETWORK);
        SCHEMA_REGISTRY.start();
    }

    /**
     * Returns the Kafka container instance.
     *
     * @return Kafka container
     */
    @Bean
    public KafkaContainer kafkaContainer() {
        return KAFKA;
    }

    /**
     * Returns the Schema Registry container instance.
     *
     * @return Schema Registry container
     */
    @Bean
    public GenericContainer<?> schemaRegistryContainer() {
        return SCHEMA_REGISTRY;
    }

    /**
     * Returns the Kafka bootstrap servers URL.
     *
     * @return bootstrap servers URL
     */
    public static String getBootstrapServers() {
        return KAFKA.getBootstrapServers();
    }

    /**
     * Returns the Schema Registry URL.
     *
     * @return Schema Registry URL
     */
    public static String getSchemaRegistryUrl() {
        return "http://" + SCHEMA_REGISTRY.getHost() + ":" +
               SCHEMA_REGISTRY.getMappedPort(SCHEMA_REGISTRY_PORT);
    }

    /**
     * Provides a Kafka producer factory configured for Avro serialization.
     *
     * @return ProducerFactory for GenericRecord
     */
    @Bean
    @Primary
    public ProducerFactory<String, GenericRecord> producerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", KAFKA.getBootstrapServers());
        configs.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        configs.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        configs.put("schema.registry.url", getSchemaRegistryUrl());
        configs.put("acks", "all");
        configs.put("enable.idempotence", true);
        return new DefaultKafkaProducerFactory<>(configs);
    }

    /**
     * Provides a KafkaTemplate configured for Avro serialization.
     *
     * @param producerFactory the producer factory
     * @return KafkaTemplate for GenericRecord
     */
    @Bean
    @Primary
    public KafkaTemplate<String, GenericRecord> kafkaTemplate(
            ProducerFactory<String, GenericRecord> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Provides a Kafka consumer factory configured for Avro deserialization.
     *
     * @return ConsumerFactory for GenericRecord
     */
    @Bean
    @Primary
    public ConsumerFactory<String, GenericRecord> consumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", KAFKA.getBootstrapServers());
        configs.put("group.id", "order-event-consumer-group-test");
        configs.put("auto.offset.reset", "earliest");
        configs.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        configs.put("value.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        configs.put("schema.registry.url", getSchemaRegistryUrl());
        configs.put("specific.avro.reader", false);
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    /**
     * Provides a Kafka listener container factory configured for Avro deserialization.
     *
     * @param consumerFactory the consumer factory
     * @return ConcurrentKafkaListenerContainerFactory for GenericRecord
     */
    @Bean
    @Primary
    public ConcurrentKafkaListenerContainerFactory<String, GenericRecord> kafkaListenerContainerFactory(
            ConsumerFactory<String, GenericRecord> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, GenericRecord> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
