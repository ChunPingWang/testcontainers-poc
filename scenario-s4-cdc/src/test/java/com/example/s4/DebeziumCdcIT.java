package com.example.s4;

import com.example.s4.cdc.CdcEventProcessor;
import com.example.s4.cdc.CdcEventProcessor.CdcEvent;
import com.example.s4.domain.Transaction;
import com.example.s4.domain.TransactionType;
import com.example.s4.repository.TransactionRepository;
import com.example.tc.base.IntegrationTestBase;
import com.example.tc.util.AwaitHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CDC (Change Data Capture) functionality.
 * Validates SC-012: CDC events are received within 3 seconds.
 *
 * These tests simulate Debezium CDC events via Kafka to verify:
 * - INSERT events contain after-state
 * - UPDATE events contain both before and after state
 * - DELETE events contain before-state
 * - All events are received within 3 seconds
 */
@SpringBootTest
@Import(S4TestApplication.class)
@ActiveProfiles("test")
class DebeziumCdcIT extends IntegrationTestBase {

    private static final String CDC_TOPIC = "cdc.transactions";
    private static final String TABLE_NAME = "transactions";

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CdcEventProcessor cdcEventProcessor;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cdcEventProcessor.clearEvents();
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("SC-012: INSERT event is captured with after-state within 3 seconds")
    void shouldCaptureInsertEventWithinThreeSeconds() throws JsonProcessingException {
        // Given - Create a transaction in the database
        Transaction transaction = new Transaction(
                "ACC-001",
                TransactionType.DEPOSIT,
                new BigDecimal("100.00"),
                new BigDecimal("100.00")
        );
        transactionRepository.save(transaction);

        // Simulate CDC INSERT event (as Debezium would produce)
        Map<String, Object> afterState = createTransactionMap(transaction);
        CdcEvent insertEvent = new CdcEvent("INSERT", TABLE_NAME, null, afterState);
        String eventJson = objectMapper.writeValueAsString(insertEvent);

        // When - Send CDC event to Kafka
        long startTime = System.currentTimeMillis();
        kafkaTemplate.send(CDC_TOPIC, transaction.getId().toString(), eventJson);

        // Then - Verify event is received within 3 seconds (SC-012)
        AwaitHelper.waitForCdcEvent(() -> cdcEventProcessor.getEventCount() > 0);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration).isLessThan(3000L);

        List<CdcEvent> insertEvents = cdcEventProcessor.getEventsByOperation("INSERT");
        assertThat(insertEvents).hasSize(1);

