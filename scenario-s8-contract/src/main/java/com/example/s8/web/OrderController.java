package com.example.s8.web;

import com.example.s8.dto.CreateOrderRequest;
import com.example.s8.dto.OrderDto;
import com.example.s8.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Order operations.
 * Implements the contract endpoints:
 * - GET /api/orders/{id} - returns order with id, customerName, status
 * - POST /api/orders - creates a new order
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Gets an order by ID.
     * Contract: Returns order with id, customerName, status.
     *
     * @param id the order ID
     * @return the order if found, 404 otherwise
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable UUID id) {
        return orderService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new order.
     * Contract: Accepts customerName, returns created order.
     *
     * @param request the order creation request
     * @return the created order with 201 status
     */
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest request) {
        OrderDto order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
