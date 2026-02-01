# Scenario S4: CDC (Change Data Capture) with Debezium

This module demonstrates Change Data Capture (CDC) patterns using PostgreSQL, Kafka, and simulated Debezium events with Testcontainers.

## Overview

CDC captures row-level changes in the database and streams them as events. This scenario implements a financial transaction system where every INSERT, UPDATE, and DELETE operation generates a CDC event containing the before and/or after state of the row.

## Components

### Domain Model

- **Transaction**: Financial transaction entity with id, accountId, type, amount, balance, and createdAt
- **TransactionType**: Enum with DEPOSIT, WITHDRAWAL, TRANSFER

### CDC Infrastructure

- **CdcEventProcessor**: Kafka listener that processes CDC events and maintains event history
- **CdcEvent**: Data structure mimicking Debezium's event format with operation, table, before/after state

### Database Configuration

The `transactions` table is configured with `REPLICA IDENTITY FULL` to ensure complete row data is captured in UPDATE and DELETE events:

```sql
ALTER TABLE transactions REPLICA IDENTITY FULL;
```

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   PostgreSQL    │────▶│     Kafka       │────▶│ CdcEventProcessor│
│  (Transactions) │     │ (CDC Events)    │     │  (Consumer)     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

In production, Debezium Connect sits between PostgreSQL and Kafka, automatically capturing changes from the WAL (Write-Ahead Log). For this PoC, we simulate CDC events to focus on testing the event processing logic.

## CDC Event Format

Events follow Debezium's structure:

```json
{
  "operation": "INSERT|UPDATE|DELETE",
  "table": "transactions",
  "before": { ... },  // null for INSERT
  "after": { ... },   // null for DELETE
  "timestamp": 1705312200000
}
```

### Operation Types

| Operation | Before State | After State |
|-----------|--------------|-------------|
| INSERT    | null         | complete row |
| UPDATE    | complete row | complete row |
| DELETE    | complete row | null |

## Test Scenarios

### DebeziumCdcIT

Tests CDC event capture within the 3-second SLA (SC-012):

1. **shouldCaptureInsertEventWithinThreeSeconds** - Verifies INSERT events contain after-state
2. **shouldCaptureUpdateEventWithBeforeAndAfterState** - Verifies UPDATE events contain both states
3. **shouldCaptureDeleteEventWithBeforeState** - Verifies DELETE events contain before-state
4. **shouldCaptureMultipleCdcOperations** - Tests sequential operations
5. **shouldCaptureTransactionTypeInCdcEvent** - Verifies all transaction types are captured

### CdcSchemaChangeIT

Tests CDC resilience to schema evolution:

1. **shouldHandleEventsWithExtraFields** - New fields don't break processing
2. **shouldHandleEventsWithMissingFields** - Missing fields are handled gracefully
3. **shouldHandleNewColumnAddition** - Schema migration compatibility
4. **shouldHandleDataTypeVariations** - Different data type representations
5. **shouldHandleTableNameChanges** - Table rename scenarios
6. **shouldVerifyReplicaIdentityFull** - Confirms CDC configuration
7. **shouldIncludeAllColumnsInBeforeStateForUpdate** - REPLICA IDENTITY FULL validation

## Running Tests

```bash
# Run all S4 tests
./gradlew :scenario-s4-cdc:test

# Run specific test class
./gradlew :scenario-s4-cdc:test --tests "DebeziumCdcIT"

# Run with verbose output
./gradlew :scenario-s4-cdc:test --info
```

## Configuration

### application.yml

```yaml
cdc:
  topic: cdc.transactions
  group-id: cdc-processor

spring:
  kafka:
    consumer:
      auto-offset-reset: earliest
```

## Performance Requirements

- **SC-012**: CDC events must be received within 3 seconds of the database change
- Events are validated using `AwaitHelper.waitForCdcEvent()` which enforces the 3-second timeout

## Dependencies

- Spring Boot Data JPA
- Spring Kafka
- PostgreSQL with REPLICA IDENTITY FULL
- Testcontainers (PostgreSQL, Kafka)
- tc-common (container factories and test utilities)

## Production Considerations

For production Debezium deployment:

1. **Debezium Connect Cluster**: Deploy Kafka Connect with Debezium PostgreSQL connector
2. **PostgreSQL Configuration**: Enable logical replication (`wal_level = logical`)
3. **Replication Slot**: Debezium creates a replication slot for WAL streaming
4. **Schema Registry**: Consider Avro serialization for schema evolution
5. **Monitoring**: Track replication lag and connector health

## Related Scenarios

- **S3-Kafka**: Basic Kafka messaging patterns
- **S2-MultiStore**: Multi-database consistency patterns
