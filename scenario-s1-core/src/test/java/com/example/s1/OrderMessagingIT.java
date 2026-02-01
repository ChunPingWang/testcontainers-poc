package com.example.s1;

import com.example.s1.domain.Order;
import com.example.s1.domain.OrderStatus;
import com.example.s1.messaging.OrderEventPublisher;
import com.example.s1.repository.OrderRepository;
import com.example.tc.base.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Order messaging (RabbitMQ).
 * Validates US2: 訂單處理端對端測試
 *
 * Given 訂單已成功儲存
 * When 系統發佈訂單建立事件
 * Then 消費者接收事件並更新訂單狀態為已確認
 */
@SpringBootTest
@Import(S1TestApplication.class)
@ActiveProfiles("test")
class OrderMessagingIT extends IntegrationTestBase {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Test
    void shouldPublishOrderCreatedEvent() {
        // Given
        Order order = createAndSaveOrder();

        // When
        orderEventPublisher.publishOrderCreated(order);

        // Then - 等待消費者處理事件並更新訂單狀態
        await().atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                Order updated = orderRepository.findById(order.getId()).orElseThrow();
                assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            });
    }

    @Test
    void shouldHandleMultipleOrderEvents() {
        // Given
        Order order1 = createAndSaveOrder();
        Order order2 = createAndSaveOrder();

        // When
        orderEventPublisher.publishOrderCreated(order1);
        orderEventPublisher.publishOrderCreated(order2);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(orderRepository.findById(order1.getId()).orElseThrow().getStatus())
                    .isEqualTo(OrderStatus.CONFIRMED);
                assertThat(orderRepository.findById(order2.getId()).orElseThrow().getStatus())
                    .isEqualTo(OrderStatus.CONFIRMED);
            });
    }

    private Order createAndSaveOrder() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerName("Test Customer");
        order.setProductName("Test Product");
        order.setQuantity(1);
        order.setAmount(new BigDecimal("100.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }
}
