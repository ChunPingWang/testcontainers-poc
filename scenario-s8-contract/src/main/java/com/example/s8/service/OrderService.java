package com.example.s8.service;

import com.example.s8.dto.CreateOrderRequest;
import com.example.s8.dto.OrderDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory order service for contract testing demonstration.
 * Uses in-memory storage to avoid database dependencies in contract tests.
 */
@Service
public class OrderService {

    private final Map<UUID, OrderDto> orders = new ConcurrentHashMap<>();

    /**
     * Creates a new order.
     *
     * @param request the order creation request
     * @return the created order
     */
    public OrderDto createOrder(CreateOrderRequest request) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        OrderDto order = new OrderDto(
            id,
            request.customerName(),
            "PENDING",
            now,
            now
        );

        orders.put(id, order);
        return order;
    }

    /**
     * Finds an order by ID.
     *
     * @param id the order ID
     * @return the order if found
     */
    public Optional<OrderDto> findById(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }

    /**
     * Adds an order directly to the store (for testing purposes).
     *
     * @param order the order to add
     */
    public void addOrder(OrderDto order) {
        orders.put(order.id(), order);
    }

    /**
     * Clears all orders (for testing purposes).
     */
    public void clearAll() {
        orders.clear();
    }
}
