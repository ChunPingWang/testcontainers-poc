# Data Model: Testcontainers Integration Testing PoC

**Date**: 2026-02-01
**Branch**: `001-testcontainers-poc`
**Plan Reference**: [plan.md](./plan.md)

## Overview

本文件定義各場景模組的資料模型，遵循 DDD 戰術設計原則。每個場景有獨立的 Bounded Context，模組間無共享實體。

---

## S1: Core (DB + MQ + API)

### Entity: Order

訂單實體，用於端對端測試驗證。

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | 訂單識別碼 |
| customerName | String | NOT NULL, max 100 | 客戶名稱 |
| productName | String | NOT NULL, max 200 | 產品名稱 |
| quantity | Integer | NOT NULL, > 0 | 數量 |
| amount | BigDecimal | NOT NULL, >= 0 | 金額 |
| status | OrderStatus | NOT NULL | 訂單狀態 |
| createdAt | Instant | NOT NULL | 建立時間 |
| updatedAt | Instant | NOT NULL | 更新時間 |

### Value Object: OrderStatus

```java
public enum OrderStatus {
    PENDING,    // 待處理
    CONFIRMED,  // 已確認
    SHIPPED,    // 已出貨
    DELIVERED,  // 已送達
    CANCELLED   // 已取消
}
```

### State Transitions

```
PENDING → CONFIRMED → SHIPPED → DELIVERED
    ↓
CANCELLED
```

### Domain Event: OrderCreatedEvent

| Field | Type | Description |
|-------|------|-------------|
| orderId | UUID | 訂單識別碼 |
| customerName | String | 客戶名稱 |
| amount | BigDecimal | 金額 |
| createdAt | Instant | 事件發生時間 |

---

## S2: Multi-Store (PostgreSQL + Redis + ES)

### Entity: Customer

客戶實體，用於多儲存層一致性測試。

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | 客戶識別碼 |
| name | String | NOT NULL, max 100 | 客戶名稱 |
| email | String | NOT NULL, UNIQUE | 電子郵件 |
| phone | String | max 20 | 電話 |
| address | String | max 500 | 地址 |
| createdAt | Instant | NOT NULL | 建立時間 |
| updatedAt | Instant | NOT NULL | 更新時間 |

### Cache Model (Redis)

```json
{
  "key": "customer:{id}",
  "value": {
    "id": "uuid",
    "name": "string",
    "email": "string"
  },
  "ttl": 300
}
```

### Search Index (Elasticsearch)

```json
{
  "index": "customers",
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "name": { "type": "text", "analyzer": "standard" },
      "email": { "type": "keyword" },
      "address": { "type": "text", "analyzer": "standard" }
    }
  }
}
```

---

## S3: Kafka + Schema Registry

### Avro Schema: OrderEvent v1

```json
{
  "type": "record",
  "name": "OrderEvent",
  "namespace": "com.example.s3.avro",
  "fields": [
    { "name": "orderId", "type": "string" },
    { "name": "customerName", "type": "string" },
    { "name": "amount", "type": "double" },
    { "name": "timestamp", "type": "long" }
  ]
}
```

### Avro Schema: OrderEvent v2 (Backward Compatible)

```json
{
  "type": "record",
  "name": "OrderEvent",
  "namespace": "com.example.s3.avro",
  "fields": [
    { "name": "orderId", "type": "string" },
    { "name": "customerName", "type": "string" },
    { "name": "amount", "type": "double" },
    { "name": "timestamp", "type": "long" },
    { "name": "productName", "type": ["null", "string"], "default": null }
  ]
}
```

---

## S4: CDC (Debezium)

### Entity: Transaction

交易實體，用於 CDC 測試。

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, NOT NULL | 交易識別碼 |
| accountId | String | NOT NULL | 帳號 |
| type | TransactionType | NOT NULL | 交易類型 |
| amount | BigDecimal | NOT NULL | 金額 |
| balance | BigDecimal | NOT NULL | 餘額 |
| createdAt | Instant | NOT NULL | 建立時間 |

### Value Object: TransactionType

