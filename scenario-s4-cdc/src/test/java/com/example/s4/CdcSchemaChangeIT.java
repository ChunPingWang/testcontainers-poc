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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CDC handling of schema changes.
 * Validates that CDC events continue to work when schema evolves.
 *
 * Note: In a real Debezium setup, schema changes are handled automatically.
 * These tests verify that our CDC event processor handles events with
 * different field sets gracefully.
 */
@SpringBootTest
@Import(S4TestApplication.class)
@ActiveProfiles("test")
class CdcSchemaChangeIT extends IntegrationTestBase {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cdcEventProcessor.clearEvents();
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("CDC processor handles events with extra fields gracefully")
    void shouldHandleEventsWithExtraFields() throws JsonProcessingException {
        // Given - CDC event with additional fields not in our entity
        Map<String, Object> afterState = new HashMap<>();
        afterState.put("id", "550e8400-e29b-41d4-a716-446655440000");
        afterState.put("account_id", "ACC-EXTRA");
        afterState.put("type", "DEPOSIT");
        afterState.put("amount", "500.00");
        afterState.put("balance", "500.00");
        afterState.put("created_at", "2024-01-15T10:30:00Z");
        // Extra fields that might come from schema evolution
        afterState.put("description", "Initial deposit");
        afterState.put("currency", "USD");
        afterState.put("processed_by", "system");

        CdcEvent event = new CdcEvent("INSERT", TABLE_NAME, null, afterState);
        String eventJson = objectMapper.writeValueAsString(event);

        // When - Send event with extra fields
        kafkaTemplate.send(CDC_TOPIC, "extra-fields-key", eventJson);

        // Then - Event should be processed successfully
        AwaitHelper.waitForCdcEvent(() -> cdcEventProcessor.getEventCount() > 0);

        CdcEvent receivedEvent = cdcEventProcessor.getReceivedEvents().get(0);
        assertThat(receivedEvent.getOperation()).isEqualTo("INSERT");
        assertThat(receivedEvent.getAfter()).containsKey("account_id");
        assertThat(receivedEvent.getAfter()).containsKey("description");
        assertThat(receivedEvent.getAfter()).containsKey("currency");
    }

    @Test
    @DisplayName("CDC processor handles events with missing optional fields")
    void shouldHandleEventsWithMissingFields() throws JsonProcessingException {
        // Given - CDC event with minimal fields
        Map<String, Object> afterState = new HashMap<>();
        afterState.put("id", "550e8400-e29b-41d4-a716-446655440001");
        afterState.put("account_id", "ACC-MINIMAL");
        afterState.put("type", "WITHDRAWAL");
        // Missing amount, balance, created_at

        CdcEvent event = new CdcEvent("INSERT", TABLE_NAME, null, afterState);
        String eventJson = objectMapper.writeValueAsString(event);

        // When - Send event with missing fields
        kafkaTemplate.send(CDC_TOPIC, "missing-fields-key", eventJson);

        // Then - Event should be processed (processor doesn't validate fields)
        AwaitHelper.waitForCdcEvent(() -> cdcEventProcessor.getEventCount() > 0);

        CdcEvent receivedEvent = cdcEventProcessor.getReceivedEvents().get(0);
        assertThat(receivedEvent.getOperation()).isEqualTo("INSERT");
        assertThat(receivedEvent.getAfter().get("account_id")).isEqualTo("ACC-MINIMAL");
        assertThat(receivedEvent.getAfter().get("amount")).isNull();
    }

