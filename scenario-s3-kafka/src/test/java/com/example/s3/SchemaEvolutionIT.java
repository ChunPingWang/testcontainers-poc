package com.example.s3;

import com.example.s3.consumer.OrderEventConsumer;
import com.example.s3.producer.OrderEventProducer;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Avro schema evolution.
 * Tests backward compatibility and schema versioning with Schema Registry.
 *
 * Given: Schema Registry is running with schemas registered
 * When: Producer sends events with evolved schema
 * Then: Consumer can read events with backward compatible changes
 */
@SpringBootTest
@Import(S3TestApplication.class)
@ActiveProfiles("test")
class SchemaEvolutionIT extends BaseKafkaIT {

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private OrderEventConsumer orderEventConsumer;

    private Schema orderEventSchemaV1;
    private Schema orderEventSchemaV2;
    private SchemaRegistryClient schemaRegistryClient;

    @BeforeEach
    void setUp() throws IOException {
        orderEventConsumer.clearReceivedEvents();
        orderEventSchemaV1 = loadSchema("avro/order-event-v1.avsc");
        orderEventSchemaV2 = loadSchema("avro/order-event-v2.avsc");

        String schemaRegistryUrl = S3TestApplication.getSchemaRegistryUrl();
        schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, 100);
    }

    @Test
    @DisplayName("Should register V1 schema successfully")
    void shouldRegisterV1Schema() throws IOException, RestClientException {
        // Given
        String subject = "order-events-test-v1-value";

        // When
        int schemaId = schemaRegistryClient.register(subject, new AvroSchema(orderEventSchemaV1));

        // Then
        assertThat(schemaId).isPositive();
        assertThat(schemaRegistryClient.getLatestSchemaMetadata(subject).getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should produce and consume V1 schema events")
    void shouldProduceAndConsumeV1Events() {
        // Given
        String orderId = UUID.randomUUID().toString();
        GenericRecord event = createV1OrderEvent(orderId, "CUST-001", 150.0, "CREATED");

        // When
        orderEventProducer.sendOrderEventSync(orderId, event);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<OrderEventConsumer.ReceivedEvent> events = orderEventConsumer.getEventsByOrderId(orderId);
                assertThat(events).hasSize(1);

                GenericRecord received = events.get(0).event();
                assertThat(received.get("orderId").toString()).isEqualTo(orderId);
                assertThat(received.get("customerId").toString()).isEqualTo("CUST-001");
                assertThat((Double) received.get("amount")).isEqualTo(150.0);
            });
    }

    @Test
    @DisplayName("Should evolve schema to V2 with backward compatibility")
    void shouldEvolveSchemaWithBackwardCompatibility() throws IOException, RestClientException {
        // Given - Register V1 first
        String subject = "order-events-evolution-test-value";
        schemaRegistryClient.register(subject, new AvroSchema(orderEventSchemaV1));

        // When - Register V2 (backward compatible)
        int v2SchemaId = schemaRegistryClient.register(subject, new AvroSchema(orderEventSchemaV2));

        // Then
        assertThat(v2SchemaId).isPositive();
        assertThat(schemaRegistryClient.getLatestSchemaMetadata(subject).getVersion()).isEqualTo(2);

        // Verify compatibility
        boolean isCompatible = schemaRegistryClient.testCompatibility(
            subject, new AvroSchema(orderEventSchemaV2));
        assertThat(isCompatible).isTrue();
    }

    @Test
    @DisplayName("Should produce V2 events with new fields")
    void shouldProduceV2EventsWithNewFields() {
        // Given
        String orderId = UUID.randomUUID().toString();
        GenericRecord event = createV2OrderEvent(orderId, "CUST-002", 250.0, "CONFIRMED",
            "Test Product", 5);

        // When
        orderEventProducer.sendOrderEventSync(orderId, event);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<OrderEventConsumer.ReceivedEvent> events = orderEventConsumer.getEventsByOrderId(orderId);
                assertThat(events).hasSize(1);

                GenericRecord received = events.get(0).event();
                assertThat(received.get("orderId").toString()).isEqualTo(orderId);
                assertThat(received.get("productName").toString()).isEqualTo("Test Product");
                assertThat((Integer) received.get("quantity")).isEqualTo(5);
            });
    }

    @Test
    @DisplayName("Should produce V2 events with null optional fields")
    void shouldProduceV2EventsWithNullOptionalFields() {
        // Given - V2 event without the new optional fields
        String orderId = UUID.randomUUID().toString();
        GenericRecord event = createV2OrderEvent(orderId, "CUST-003", 300.0, "SHIPPED",
            null, null);

        // When
        orderEventProducer.sendOrderEventSync(orderId, event);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<OrderEventConsumer.ReceivedEvent> events = orderEventConsumer.getEventsByOrderId(orderId);
                assertThat(events).hasSize(1);

                GenericRecord received = events.get(0).event();
                assertThat(received.get("orderId").toString()).isEqualTo(orderId);
                assertThat(received.get("productName")).isNull();
                assertThat(received.get("quantity")).isNull();
            });
    }

    @Test
    @DisplayName("Should handle mixed V1 and V2 events")
    void shouldHandleMixedV1AndV2Events() {
        // Given
        String orderId1 = UUID.randomUUID().toString();
        String orderId2 = UUID.randomUUID().toString();

        GenericRecord v1Event = createV1OrderEvent(orderId1, "CUST-001", 100.0, "CREATED");
        GenericRecord v2Event = createV2OrderEvent(orderId2, "CUST-002", 200.0, "CONFIRMED",
            "Product V2", 3);

        // When
        orderEventProducer.sendOrderEventSync(orderId1, v1Event);
        orderEventProducer.sendOrderEventSync(orderId2, v2Event);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(orderEventConsumer.getReceivedEventCount()).isEqualTo(2);

                // V1 event - no new fields
                GenericRecord received1 = orderEventConsumer.getEventsByOrderId(orderId1).get(0).event();
                assertThat(received1.get("orderId").toString()).isEqualTo(orderId1);

                // V2 event - has new fields
                GenericRecord received2 = orderEventConsumer.getEventsByOrderId(orderId2).get(0).event();
                assertThat(received2.get("orderId").toString()).isEqualTo(orderId2);
                assertThat(received2.get("productName").toString()).isEqualTo("Product V2");
            });
    }

    @Test
    @DisplayName("Should verify schema compatibility check")
    void shouldVerifySchemaCompatibilityCheck() throws IOException, RestClientException {
        // Given
        String subject = "order-events-compat-test-value";
        schemaRegistryClient.register(subject, new AvroSchema(orderEventSchemaV1));

        // When/Then - V2 with nullable new fields is backward compatible
        boolean isCompatible = schemaRegistryClient.testCompatibility(
            subject, new AvroSchema(orderEventSchemaV2));
        assertThat(isCompatible).isTrue();
    }

    @Test
    @DisplayName("Should reject non-backward compatible schema")
    void shouldRejectNonBackwardCompatibleSchema() throws IOException, RestClientException {
        // Given
        String subject = "order-events-incompatible-test-value";
        schemaRegistryClient.register(subject, new AvroSchema(orderEventSchemaV1));

        // Create an incompatible schema (adding a required field without default)
        // In BACKWARD compatibility, new consumers must be able to read old data.
        // Adding a required field without default makes it impossible to read old data
        // (old records don't have this field), so it's BACKWARD INCOMPATIBLE.
        String incompatibleSchemaJson = """
            {
              "type": "record",
              "name": "OrderEvent",
              "namespace": "com.example.s3.avro",
              "fields": [
                {"name": "orderId", "type": "string"},
                {"name": "customerId", "type": "string"},
                {"name": "amount", "type": "double"},
                {"name": "status", "type": {"type": "enum", "name": "OrderStatus",
                    "symbols": ["CREATED", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"]}},
                {"name": "timestamp", "type": "long"},
                {"name": "requiredNewField", "type": "string"}
              ]
            }
            """;
        Schema incompatibleSchema = new Schema.Parser().parse(incompatibleSchemaJson);

        // When/Then - Should not be compatible (adding required field is backward incompatible)
        boolean isCompatible = schemaRegistryClient.testCompatibility(
            subject, new AvroSchema(incompatibleSchema));
        assertThat(isCompatible).isFalse();
    }

    @Test
    @DisplayName("Should list all versions of a schema")
    void shouldListAllSchemaVersions() throws IOException, RestClientException {
        // Given
        String subject = "order-events-versions-test-value";
        schemaRegistryClient.register(subject, new AvroSchema(orderEventSchemaV1));
        schemaRegistryClient.register(subject, new AvroSchema(orderEventSchemaV2));

        // When
        List<Integer> versions = schemaRegistryClient.getAllVersions(subject);

        // Then
        assertThat(versions).hasSize(2);
        assertThat(versions).containsExactly(1, 2);
    }

    private GenericRecord createV1OrderEvent(String orderId, String customerId, double amount, String status) {
        GenericRecord record = new GenericData.Record(orderEventSchemaV1);
        record.put("orderId", orderId);
        record.put("customerId", customerId);
        record.put("amount", amount);
        record.put("status", new GenericData.EnumSymbol(
            orderEventSchemaV1.getField("status").schema(), status));
        record.put("timestamp", System.currentTimeMillis());
        return record;
    }

    private GenericRecord createV2OrderEvent(String orderId, String customerId, double amount,
                                             String status, String productName, Integer quantity) {
        GenericRecord record = new GenericData.Record(orderEventSchemaV2);
        record.put("orderId", orderId);
        record.put("customerId", customerId);
        record.put("amount", amount);
        record.put("status", new GenericData.EnumSymbol(
            orderEventSchemaV2.getField("status").schema(), status));
        record.put("timestamp", System.currentTimeMillis());
        record.put("productName", productName);
        record.put("quantity", quantity);
        return record;
    }

    private Schema loadSchema(String schemaPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(schemaPath);
        return new Schema.Parser().parse(resource.getInputStream());
    }
}
