package com.example.s1.messaging;

import com.example.s1.domain.Order;
import com.example.s1.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Consumes order events from RabbitMQ.
 * Processes order created events and updates order status.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    public static final String QUEUE = "order.created.queue";

    private final OrderRepository orderRepository;

    public OrderEventConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Handles order created events.
     * Confirms the order upon receiving the event.
     *
     * @param event the order created event
     */
    @RabbitListener(queues = QUEUE)
    @Transactional
    public void handleOrderCreated(Map<String, Object> event) {
        String orderIdStr = (String) event.get("orderId");
        log.info("Received order created event for order: {}", orderIdStr);

        try {
            UUID orderId = UUID.fromString(orderIdStr);
            orderRepository.findById(orderId).ifPresent(order -> {
                order.confirm();
                orderRepository.save(order);
                log.info("Order {} confirmed", orderId);
            });
        } catch (Exception e) {
            log.error("Error processing order created event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
