package com.example.s1.messaging;

import com.example.s1.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes order events to RabbitMQ.
 * This is an adapter (driven adapter) in hexagonal architecture terms.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    public static final String EXCHANGE = "order.exchange";
    public static final String ROUTING_KEY_CREATED = "order.created";

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes an order created event.
     *
     * @param order the created order
     */
    public void publishOrderCreated(Order order) {
        Map<String, Object> event = new HashMap<>();
        event.put("orderId", order.getId().toString());
        event.put("customerName", order.getCustomerName());
        event.put("amount", order.getAmount().toString());
        event.put("createdAt", order.getCreatedAt().toString());

        log.info("Publishing order created event for order: {}", order.getId());
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_CREATED, event);
    }
}
