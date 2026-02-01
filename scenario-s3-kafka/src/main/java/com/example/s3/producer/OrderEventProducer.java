package com.example.s3.producer;

import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for order events.
 * Sends order events to Kafka topic using Avro serialization.
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, GenericRecord> kafkaTemplate;
    private final String orderEventsTopic;

    public OrderEventProducer(
            KafkaTemplate<String, GenericRecord> kafkaTemplate,
            @Value("${app.kafka.topics.order-events}") String orderEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderEventsTopic = orderEventsTopic;
    }

    /**
     * Sends an order event to Kafka.
     * Uses the orderId as the message key to ensure events for the same order
     * are sent to the same partition, maintaining order.
     *
     * @param orderId the order ID (used as partition key)
     * @param event   the order event to send
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, GenericRecord>> sendOrderEvent(
            String orderId, GenericRecord event) {
        log.debug("Sending order event for orderId: {} to topic: {}", orderId, orderEventsTopic);

        return kafkaTemplate.send(orderEventsTopic, orderId, event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Order event sent successfully for orderId: {}, partition: {}, offset: {}",
                        orderId,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send order event for orderId: {}", orderId, ex);
                }
            });
    }

    /**
     * Sends an order event synchronously and waits for acknowledgment.
     *
     * @param orderId the order ID (used as partition key)
     * @param event   the order event to send
     * @return SendResult with metadata about the sent message
     */
    public SendResult<String, GenericRecord> sendOrderEventSync(
            String orderId, GenericRecord event) {
        try {
            return sendOrderEvent(orderId, event).get();
        } catch (Exception e) {
            log.error("Error sending order event synchronously for orderId: {}", orderId, e);
            throw new RuntimeException("Failed to send order event", e);
        }
    }
}