    @Test
    @DisplayName("CDC processor handles schema evolution - new column added")
    void shouldHandleNewColumnAddition() throws JsonProcessingException {
        // Simulate a scenario where a new column is added to the table
        // Old events don't have the new field, new events do

        // Old event format (before schema change)
        Map<String, Object> oldFormatEvent = new HashMap<>();
        oldFormatEvent.put("id", "550e8400-e29b-41d4-a716-446655440002");
        oldFormatEvent.put("account_id", "ACC-OLD");
        oldFormatEvent.put("type", "TRANSFER");
        oldFormatEvent.put("amount", "100.00");
        oldFormatEvent.put("balance", "900.00");
        oldFormatEvent.put("created_at", "2024-01-15T10:30:00Z");

        CdcEvent oldEvent = new CdcEvent("INSERT", TABLE_NAME, null, oldFormatEvent);
        kafkaTemplate.send(CDC_TOPIC, "old-format-key", objectMapper.writeValueAsString(oldEvent));

        // New event format (after schema change - with new field)
        Map<String, Object> newFormatEvent = new HashMap<>();
        newFormatEvent.put("id", "550e8400-e29b-41d4-a716-446655440003");
        newFormatEvent.put("account_id", "ACC-NEW");
        newFormatEvent.put("type", "DEPOSIT");
        newFormatEvent.put("amount", "200.00");
        newFormatEvent.put("balance", "1100.00");
        newFormatEvent.put("created_at", "2024-01-15T11:30:00Z");
        newFormatEvent.put("reference_number", "REF-12345"); // New column

        CdcEvent newEvent = new CdcEvent("INSERT", TABLE_NAME, null, newFormatEvent);
        kafkaTemplate.send(CDC_TOPIC, "new-format-key", objectMapper.writeValueAsString(newEvent));

        // Then - Both events should be processed
        AwaitHelper.waitForCdcEvent(() -> cdcEventProcessor.getEventCount() >= 2);

        List<CdcEvent> events = cdcEventProcessor.getReceivedEvents();
        assertThat(events).hasSize(2);

        // Verify old format event
        CdcEvent oldReceived = events.stream()
                .filter(e -> "ACC-OLD".equals(e.getAfter().get("account_id")))
                .findFirst()
                .orElseThrow();
        assertThat(oldReceived.getAfter()).doesNotContainKey("reference_number");

        // Verify new format event
        CdcEvent newReceived = events.stream()
                .filter(e -> "ACC-NEW".equals(e.getAfter().get("account_id")))
                .findFirst()
                .orElseThrow();
        assertThat(newReceived.getAfter()).containsKey("reference_number");
        assertThat(newReceived.getAfter().get("reference_number")).isEqualTo("REF-12345");
    }

    @Test
    @DisplayName("CDC processor handles data type changes in events")
    void shouldHandleDataTypeVariations() throws JsonProcessingException {
        // Test handling of different data type representations

        // Amount as string
        Map<String, Object> stringAmount = new HashMap<>();
        stringAmount.put("id", "550e8400-e29b-41d4-a716-446655440004");
        stringAmount.put("account_id", "ACC-STRING");
        stringAmount.put("type", "DEPOSIT");
        stringAmount.put("amount", "123.45");
        stringAmount.put("balance", "123.45");
        stringAmount.put("created_at", "2024-01-15T10:30:00Z");

        CdcEvent stringEvent = new CdcEvent("INSERT", TABLE_NAME, null, stringAmount);
        kafkaTemplate.send(CDC_TOPIC, "string-amount-key", objectMapper.writeValueAsString(stringEvent));

        // Amount as number
        Map<String, Object> numberAmount = new HashMap<>();
        numberAmount.put("id", "550e8400-e29b-41d4-a716-446655440005");
        numberAmount.put("account_id", "ACC-NUMBER");
        numberAmount.put("type", "DEPOSIT");
        numberAmount.put("amount", 234.56);
        numberAmount.put("balance", 234.56);
        numberAmount.put("created_at", "2024-01-15T10:30:00Z");

        CdcEvent numberEvent = new CdcEvent("INSERT", TABLE_NAME, null, numberAmount);
        kafkaTemplate.send(CDC_TOPIC, "number-amount-key", objectMapper.writeValueAsString(numberEvent));

        // Then - Both formats should be processed
        AwaitHelper.waitForCdcEvent(() -> cdcEventProcessor.getEventCount() >= 2);

        List<CdcEvent> events = cdcEventProcessor.getReceivedEvents();
        assertThat(events).hasSize(2);
    }

