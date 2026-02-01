package com.example.s6.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Secured Order REST Controller.
 * Requires USER role for access.
 *
 * Demonstrates:
 * - JWT authentication
 * - Role-based authorization
 * - User context extraction from token
 */
@RestController
@RequestMapping("/api/orders")
public class SecuredOrderController {

    private final Map<UUID, OrderDto> orders = new ConcurrentHashMap<>();

    /**
     * Creates a new order.
     * Requires USER role.
     *
     * @param request the order creation request
     * @param jwt the authenticated user's JWT
     * @return the created order
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderDto> createOrder(
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String username = jwt.getClaimAsString("preferred_username");

        OrderDto order = new OrderDto(
            UUID.randomUUID(),
            request.productName(),
            request.quantity(),
            request.price(),
            username,
            Instant.now(),
            "PENDING"
        );

        orders.put(order.id(), order);

        return ResponseEntity.status(201).body(order);
    }

    /**
     * Gets all orders for the current user.
     * Requires USER role.
     *
     * @param jwt the authenticated user's JWT
     * @return list of user's orders
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<OrderDto>> getOrders(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");

        List<OrderDto> userOrders = orders.values().stream()
            .filter(order -> order.createdBy().equals(username))
            .toList();

        return ResponseEntity.ok(userOrders);
    }

    /**
     * Gets a specific order by ID.
     * Requires USER role.
     *
     * @param id the order ID
     * @return the order if found
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderDto> getOrder(@PathVariable UUID id) {
        OrderDto order = orders.get(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    /**
     * Gets current user information from JWT.
     *
     * @param jwt the authenticated user's JWT
     * @return user information
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserInfo> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UserInfo userInfo = new UserInfo(
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username"),
            jwt.getClaimAsString("email"),
            jwt.getClaimAsStringList("realm_access") != null
                ? jwt.getClaimAsStringList("realm_access")
                : List.of()
        );
        return ResponseEntity.ok(userInfo);
    }

    /**
     * Order DTO record.
     */
    public record OrderDto(
        UUID id,
        String productName,
        int quantity,
        BigDecimal price,
        String createdBy,
        Instant createdAt,
        String status
    ) {}

    /**
     * Create order request record.
     */
    public record CreateOrderRequest(
        String productName,
        int quantity,
        BigDecimal price
    ) {}

    /**
     * User information record.
     */
    public record UserInfo(
        String id,
        String username,
        String email,
        List<String> roles
    ) {}
}
