package com.example.s1.service;

import com.example.s1.domain.Order;
import com.example.s1.domain.OrderStatus;
import com.example.s1.messaging.OrderEventPublisher;
import com.example.s1.repository.OrderRepository;
import com.example.tc.dto.CreateOrderRequest;
import com.example.tc.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Application service for Order operations.
 * Orchestrates domain logic and infrastructure concerns.
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    /**
     * Creates a new order.
     *
     * @param request the order creation request
     * @return the created order response
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerName(request.customerName());
        order.setProductName(request.productName());
        order.setQuantity(request.quantity());
        order.setAmount(request.amount());

        Order saved = orderRepository.save(order);

        // Publish order created event
        orderEventPublisher.publishOrderCreated(saved);

        return toResponse(saved);
    }

    /**
     * Finds an order by ID.
     *
     * @param id the order ID
     * @return the order if found
     */
    @Transactional(readOnly = true)
    public Optional<OrderResponse> findById(UUID id) {
        return orderRepository.findById(id).map(this::toResponse);
    }

    /**
     * Lists all orders with pagination.
     *
     * @param status   optional status filter
     * @param pageable pagination information
     * @return page of order responses
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(this::toResponse);
    }

    /**
     * Confirms an order.
     *
     * @param id the order ID
     * @return the updated order response
     */
    public Optional<OrderResponse> confirmOrder(UUID id) {
        return orderRepository.findById(id)
            .map(order -> {
                order.confirm();
                return toResponse(orderRepository.save(order));
            });
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getCustomerName(),
            order.getProductName(),
            order.getQuantity(),
            order.getAmount(),
            order.getStatus().name(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
