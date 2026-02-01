# Tasks: Testcontainers Integration Testing PoC

**Input**: Design documents from `/specs/001-testcontainers-poc/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: TDD 是專案憲章的核心原則（NON-NEGOTIABLE），所有任務包含測試先行。

**Organization**: 任務依使用者故事分組，每個故事可獨立實作與測試。本專案採用 Gradle Monorepo Multi-Module 架構，每個場景對應獨立模組。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可平行執行（不同檔案、無相依）
- **[Story]**: 對應的使用者故事（US1, US2, US3...）
- 包含精確的檔案路徑

## Path Conventions

本專案採用 Gradle Monorepo 結構：

```text
testcontainers-poc/
├── tc-common/src/main/java/com/example/tc/
├── scenario-s1-core/src/main/java/com/example/s1/
├── scenario-s1-core/src/test/java/com/example/s1/
├── scenario-s2-multistore/src/...
└── ... (s3-s8 modules)
```

---

## Phase 1: Setup (Monorepo 基礎設施)

**Purpose**: 建立 Gradle Monorepo 骨架與共用模組

- [ ] T001 建立 Gradle wrapper 與 root build.gradle.kts in build.gradle.kts
- [ ] T002 建立 Version Catalog in gradle/libs.versions.toml
- [ ] T003 建立 settings.gradle.kts 註冊所有模組 in settings.gradle.kts
- [ ] T004 建立 gradle.properties 啟用平行建置 in gradle.properties
- [ ] T005 [P] 建立 tc-common 模組骨架 in tc-common/build.gradle.kts
- [ ] T006 [P] 建立 scenario-s1-core 模組骨架 in scenario-s1-core/build.gradle.kts
- [ ] T007 [P] 建立 scenario-s2-multistore 模組骨架 in scenario-s2-multistore/build.gradle.kts
- [ ] T008 [P] 建立 scenario-s3-kafka 模組骨架 in scenario-s3-kafka/build.gradle.kts
- [ ] T009 [P] 建立 scenario-s4-cdc 模組骨架 in scenario-s4-cdc/build.gradle.kts
- [ ] T010 [P] 建立 scenario-s5-resilience 模組骨架 in scenario-s5-resilience/build.gradle.kts
- [ ] T011 [P] 建立 scenario-s6-security 模組骨架 in scenario-s6-security/build.gradle.kts
- [ ] T012 [P] 建立 scenario-s7-cloud 模組骨架 in scenario-s7-cloud/build.gradle.kts
- [ ] T013 [P] 建立 scenario-s8-contract 模組骨架 in scenario-s8-contract/build.gradle.kts

---

## Phase 2: Foundational (tc-common 共用基礎設施)

**Purpose**: 建立所有場景模組共用的容器定義與測試基底類別

**⚠️ CRITICAL**: 所有使用者故事必須等待此階段完成

### Tests (TDD - 先寫測試)

- [ ] T014 [P] 撰寫 PostgresContainerFactory 單元測試 in tc-common/src/test/java/com/example/tc/containers/PostgresContainerFactoryTest.java
- [ ] T015 [P] 撰寫 RabbitMqContainerFactory 單元測試 in tc-common/src/test/java/com/example/tc/containers/RabbitMqContainerFactoryTest.java
- [ ] T016 [P] 撰寫 IntegrationTestBase 測試 in tc-common/src/test/java/com/example/tc/base/IntegrationTestBaseTest.java

### Implementation

- [ ] T017 [P] 實作 PostgresContainerFactory in tc-common/src/main/java/com/example/tc/containers/PostgresContainerFactory.java
- [ ] T018 [P] 實作 RabbitMqContainerFactory in tc-common/src/main/java/com/example/tc/containers/RabbitMqContainerFactory.java
- [ ] T019 [P] 實作 RedisContainerFactory in tc-common/src/main/java/com/example/tc/containers/RedisContainerFactory.java
- [ ] T020 [P] 實作 ElasticsearchContainerFactory in tc-common/src/main/java/com/example/tc/containers/ElasticsearchContainerFactory.java
- [ ] T021 [P] 實作 KafkaContainerFactory in tc-common/src/main/java/com/example/tc/containers/KafkaContainerFactory.java
- [ ] T022 [P] 實作 SchemaRegistryContainerFactory in tc-common/src/main/java/com/example/tc/containers/SchemaRegistryContainerFactory.java
- [ ] T023 [P] 實作 DebeziumContainerFactory in tc-common/src/main/java/com/example/tc/containers/DebeziumContainerFactory.java
- [ ] T024 [P] 實作 WireMockContainerFactory in tc-common/src/main/java/com/example/tc/containers/WireMockContainerFactory.java
- [ ] T025 [P] 實作 ToxiproxyContainerFactory in tc-common/src/main/java/com/example/tc/containers/ToxiproxyContainerFactory.java
- [ ] T026 [P] 實作 KeycloakContainerFactory in tc-common/src/main/java/com/example/tc/containers/KeycloakContainerFactory.java
- [ ] T027 [P] 實作 VaultContainerFactory in tc-common/src/main/java/com/example/tc/containers/VaultContainerFactory.java
- [ ] T028 [P] 實作 LocalStackContainerFactory in tc-common/src/main/java/com/example/tc/containers/LocalStackContainerFactory.java
- [ ] T029 [P] 實作 AzuriteContainerFactory in tc-common/src/main/java/com/example/tc/containers/AzuriteContainerFactory.java
- [ ] T030 [P] 實作 PactBrokerContainerFactory in tc-common/src/main/java/com/example/tc/containers/PactBrokerContainerFactory.java
- [ ] T031 實作 IntegrationTestBase in tc-common/src/main/java/com/example/tc/base/IntegrationTestBase.java
- [ ] T032 [P] 實作 CreateOrderRequest DTO in tc-common/src/main/java/com/example/tc/dto/CreateOrderRequest.java
- [ ] T033 [P] 實作 OrderResponse DTO in tc-common/src/main/java/com/example/tc/dto/OrderResponse.java
- [ ] T034 [P] 實作 AwaitHelper 工具 in tc-common/src/main/java/com/example/tc/util/AwaitHelper.java
- [ ] T035 [P] 實作 TokenHelper 工具 in tc-common/src/main/java/com/example/tc/util/TokenHelper.java

**Checkpoint**: tc-common 模組完成，所有場景模組可開始實作

---

## Phase 3: User Story 1+2 - S1 基礎整合場景 (Priority: P1) 🎯 MVP

**Goal**: 驗證 Testcontainers 管理 PostgreSQL + RabbitMQ，實現訂單端對端測試

**Independent Test**: `./gradlew :scenario-s1-core:test`

**Maps to**: US1 (本機執行單一場景測試) + US2 (訂單處理端對端測試)

### Tests (TDD - 先寫測試) ⚠️

> **NOTE: 測試必須先寫且失敗，才能開始實作**

- [ ] T036 [P] [US1] 撰寫 OrderRepositoryIT in scenario-s1-core/src/test/java/com/example/s1/OrderRepositoryIT.java
- [ ] T037 [P] [US2] 撰寫 OrderMessagingIT in scenario-s1-core/src/test/java/com/example/s1/OrderMessagingIT.java
- [ ] T038 [P] [US2] 撰寫 OrderApiIT in scenario-s1-core/src/test/java/com/example/s1/OrderApiIT.java

### Implementation

- [ ] T039 [P] [US1] 建立 S1Application in scenario-s1-core/src/main/java/com/example/s1/S1Application.java
- [ ] T040 [P] [US1] 建立 OrderStatus enum in scenario-s1-core/src/main/java/com/example/s1/domain/OrderStatus.java
- [ ] T041 [P] [US1] 建立 Order entity in scenario-s1-core/src/main/java/com/example/s1/domain/Order.java
- [ ] T042 [US1] 建立 OrderRepository in scenario-s1-core/src/main/java/com/example/s1/repository/OrderRepository.java
- [ ] T043 [US2] 實作 OrderService in scenario-s1-core/src/main/java/com/example/s1/service/OrderService.java
- [ ] T044 [P] [US2] 實作 OrderEventPublisher in scenario-s1-core/src/main/java/com/example/s1/messaging/OrderEventPublisher.java
- [ ] T045 [P] [US2] 實作 OrderEventConsumer in scenario-s1-core/src/main/java/com/example/s1/messaging/OrderEventConsumer.java
- [ ] T046 [US2] 實作 OrderController in scenario-s1-core/src/main/java/com/example/s1/web/OrderController.java
- [ ] T047 [P] [US1] 建立 RabbitMqConfig in scenario-s1-core/src/main/java/com/example/s1/config/RabbitMqConfig.java
- [ ] T048 [P] [US1] 建立 application.yml in scenario-s1-core/src/main/resources/application.yml
- [ ] T049 [P] [US1] 建立 Flyway migration in scenario-s1-core/src/main/resources/db/migration/V1__create_orders_table.sql
- [ ] T050 [US1] 建立 S1TestApplication in scenario-s1-core/src/test/java/com/example/s1/S1TestApplication.java
- [ ] T051 [P] [US1] 建立 scenario-s1-core/README.md

**Checkpoint**: S1 場景完成，可執行 `./gradlew :scenario-s1-core:test` 驗證

---

## Phase 4: User Story 3 - S2 多儲存層場景 (Priority: P1)

**Goal**: 驗證 PostgreSQL + Redis + Elasticsearch 三層儲存資料一致性

**Independent Test**: `./gradlew :scenario-s2-multistore:test`

**Maps to**: US3 (多儲存層資料一致性驗證)

### Tests (TDD - 先寫測試) ⚠️

- [ ] T052 [P] [US3] 撰寫 RedisCacheIT in scenario-s2-multistore/src/test/java/com/example/s2/RedisCacheIT.java
- [ ] T053 [P] [US3] 撰寫 ElasticsearchSyncIT in scenario-s2-multistore/src/test/java/com/example/s2/ElasticsearchSyncIT.java
- [ ] T054 [P] [US3] 撰寫 MultiStoreConsistencyIT in scenario-s2-multistore/src/test/java/com/example/s2/MultiStoreConsistencyIT.java

### Implementation

- [ ] T055 [P] [US3] 建立 S2Application in scenario-s2-multistore/src/main/java/com/example/s2/S2Application.java
- [ ] T056 [P] [US3] 建立 Customer entity in scenario-s2-multistore/src/main/java/com/example/s2/domain/Customer.java
- [ ] T057 [US3] 建立 CustomerRepository in scenario-s2-multistore/src/main/java/com/example/s2/repository/CustomerRepository.java
- [ ] T058 [P] [US3] 實作 CacheService in scenario-s2-multistore/src/main/java/com/example/s2/service/CacheService.java
- [ ] T059 [P] [US3] 實作 SearchService in scenario-s2-multistore/src/main/java/com/example/s2/service/SearchService.java
- [ ] T060 [US3] 實作 CustomerService in scenario-s2-multistore/src/main/java/com/example/s2/service/CustomerService.java
- [ ] T061 [P] [US3] 建立 RedisConfig in scenario-s2-multistore/src/main/java/com/example/s2/config/RedisConfig.java
- [ ] T062 [P] [US3] 建立 ElasticsearchConfig in scenario-s2-multistore/src/main/java/com/example/s2/config/ElasticsearchConfig.java
- [ ] T063 [P] [US3] 建立 application.yml in scenario-s2-multistore/src/main/resources/application.yml
- [ ] T064 [P] [US3] 建立 Flyway migration in scenario-s2-multistore/src/main/resources/db/migration/V1__create_customers_table.sql
- [ ] T065 [US3] 建立 S2TestApplication in scenario-s2-multistore/src/test/java/com/example/s2/S2TestApplication.java
- [ ] T066 [P] [US3] 建立 scenario-s2-multistore/README.md

**Checkpoint**: S2 場景完成，可執行 `./gradlew :scenario-s2-multistore:test` 驗證

---

## Phase 5: User Story 4+5 - CI Pipeline (Priority: P2)

**Goal**: 建立 GitHub Actions CI 與 Schema Migration 驗證

**Independent Test**: 推送程式碼至 GitHub 觸發 CI

**Maps to**: US4 (Schema 遷移測試) + US5 (CI 模組化平行建置)

### Implementation

- [ ] T067 [P] [US5] 建立 CI workflow in .github/workflows/ci.yml
- [ ] T068 [P] [US4] 建立 Schema Migration 測試 in scenario-s1-core/src/test/java/com/example/s1/SchemaMigrationIT.java
- [ ] T069 [P] [US4] 建立 Schema Migration 測試 in scenario-s2-multistore/src/test/java/com/example/s2/SchemaMigrationIT.java

**Checkpoint**: CI pipeline 就緒，推送程式碼後自動觸發模組測試

---

## Phase 6: User Story 6 - S3 Kafka 事件串流場景 (Priority: P2)

**Goal**: 驗證 Kafka + Schema Registry 事件串流與 Schema Evolution

**Independent Test**: `./gradlew :scenario-s3-kafka:test`

**Maps to**: US6 (事件串流與 Schema 演進測試)

### Tests (TDD - 先寫測試) ⚠️

- [ ] T070 [P] [US6] 撰寫 KafkaProducerConsumerIT in scenario-s3-kafka/src/test/java/com/example/s3/KafkaProducerConsumerIT.java
- [ ] T071 [P] [US6] 撰寫 SchemaEvolutionIT in scenario-s3-kafka/src/test/java/com/example/s3/SchemaEvolutionIT.java

### Implementation

- [ ] T072 [P] [US6] 建立 S3Application in scenario-s3-kafka/src/main/java/com/example/s3/S3Application.java
- [ ] T073 [P] [US6] 建立 Avro schema v1 in scenario-s3-kafka/src/main/resources/avro/order-event-v1.avsc
- [ ] T074 [P] [US6] 建立 Avro schema v2 in scenario-s3-kafka/src/main/resources/avro/order-event-v2.avsc
- [ ] T075 [US6] 實作 OrderEventProducer in scenario-s3-kafka/src/main/java/com/example/s3/producer/OrderEventProducer.java
- [ ] T076 [US6] 實作 OrderEventConsumer in scenario-s3-kafka/src/main/java/com/example/s3/consumer/OrderEventConsumer.java
- [ ] T077 [P] [US6] 建立 KafkaConfig in scenario-s3-kafka/src/main/java/com/example/s3/config/KafkaConfig.java
- [ ] T078 [P] [US6] 建立 application.yml in scenario-s3-kafka/src/main/resources/application.yml
- [ ] T079 [US6] 建立 S3TestApplication in scenario-s3-kafka/src/test/java/com/example/s3/S3TestApplication.java
- [ ] T080 [P] [US6] 建立 scenario-s3-kafka/README.md

**Checkpoint**: S3 場景完成，可執行 `./gradlew :scenario-s3-kafka:test` 驗證

---

## Phase 7: User Story 7 - S4 CDC 場景 (Priority: P2)

**Goal**: 驗證 Debezium CDC 資料變更捕獲

**Independent Test**: `./gradlew :scenario-s4-cdc:test`

**Maps to**: US7 (資料變更捕獲測試)

### Tests (TDD - 先寫測試) ⚠️

- [ ] T081 [P] [US7] 撰寫 DebeziumCdcIT in scenario-s4-cdc/src/test/java/com/example/s4/DebeziumCdcIT.java
- [ ] T082 [P] [US7] 撰寫 CdcSchemaChangeIT in scenario-s4-cdc/src/test/java/com/example/s4/CdcSchemaChangeIT.java

### Implementation

- [ ] T083 [P] [US7] 建立 S4Application in scenario-s4-cdc/src/main/java/com/example/s4/S4Application.java
- [ ] T084 [P] [US7] 建立 TransactionType enum in scenario-s4-cdc/src/main/java/com/example/s4/domain/TransactionType.java
- [ ] T085 [P] [US7] 建立 Transaction entity in scenario-s4-cdc/src/main/java/com/example/s4/domain/Transaction.java
- [ ] T086 [US7] 建立 TransactionRepository in scenario-s4-cdc/src/main/java/com/example/s4/repository/TransactionRepository.java
- [ ] T087 [US7] 實作 CdcEventProcessor in scenario-s4-cdc/src/main/java/com/example/s4/cdc/CdcEventProcessor.java
- [ ] T088 [P] [US7] 建立 application.yml in scenario-s4-cdc/src/main/resources/application.yml
- [ ] T089 [P] [US7] 建立 Flyway migration in scenario-s4-cdc/src/main/resources/db/migration/V1__create_transactions_table.sql
- [ ] T090 [US7] 建立 S4TestApplication in scenario-s4-cdc/src/test/java/com/example/s4/S4TestApplication.java
- [ ] T091 [P] [US7] 建立 scenario-s4-cdc/README.md

**Checkpoint**: S4 場景完成，可執行 `./gradlew :scenario-s4-cdc:test` 驗證

---

## Phase 8: User Story 8 - S5 韌性測試場景 (Priority: P2)

**Goal**: 驗證 WireMock + Toxiproxy 外部系統模擬與故障注入

**Independent Test**: `./gradlew :scenario-s5-resilience:test`

**Maps to**: US8 (外部系統故障韌性測試)

### Tests (TDD - 先寫測試) ⚠️

- [ ] T092 [P] [US8] 撰寫 WireMockApiIT in scenario-s5-resilience/src/test/java/com/example/s5/WireMockApiIT.java
- [ ] T093 [P] [US8] 撰寫 ToxiproxyFaultIT in scenario-s5-resilience/src/test/java/com/example/s5/ToxiproxyFaultIT.java
- [ ] T094 [P] [US8] 撰寫 CircuitBreakerIT in scenario-s5-resilience/src/test/java/com/example/s5/CircuitBreakerIT.java

### Implementation

- [ ] T095 [P] [US8] 建立 S5Application in scenario-s5-resilience/src/main/java/com/example/s5/S5Application.java
- [ ] T096 [US8] 實作 ExternalApiClient in scenario-s5-resilience/src/main/java/com/example/s5/client/ExternalApiClient.java
- [ ] T097 [US8] 實作 CreditCheckService in scenario-s5-resilience/src/main/java/com/example/s5/service/CreditCheckService.java
- [ ] T098 [P] [US8] 建立 ResilienceConfig in scenario-s5-resilience/src/main/java/com/example/s5/config/ResilienceConfig.java
- [ ] T099 [P] [US8] 建立 application.yml in scenario-s5-resilience/src/main/resources/application.yml
- [ ] T100 [US8] 建立 S5TestApplication in scenario-s5-resilience/src/test/java/com/example/s5/S5TestApplication.java
- [ ] T101 [P] [US8] 建立 scenario-s5-resilience/README.md

**Checkpoint**: S5 場景完成，可執行 `./gradlew :scenario-s5-resilience:test` 驗證

---

## Phase 9: User Story 9+10 - S6 安全場景 (Priority: P3)

**Goal**: 驗證 Keycloak OAuth2 + Vault 動態憑證

**Independent Test**: `./gradlew :scenario-s6-security:test`

**Maps to**: US9 (身份驗證與授權) + US10 (動態憑證管理)

### Tests (TDD - 先寫測試) ⚠️

- [ ] T102 [P] [US9] 撰寫 KeycloakAuthIT in scenario-s6-security/src/test/java/com/example/s6/KeycloakAuthIT.java
- [ ] T103 [P] [US10] 撰寫 VaultCredentialIT in scenario-s6-security/src/test/java/com/example/s6/VaultCredentialIT.java

### Implementation

- [ ] T104 [P] [US9] 建立 S6Application in scenario-s6-security/src/main/java/com/example/s6/S6Application.java
- [ ] T105 [P] [US9] 實作 SecuredOrderController in scenario-s6-security/src/main/java/com/example/s6/web/SecuredOrderController.java
- [ ] T106 [P] [US9] 實作 AdminController in scenario-s6-security/src/main/java/com/example/s6/web/AdminController.java
- [ ] T107 [US9] 建立 SecurityConfig in scenario-s6-security/src/main/java/com/example/s6/config/SecurityConfig.java
- [ ] T108 [P] [US9] 建立 Keycloak realm export in scenario-s6-security/src/main/resources/keycloak/realm-export.json
- [ ] T109 [P] [US9] 建立 application.yml in scenario-s6-security/src/main/resources/application.yml
- [ ] T110 [US9] 建立 S6TestApplication in scenario-s6-security/src/test/java/com/example/s6/S6TestApplication.java
- [ ] T111 [P] [US9] 建立 scenario-s6-security/README.md

**Checkpoint**: S6 場景完成，可執行 `./gradlew :scenario-s6-security:test` 驗證

---

## Phase 10: User Story 11 - S7 雲端模擬場景 (Priority: P3)

**Goal**: 驗證 LocalStack + Azurite 雲端服務模擬

**Independent Test**: `./gradlew :scenario-s7-cloud:test`

**Maps to**: US11 (雲端服務離線測試)

### Tests (TDD - 先寫測試) ⚠️

- [ ] T112 [P] [US11] 撰寫 LocalStackS3IT in scenario-s7-cloud/src/test/java/com/example/s7/LocalStackS3IT.java
- [ ] T113 [P] [US11] 撰寫 LocalStackSqsIT in scenario-s7-cloud/src/test/java/com/example/s7/LocalStackSqsIT.java
- [ ] T114 [P] [US11] 撰寫 LocalStackDynamoDbIT in scenario-s7-cloud/src/test/java/com/example/s7/LocalStackDynamoDbIT.java
- [ ] T115 [P] [US11] 撰寫 AzuriteBlobIT in scenario-s7-cloud/src/test/java/com/example/s7/AzuriteBlobIT.java

### Implementation

- [ ] T116 [P] [US11] 建立 S7Application in scenario-s7-cloud/src/main/java/com/example/s7/S7Application.java
- [ ] T117 [P] [US11] 實作 S3FileService in scenario-s7-cloud/src/main/java/com/example/s7/aws/S3FileService.java
- [ ] T118 [P] [US11] 實作 SqsMessageService in scenario-s7-cloud/src/main/java/com/example/s7/aws/SqsMessageService.java
- [ ] T119 [P] [US11] 實作 DynamoDbService in scenario-s7-cloud/src/main/java/com/example/s7/aws/DynamoDbService.java
- [ ] T120 [P] [US11] 實作 BlobStorageService in scenario-s7-cloud/src/main/java/com/example/s7/azure/BlobStorageService.java
- [ ] T121 [P] [US11] 建立 AwsConfig in scenario-s7-cloud/src/main/java/com/example/s7/config/AwsConfig.java
- [ ] T122 [P] [US11] 建立 AzureConfig in scenario-s7-cloud/src/main/java/com/example/s7/config/AzureConfig.java
- [ ] T123 [P] [US11] 建立 application.yml in scenario-s7-cloud/src/main/resources/application.yml
- [ ] T124 [US11] 建立 S7TestApplication in scenario-s7-cloud/src/test/java/com/example/s7/S7TestApplication.java
- [ ] T125 [P] [US11] 建立 scenario-s7-cloud/README.md

**Checkpoint**: S7 場景完成，可執行 `./gradlew :scenario-s7-cloud:test` 驗證

---

## Phase 11: User Story 12 - S8 契約測試場景 (Priority: P3)

**Goal**: 驗證 Pact Broker 消費者驅動契約測試

**Independent Test**: `./gradlew :scenario-s8-contract:test`

**Maps to**: US12 (微服務契約測試)

### Tests (TDD - 先寫測試) ⚠️

- [ ] T126 [P] [US12] 撰寫 OrderConsumerPactIT in scenario-s8-contract/src/test/java/com/example/s8/OrderConsumerPactIT.java
- [ ] T127 [P] [US12] 撰寫 OrderProviderPactIT in scenario-s8-contract/src/test/java/com/example/s8/OrderProviderPactIT.java

### Implementation

- [ ] T128 [P] [US12] 建立 S8Application in scenario-s8-contract/src/main/java/com/example/s8/S8Application.java
- [ ] T129 [US12] 實作 OrderService in scenario-s8-contract/src/main/java/com/example/s8/service/OrderService.java
- [ ] T130 [US12] 實作 OrderController in scenario-s8-contract/src/main/java/com/example/s8/web/OrderController.java
- [ ] T131 [P] [US12] 建立 application.yml in scenario-s8-contract/src/main/resources/application.yml
- [ ] T132 [US12] 建立 S8TestApplication in scenario-s8-contract/src/test/java/com/example/s8/S8TestApplication.java
- [ ] T133 [P] [US12] 建立 scenario-s8-contract/README.md

**Checkpoint**: S8 場景完成，可執行 `./gradlew :scenario-s8-contract:test` 驗證

---

## Phase 12: Polish & Cross-Cutting Concerns

**Purpose**: 跨模組優化與文件完善

- [ ] T134 [P] 更新 root README.md 加入所有場景說明
- [ ] T135 [P] 執行全模組測試驗證 `./gradlew test`
- [ ] T136 驗證 quickstart.md 步驟可執行
- [ ] T137 [P] 產生 JaCoCo 覆蓋率報告 `./gradlew jacocoAggregatedReport`
- [ ] T138 驗證所有模組可獨立建置與測試
- [ ] T139 [P] 清理程式碼與移除未使用的相依

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1 (Setup) ─────────────────────────────────────────────┐
     │                                                        │
     ▼                                                        │
Phase 2 (Foundational/tc-common) ◄─── BLOCKS ALL BELOW ──────┤
     │                                                        │
     ├─────────┬─────────┬─────────┬─────────┬─────────┬─────┼─────────┐
     ▼         ▼         ▼         ▼         ▼         ▼     ▼         ▼
Phase 3    Phase 4   Phase 6   Phase 7   Phase 8   Phase 9  Phase 10  Phase 11
 (S1)       (S2)      (S3)      (S4)      (S5)      (S6)     (S7)      (S8)
  │          │         │         │         │         │        │         │
  └──────────┴─────────┴─────────┴─────────┴─────────┴────────┴─────────┘
                                    │
                                    ▼
                              Phase 5 (CI)
                                    │
                                    ▼
                              Phase 12 (Polish)
```