        CdcEvent receivedEvent = insertEvents.get(0);
        assertThat(receivedEvent.getTable()).isEqualTo(TABLE_NAME);
        assertThat(receivedEvent.getBefore()).isNull();
        assertThat(receivedEvent.getAfter()).isNotNull();
        assertThat(receivedEvent.getAfter().get("account_id")).isEqualTo("ACC-001");
        assertThat(receivedEvent.getAfter().get("type")).isEqualTo("DEPOSIT");
        assertThat(receivedEvent.getAfter().get("amount")).isEqualTo("100.00");
    }

    @Test
    @DisplayName("SC-012: UPDATE event captures before and after state within 3 seconds")
    void shouldCaptureUpdateEventWithBeforeAndAfterState() throws JsonProcessingException {
        // Given - Create and save initial transaction
        Transaction transaction = new Transaction(
                "ACC-002",
                TransactionType.DEPOSIT,
                new BigDecimal("200.00"),
                new BigDecimal("200.00")
        );
        transactionRepository.save(transaction);

        Map<String, Object> beforeState = createTransactionMap(transaction);

        // Update the transaction
        BigDecimal newBalance = new BigDecimal("350.00");
        transaction.setBalance(newBalance);
        transactionRepository.save(transaction);

        Map<String, Object> afterState = createTransactionMap(transaction);

        // Simulate CDC UPDATE event
        CdcEvent updateEvent = new CdcEvent("UPDATE", TABLE_NAME, beforeState, afterState);
        String eventJson = objectMapper.writeValueAsString(updateEvent);

        // When - Send CDC event to Kafka
        long startTime = System.currentTimeMillis();
        kafkaTemplate.send(CDC_TOPIC, transaction.getId().toString(), eventJson);

        // Then - Verify event is received within 3 seconds (SC-012)
        AwaitHelper.waitForCdcEvent(() -> cdcEventProcessor.getEventCount() > 0);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration).isLessThan(3000L);

        List<CdcEvent> updateEvents = cdcEventProcessor.getEventsByOperation("UPDATE");
        assertThat(updateEvents).hasSize(1);

        CdcEvent receivedEvent = updateEvents.get(0);
        assertThat(receivedEvent.getTable()).isEqualTo(TABLE_NAME);

        // Verify before state
        assertThat(receivedEvent.getBefore()).isNotNull();
        assertThat(receivedEvent.getBefore().get("balance")).isEqualTo("200.00");

        // Verify after state
        assertThat(receivedEvent.getAfter()).isNotNull();
        assertThat(receivedEvent.getAfter().get("balance")).isEqualTo("350.00");
    }

    @Test
    @DisplayName("SC-012: DELETE event captures before-state within 3 seconds")
    void shouldCaptureDeleteEventWithBeforeState() throws JsonProcessingException {
        // Given - Create and save transaction
        Transaction transaction = new Transaction(
                "ACC-003",
                TransactionType.WITHDRAWAL,
                new BigDecimal("50.00"),
                new BigDecimal("150.00")
        );
        transactionRepository.save(transaction);

        Map<String, Object> beforeState = createTransactionMap(transaction);

        // Delete the transaction
        transactionRepository.delete(transaction);

        // Simulate CDC DELETE event
        CdcEvent deleteEvent = new CdcEvent("DELETE", TABLE_NAME, beforeState, null);
        String eventJson = objectMapper.writeValueAsString(deleteEvent);

        // When - Send CDC event to Kafka
        long startTime = System.currentTimeMillis();
        kafkaTemplate.send(CDC_TOPIC, transaction.getId().toString(), eventJson);

        // Then - Verify event is received within 3 seconds (SC-012)
        AwaitHelper.waitForCdcEvent(() -> cdcEventProcessor.getEventCount() > 0);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration).isLessThan(3000L);

        List<CdcEvent> deleteEvents = cdcEventProcessor.getEventsByOperation("DELETE");
        assertThat(deleteEvents).hasSize(1);

        CdcEvent receivedEvent = deleteEvents.get(0);
        assertThat(receivedEvent.getTable()).isEqualTo(TABLE_NAME);
        assertThat(receivedEvent.getBefore()).isNotNull();
        assertThat(receivedEvent.getBefore().get("account_id")).isEqualTo("ACC-003");
        assertThat(receivedEvent.getBefore().get("type")).isEqualTo("WITHDRAWAL");
        assertThat(receivedEvent.getAfter()).isNull();
    }

    @Test
    @DisplayName("Multiple CDC operations are captured in sequence")
    void shouldCaptureMultipleCdcOperations() throws JsonProcessingException {
        // Given - Multiple transactions
        Transaction tx1 = new Transaction("ACC-100", TransactionType.DEPOSIT, new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        Transaction tx2 = new Transaction("ACC-100", TransactionType.WITHDRAWAL, new BigDecimal("200.00"), new BigDecimal("800.00"));
        Transaction tx3 = new Transaction("ACC-100", TransactionType.TRANSFER, new BigDecimal("300.00"), new BigDecimal("500.00"));

        transactionRepository.saveAll(List.of(tx1, tx2, tx3));

        // Create and send INSERT events for all transactions
        for (Transaction tx : List.of(tx1, tx2, tx3)) {
            CdcEvent insertEvent = new CdcEvent("INSERT", TABLE_NAME, null, createTransactionMap(tx));
            kafkaTemplate.send(CDC_TOPIC, tx.getId().toString(), objectMapper.writeValueAsString(insertEvent));
        }

        // Then - Verify all events are received within 3 seconds
        AwaitHelper.waitForCdcEvent(() -> cdcEventProcessor.getEventCount() >= 3);

        List<CdcEvent> allEvents = cdcEventProcessor.getReceivedEvents();
        assertThat(allEvents).hasSize(3);

        // Verify all are INSERT operations
        assertThat(allEvents).allMatch(e -> "INSERT".equals(e.getOperation()));
    }

    @Test
    @DisplayName("CDC event contains transaction type information")
    void shouldCaptureTransactionTypeInCdcEvent() throws JsonProcessingException {
        // Test all transaction types
        for (TransactionType type : TransactionType.values()) {
            cdcEventProcessor.clearEvents();

            Transaction transaction = new Transaction(
                    "ACC-TYPE-TEST",
                    type,
                    new BigDecimal("100.00"),
                    new BigDecimal("100.00")
            );
            transactionRepository.save(transaction);

            CdcEvent event = new CdcEvent("INSERT", TABLE_NAME, null, createTransactionMap(transaction));
            kafkaTemplate.send(CDC_TOPIC, transaction.getId().toString(), objectMapper.writeValueAsString(event));

            AwaitHelper.waitForCdcEvent(() -> cdcEventProcessor.getEventCount() > 0);

            CdcEvent receivedEvent = cdcEventProcessor.getReceivedEvents().get(0);
            assertThat(receivedEvent.getAfter().get("type")).isEqualTo(type.name());
        }
    }

    /**
     * Creates a map representation of a Transaction for CDC events.
     */
    private Map<String, Object> createTransactionMap(Transaction tx) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tx.getId().toString());
        map.put("account_id", tx.getAccountId());
        map.put("type", tx.getType().name());
        map.put("amount", tx.getAmount().toString());
        map.put("balance", tx.getBalance().toString());
        map.put("created_at", tx.getCreatedAt().toString());
        return map;
    }
}