    @Test
    @DisplayName("CDC processor handles table rename simulation")
    void shouldHandleTableNameChanges() throws JsonProcessingException {
        // Simulate events coming from differently named tables
        Map<String, Object> afterState = createBasicTransactionMap("ACC-TABLE");

        // Event from original table
        CdcEvent originalTableEvent = new CdcEvent("INSERT", "transactions", null, afterState);
        kafkaTemplate.send(CDC_TOPIC, "original-table-key", objectMapper.writeValueAsString(originalTableEvent));

        // Event from renamed table (e.g., after migration)
        Map<String, Object> afterState2 = createBasicTransactionMap("ACC-TABLE-2");
        CdcEvent renamedTableEvent = new CdcEvent("INSERT", "financial_transactions", null, afterState2);
        kafkaTemplate.send(CDC_TOPIC, "renamed-table-key", objectMapper.writeValueAsString(renamedTableEvent));

        // Then - Both events should be processed
        AwaitHelper.waitForCdcEvent(() -> cdcEventProcessor.getEventCount() >= 2);

        List<CdcEvent> events = cdcEventProcessor.getReceivedEvents();
        assertThat(events).hasSize(2);

        // Verify table names are preserved
        assertThat(events).anyMatch(e -> "transactions".equals(e.getTable()));
        assertThat(events).anyMatch(e -> "financial_transactions".equals(e.getTable()));
    }

    @Test
    @DisplayName("CDC processor maintains REPLICA IDENTITY FULL behavior")
    void shouldVerifyReplicaIdentityFull() {
        // Verify that REPLICA IDENTITY FULL is set on the table
        String replicaIdentity = jdbcTemplate.queryForObject(
                "SELECT relreplident FROM pg_class WHERE relname = 'transactions'",
                String.class
        );

        // 'f' means REPLICA IDENTITY FULL
        assertThat(replicaIdentity).isEqualTo("f");
    }

    @Test
    @DisplayName("UPDATE event with REPLICA IDENTITY FULL includes all columns in before-state")
    void shouldIncludeAllColumnsInBeforeStateForUpdate() throws JsonProcessingException {
        // Given - Create a transaction
        Transaction transaction = new Transaction(
                "ACC-REPLICA",
                TransactionType.DEPOSIT,
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00")
        );
        transactionRepository.save(transaction);

        // Full before state (REPLICA IDENTITY FULL captures all columns)
        Map<String, Object> beforeState = new HashMap<>();
        beforeState.put("id", transaction.getId().toString());
        beforeState.put("account_id", "ACC-REPLICA");
        beforeState.put("type", "DEPOSIT");
        beforeState.put("amount", "1000.00");
        beforeState.put("balance", "1000.00");
        beforeState.put("created_at", transaction.getCreatedAt().toString());

        // After state with updated balance
        Map<String, Object> afterState = new HashMap<>(beforeState);
        afterState.put("balance", "1500.00");

        CdcEvent updateEvent = new CdcEvent("UPDATE", TABLE_NAME, beforeState, afterState);
        kafkaTemplate.send(CDC_TOPIC, transaction.getId().toString(), objectMapper.writeValueAsString(updateEvent));

        // Then - Verify before state has all columns
        AwaitHelper.waitForCdcEvent(() -> cdcEventProcessor.getEventCount() > 0);

        CdcEvent receivedEvent = cdcEventProcessor.getReceivedEvents().get(0);

        // With REPLICA IDENTITY FULL, before state should have ALL columns
        Map<String, Object> receivedBefore = receivedEvent.getBefore();
        assertThat(receivedBefore).containsKeys("id", "account_id", "type", "amount", "balance", "created_at");
    }

    /**
     * Creates a basic transaction map for testing.
     */
    private Map<String, Object> createBasicTransactionMap(String accountId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", java.util.UUID.randomUUID().toString());
        map.put("account_id", accountId);
        map.put("type", "DEPOSIT");
        map.put("amount", "100.00");
        map.put("balance", "100.00");
        map.put("created_at", java.time.Instant.now().toString());
        return map;
    }
}
