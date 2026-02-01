package com.example.s1;

import com.example.s1.domain.Order;
import com.example.s1.domain.OrderStatus;
import com.example.s1.repository.OrderRepository;
import com.example.tc.base.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OrderRepository.
 * Validates US1: 本機執行單一場景測試
 *
 * Given 開發人員已安裝 Docker
 * When 執行單一模組測試指令
 * Then 僅啟動該模組所需的容器，測試完成後容器自動清理
 */
@SpringBootTest
@Import(S1TestApplication.class)
@ActiveProfiles("test")
class OrderRepositoryIT extends IntegrationTestBase {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldSaveAndFindOrder() {
        // Given
        Order order = createTestOrder();

        // When
        Order saved = orderRepository.save(order);
        Optional<Order> found = orderRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerName()).isEqualTo("Test Customer");
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldFindOrdersByStatus() {
        // Given
        Order pendingOrder = createTestOrder();
        pendingOrder.setStatus(OrderStatus.PENDING);
        orderRepository.save(pendingOrder);

        Order confirmedOrder = createTestOrder();
        confirmedOrder.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(confirmedOrder);

        // When
        var pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        var confirmedOrders = orderRepository.findByStatus(OrderStatus.CONFIRMED);

        // Then
        assertThat(pendingOrders).isNotEmpty();
        assertThat(confirmedOrders).isNotEmpty();
    }

    @Test
    void shouldUpdateOrderStatus() {
        // Given
        Order order = createTestOrder();
        Order saved = orderRepository.save(order);

        // When
        saved.setStatus(OrderStatus.CONFIRMED);
        Order updated = orderRepository.save(saved);

        // Then
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(saved.getCreatedAt());
    }

    private Order createTestOrder() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerName("Test Customer");
        order.setProductName("Test Product");
        order.setQuantity(1);
        order.setAmount(new BigDecimal("100.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return order;
    }
}