### User Story Dependencies

- **US1+US2 (S1)**: 依賴 Phase 2 完成 → 無其他故事相依
- **US3 (S2)**: 依賴 Phase 2 完成 → 無其他故事相依
- **US4+US5 (CI)**: 依賴 S1, S2 完成
- **US6 (S3)**: 依賴 Phase 2 完成 → 無其他故事相依
- **US7 (S4)**: 依賴 Phase 2 完成 → 無其他故事相依
- **US8 (S5)**: 依賴 Phase 2 完成 → 無其他故事相依
- **US9+US10 (S6)**: 依賴 Phase 2 完成 → 無其他故事相依
- **US11 (S7)**: 依賴 Phase 2 完成 → 無其他故事相依
- **US12 (S8)**: 依賴 Phase 2 完成 → 無其他故事相依

### Within Each Scenario Module

1. Tests MUST be written and FAIL before implementation (TDD)
2. Domain models before services
3. Services before controllers/endpoints
4. Configuration before test application
5. README last

### Parallel Opportunities

- **Phase 1**: T005-T013 可平行（所有模組骨架）
- **Phase 2**: T014-T035 大部分可平行（Container Factories）
- **Phase 3-11**: 各場景可平行開發（不同模組、無相依）
- **Phase 12**: T134, T137, T139 可平行

