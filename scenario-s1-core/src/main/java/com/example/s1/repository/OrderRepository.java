package com.example.s1.repository;

import com.example.s1.domain.Order;
import com.example.s1.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Order persistence operations.
 * This is a port (driven port) in hexagonal architecture terms.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Finds all orders with a specific status.
     *
     * @param status the order status to filter by
     * @return list of orders with the given status
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Finds orders with pagination and optional status filter.
     *
     * @param status   the order status (optional)
     * @param pageable pagination information
     * @return page of orders
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Counts orders by status.
     *
     * @param status the order status
     * @return count of orders with the given status
     */
    long countByStatus(OrderStatus status);
}