```java
public enum TransactionType {
    DEPOSIT,    // 存款
    WITHDRAWAL, // 提款
    TRANSFER    // 轉帳
}
```

### CDC Event Structure (Debezium)

```json
{
  "before": { "id": "...", "amount": 100.00, "balance": 500.00 },
  "after": { "id": "...", "amount": 100.00, "balance": 600.00 },
  "op": "u",
  "ts_ms": 1706745600000,
  "source": {
    "connector": "postgresql",
    "db": "testdb",
    "table": "transactions"
  }
}
```

---

## S5: Resilience (WireMock + Toxiproxy)

### External API: Credit Check

模擬外部聯徵 API。

**Request**:
```json
{
  "customerId": "string"
}
```

**Response (Success)**:
```json
{
  "customerId": "string",
  "creditScore": 750,
  "approved": true
}
```

**Response (Error)**:
```json
{
  "error": "SERVICE_UNAVAILABLE",
  "message": "Credit check service is temporarily unavailable"
}
```

---

## S6: Security (Keycloak + Vault)

### Keycloak Realm Configuration

| Entity | Value | Description |
|--------|-------|-------------|
| Realm | testcontainers-poc | 測試用 Realm |
| Client | tc-client | OAuth2 Client |
| Users | admin, user | 測試用帳號 |
| Roles | ADMIN, USER | 角色 |

### User Credentials

| Username | Password | Roles |
|----------|----------|-------|
| admin | admin123 | ADMIN, USER |
| user | user123 | USER |

### Vault Secret Structure

```text
secret/database
├── username: "dynamic-user-xxx"
├── password: "dynamic-pass-xxx"
└── ttl: 3600
```

---

## S7: Cloud (LocalStack + Azurite)

### S3 Bucket Structure

| Bucket | Purpose |
|--------|---------|
| documents | 文件儲存 |
| reports | 報表儲存 |

### SQS Queue Structure

| Queue | DLQ | Purpose |
|-------|-----|---------|
| order-queue | order-queue-dlq | 訂單處理 |

### DynamoDB Table

| Table | PK | SK | Attributes |
|-------|----|----|------------|
| Orders | orderId (S) | - | customerName, amount, status |

### Azure Blob Container

| Container | Purpose |
|-----------|---------|
| documents | 文件儲存 |

---

## S8: Contract (Pact)

### Consumer Contract: OrderService

```json
{
  "consumer": { "name": "order-consumer" },
  "provider": { "name": "order-service" },
  "interactions": [
    {
      "description": "get order by id",
      "request": {
        "method": "GET",
        "path": "/api/orders/123"
      },
      "response": {
        "status": 200,
        "body": {
          "id": "123",
          "customerName": "Test Customer",
          "status": "CONFIRMED"
        }
      }
    }
  ]
}
```

---

## Entity Relationship Summary

```text
S1: Order (Aggregate Root)
    └── OrderStatus (Value Object)
    └── OrderCreatedEvent (Domain Event)

S2: Customer (Aggregate Root)
    └── CustomerCache (Read Model)
    └── CustomerIndex (Search Model)

S3: OrderEvent (Avro Message)
    └── v1, v2 (Schema Evolution)

S4: Transaction (Aggregate Root)
    └── TransactionType (Value Object)
    └── CdcEvent (Change Event)

S5: CreditCheckRequest/Response (DTO)

S6: User, Role (Keycloak Entities)
    └── VaultSecret (Dynamic Credential)

S7: S3Object, SqsMessage, DynamoItem (Cloud Entities)
    └── BlobItem (Azure Entity)

S8: PactContract (Consumer/Provider)
```

---

## Database Migrations

### S1: V1__create_orders_table.sql

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    amount DECIMAL(19,2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
```

### S2: V1__create_customers_table.sql

```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_customers_email ON customers(email);
```

### S4: V1__create_transactions_table.sql

```sql
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    account_id VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    balance DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Enable logical replication for Debezium CDC
ALTER TABLE transactions REPLICA IDENTITY FULL;
CREATE INDEX idx_transactions_account ON transactions(account_id);
```