---

## Parallel Example: Phase 2 (tc-common)

```bash
# 平行執行所有 Container Factory 實作:
Task: "實作 PostgresContainerFactory in tc-common/.../PostgresContainerFactory.java"
Task: "實作 RabbitMqContainerFactory in tc-common/.../RabbitMqContainerFactory.java"
Task: "實作 RedisContainerFactory in tc-common/.../RedisContainerFactory.java"
Task: "實作 ElasticsearchContainerFactory in tc-common/.../ElasticsearchContainerFactory.java"
Task: "實作 KafkaContainerFactory in tc-common/.../KafkaContainerFactory.java"
# ... (all can run in parallel)
```

---

## Implementation Strategy

### MVP First (Phase 1-3 Only)

1. Complete Phase 1: Monorepo Setup
2. Complete Phase 2: tc-common (CRITICAL - blocks all scenarios)
3. Complete Phase 3: S1 (DB + MQ + API)
4. **STOP and VALIDATE**: `./gradlew :scenario-s1-core:test`
5. Demo MVP: 基礎整合測試能力驗證

### Incremental Delivery (PRD Phase 1)

1. Setup + tc-common → Foundation ready
2. Add S1 → Test independently → Demo (MVP!)
3. Add S2 → Test independently → Demo
4. Add CI → Verify parallel builds
5. PRD Phase 1 完成

### Parallel Team Strategy

With 3+ developers:

1. Team completes Setup + tc-common together
2. Once tc-common is done:
   - Developer A: S1 (Core)
   - Developer B: S2 (Multi-store)
   - Developer C: S3 (Kafka) + S4 (CDC)
3. Scenarios complete and integrate independently

---

## Notes

- `[P]` tasks = 不同檔案、無相依，可平行執行
- `[Story]` label = 對應 spec.md 中的使用者故事
- 每個場景模組應可獨立完成與測試
- TDD: 測試必須先寫且失敗，才能開始實作
- 每個任務完成後應 commit
- 在任何 Checkpoint 可暫停驗證場景獨立性
- 避免：模糊任務、同檔案衝突、破壞獨立性的跨故事相依
