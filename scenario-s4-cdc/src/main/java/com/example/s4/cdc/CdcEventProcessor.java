package com.example.s4.cdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Processes CDC (Change Data Capture) events from Kafka.
 * This component listens to CDC events and maintains a list of received events
 * for testing and verification purposes.
 *
 * In a production scenario, this would be connected to Debezium Connect.
 * For this PoC, we simulate CDC events via direct Kafka messages.
 */
@Component
public class CdcEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(CdcEventProcessor.class);

    private final ObjectMapper objectMapper;
    private final List<CdcEvent> receivedEvents = new CopyOnWriteArrayList<>();

    public CdcEventProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Processes CDC events from the transactions CDC topic.
     *
     * @param message the CDC event message as JSON
     */
    @KafkaListener(topics = "${cdc.topic:cdc.transactions}", groupId = "${cdc.group-id:cdc-processor}")
    public void processEvent(String message) {
        log.debug("Received CDC event: {}", message);
        try {
            CdcEvent event = objectMapper.readValue(message, CdcEvent.class);
            receivedEvents.add(event);
            log.info("Processed CDC event: operation={}, table={}", event.getOperation(), event.getTable());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse CDC event: {}", message, e);
        }
    }

    /**
     * Gets all received CDC events.
     *
     * @return list of received CDC events
     */
    public List<CdcEvent> getReceivedEvents() {
        return List.copyOf(receivedEvents);
    }

    /**
     * Clears all received events.
     * Useful for resetting state between tests.
     */
    public void clearEvents() {
        receivedEvents.clear();
    }

    /**
     * Gets the count of received events.
     *
     * @return the number of received events
     */
    public int getEventCount() {
        return receivedEvents.size();
    }

    /**
     * Gets events filtered by operation type.
     *
     * @param operation the operation type (INSERT, UPDATE, DELETE)
     * @return list of matching events
     */
    public List<CdcEvent> getEventsByOperation(String operation) {
        return receivedEvents.stream()
                .filter(e -> operation.equalsIgnoreCase(e.getOperation()))
                .toList();
    }

    /**
     * Represents a CDC event with before/after state.
     * This structure mimics Debezium's event format.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CdcEvent {
        private String operation;
        private String table;
        private Map<String, Object> before;
        private Map<String, Object> after;
        private long timestamp;

        public CdcEvent() {
        }

        public CdcEvent(String operation, String table, Map<String, Object> before, Map<String, Object> after) {
            this.operation = operation;
            this.table = table;
            this.before = before;
            this.after = after;
            this.timestamp = System.currentTimeMillis();
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public Map<String, Object> getBefore() {
            return before;
        }

        public void setBefore(Map<String, Object> before) {
            this.before = before;
        }

        public Map<String, Object> getAfter() {
            return after;
        }

        public void setAfter(Map<String, Object> after) {
            this.after = after;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "CdcEvent{" +
                    "operation='" + operation + '\'' +
                    ", table='" + table + '\'' +
                    ", before=" + before +
                    ", after=" + after +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}
