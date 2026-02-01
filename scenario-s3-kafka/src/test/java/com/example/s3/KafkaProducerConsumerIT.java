package com.example.s3;

import com.example.s3.consumer.OrderEventConsumer;
import com.example.s3.producer.OrderEventProducer;
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
 * Integration tests for Kafka producer and consumer.
 * Tests message production, consumption, and partition ordering.
 *
 * Given: Kafka and Schema Registry containers are running
 * When: Producer sends order events
 * Then: Consumer receives events correctly with proper ordering
 */
@SpringBootTest
@Import(S3TestApplication.class)
@ActiveProfiles("test")
class KafkaProducerConsumerIT extends BaseKafkaIT {

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private OrderEventConsumer orderEventConsumer;

    private Schema orderEventSchema;

    @BeforeEach
    void setUp() throws IOException {
        orderEventConsumer.clearReceivedEvents();
        orderEventSchema = loadSchema("avro/order-event-v1.avsc");
    }

    @Test
    @DisplayName("Should produce and consume order event successfully")
    void shouldProduceAndConsumeOrderEvent() {
        // Given
        String orderId = UUID.randomUUID().toString();
        GenericRecord event = createOrderEvent(orderId, "CUST-001", 100.0, "CREATED");

        // When
        orderEventProducer.sendOrderEventSync(orderId, event);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<OrderEventConsumer.ReceivedEvent> events = orderEventConsumer.getEventsByOrderId(orderId);
                assertThat(events).hasSize(1);

                GenericRecord received = events.get(0).event();
                assertThat(received.get("orderId").toString()).isEqualTo(orderId);
                assertThat(received.get("customerId").toString()).isEqualTo("CUST-001");
                assertThat((Double) received.get("amount")).isEqualTo(100.0);
                assertThat(received.get("status").toString()).isEqualTo("CREATED");
            });
    }

    @Test
    @DisplayName("Should maintain event ordering for same partition key")
    void shouldMaintainEventOrderingForSamePartitionKey() {
        // Given - Multiple events for the same order (same partition key)
        String orderId = UUID.randomUUID().toString();
        GenericRecord event1 = createOrderEvent(orderId, "CUST-001", 100.0, "CREATED");
        GenericRecord event2 = createOrderEvent(orderId, "CUST-001", 100.0, "CONFIRMED");
        GenericRecord event3 = createOrderEvent(orderId, "CUST-001", 100.0, "SHIPPED");

        // When - Send events in sequence
        orderEventProducer.sendOrderEventSync(orderId, event1);
        orderEventProducer.sendOrderEventSync(orderId, event2);
        orderEventProducer.sendOrderEventSync(orderId, event3);

        // Then - Events should be received in the same order
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<OrderEventConsumer.ReceivedEvent> events = orderEventConsumer.getEventsByOrderId(orderId);
                assertThat(events).hasSize(3);

                // Verify ordering
                assertThat(events.get(0).event().get("status").toString()).isEqualTo("CREATED");
                assertThat(events.get(1).event().get("status").toString()).isEqualTo("CONFIRMED");
                assertThat(events.get(2).event().get("status").toString()).isEqualTo("SHIPPED");

                // Verify all events went to the same partition
                int partition = events.get(0).partition();
                assertThat(events).allMatch(e -> e.partition() == partition);

                // Verify offsets are increasing
                assertThat(events.get(0).offset()).isLessThan(events.get(1).offset());
                assertThat(events.get(1).offset()).isLessThan(events.get(2).offset());
            });
    }

    @Test
    @DisplayName("Should handle multiple orders concurrently")
    void shouldHandleMultipleOrdersConcurrently() {
        // Given - Multiple different orders
        String orderId1 = UUID.randomUUID().toString();
        String orderId2 = UUID.randomUUID().toString();
        String orderId3 = UUID.randomUUID().toString();

        // When - Send events for different orders
        orderEventProducer.sendOrderEventSync(orderId1, createOrderEvent(orderId1, "CUST-001", 100.0, "CREATED"));
        orderEventProducer.sendOrderEventSync(orderId2, createOrderEvent(orderId2, "CUST-002", 200.0, "CREATED"));
        orderEventProducer.sendOrderEventSync(orderId3, createOrderEvent(orderId3, "CUST-003", 300.0, "CREATED"));

        // Then - All events should be received
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(orderEventConsumer.getReceivedEventCount()).isEqualTo(3);

                assertThat(orderEventConsumer.getEventsByOrderId(orderId1)).hasSize(1);
                assertThat(orderEventConsumer.getEventsByOrderId(orderId2)).hasSize(1);
                assertThat(orderEventConsumer.getEventsByOrderId(orderId3)).hasSize(1);

                // Verify amounts
                assertThat((Double) orderEventConsumer.getEventsByOrderId(orderId1).get(0).event().get("amount"))
                    .isEqualTo(100.0);
                assertThat((Double) orderEventConsumer.getEventsByOrderId(orderId2).get(0).event().get("amount"))
                    .isEqualTo(200.0);
                assertThat((Double) orderEventConsumer.getEventsByOrderId(orderId3).get(0).event().get("amount"))
                    .isEqualTo(300.0);
            });
    }

    @Test
    @DisplayName("Should distribute events across partitions based on key")
    void shouldDistributeEventsAcrossPartitions() {
        // Given - Multiple orders that should hash to different partitions
        int eventCount = 30;

        // When - Send events for many different orders
        for (int i = 0; i < eventCount; i++) {
            String orderId = "ORDER-" + i;
            GenericRecord event = createOrderEvent(orderId, "CUST-" + i, 100.0 + i, "CREATED");
            orderEventProducer.sendOrderEventSync(orderId, event);
        }

        // Then - Events should be distributed across partitions
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<OrderEventConsumer.ReceivedEvent> events = orderEventConsumer.getReceivedEvents();
                assertThat(events).hasSize(eventCount);

                // Check that we used multiple partitions
                long distinctPartitions = events.stream()
                    .map(OrderEventConsumer.ReceivedEvent::partition)
                    .distinct()
                    .count();

                // With 30 events and 3 partitions, we should see at least 2 partitions used
                assertThat(distinctPartitions).isGreaterThanOrEqualTo(2);
            });
    }

    private GenericRecord createOrderEvent(String orderId, String customerId, double amount, String status) {
        GenericRecord record = new GenericData.Record(orderEventSchema);
        record.put("orderId", orderId);
        record.put("customerId", customerId);
        record.put("amount", amount);
        record.put("status", new GenericData.EnumSymbol(
            orderEventSchema.getField("status").schema(), status));
        record.put("timestamp", System.currentTimeMillis());
        return record;
    }

    private Schema loadSchema(String schemaPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(schemaPath);
        return new Schema.Parser().parse(resource.getInputStream());
    }
}
