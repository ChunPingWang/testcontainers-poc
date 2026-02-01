package com.example.s3.consumer;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kafka consumer for order events.
 * Consumes order events from Kafka topic using Avro deserialization.
 * Maintains received events for testing purposes.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    /**
     * Stores all received events for verification.
     */
    private final List<ReceivedEvent> receivedEvents = new CopyOnWriteArrayList<>();

    /**
     * Stores events grouped by orderId to verify ordering.
     */
    private final Map<String, List<ReceivedEvent>> eventsByOrderId = new ConcurrentHashMap<>();

    /**
     * Listens to order events from the configured topic.
     *
     * @param record the Kafka consumer record containing the order event
     */
    @KafkaListener(
        topics = "${app.kafka.topics.order-events}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, GenericRecord> record) {
        String orderId = record.key();
        GenericRecord event = record.value();

        log.info("Received order event - orderId: {}, partition: {}, offset: {}, event: {}",
            orderId, record.partition(), record.offset(), event);

        ReceivedEvent receivedEvent = new ReceivedEvent(
            orderId,
            event,
            record.partition(),
            record.offset(),
            System.currentTimeMillis()
        );

        receivedEvents.add(receivedEvent);
        eventsByOrderId
            .computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>())
            .add(receivedEvent);

        processEvent(orderId, event);
    }

    /**
     * Processes the received order event.
     * Override in subclass or enhance for actual business logic.
     *
     * @param orderId the order ID
     * @param event   the order event
     */
    protected void processEvent(String orderId, GenericRecord event) {
        log.debug("Processing order event for orderId: {}, status: {}",
            orderId, event.get("status"));
    }

    /**
     * Returns all received events.
     *
     * @return unmodifiable list of all received events
     */
    public List<ReceivedEvent> getReceivedEvents() {
        return Collections.unmodifiableList(new ArrayList<>(receivedEvents));
    }

    /**
     * Returns events for a specific orderId in the order they were received.
     *
     * @param orderId the order ID
     * @return list of events for the order, or empty list if none
     */
    public List<ReceivedEvent> getEventsByOrderId(String orderId) {
        return eventsByOrderId.getOrDefault(orderId, Collections.emptyList());
    }

    /**
     * Returns the count of received events.
     *
     * @return number of events received
     */
    public int getReceivedEventCount() {
        return receivedEvents.size();
    }

    /**
     * Clears all received events.
     * Useful for resetting state between tests.
     */
    public void clearReceivedEvents() {
        receivedEvents.clear();
        eventsByOrderId.clear();
        log.debug("Cleared all received events");
    }

    /**
     * Record class to hold received event data.
     */
    public record ReceivedEvent(
        String orderId,
        GenericRecord event,
        int partition,
        long offset,
        long receivedAt
    ) {}
}
