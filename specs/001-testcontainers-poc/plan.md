# Implementation Plan: Testcontainers Integration Testing PoC

**Branch**: `001-testcontainers-poc` | **Date**: 2026-02-01 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-testcontainers-poc/spec.md`
**Tech Reference**: [TECH.md](../../TECH.md)

## Summary

建立企業級金融系統的 Testcontainers 整合測試 PoC，採用 **Gradle Monorepo Multi-Module** 架構，涵蓋 8 大測試場景。每個場景為獨立的 Gradle sub-module，共享 `tc-common` 通用模組。專案遵循六角形架構與 TDD/BDD 原則，確保測試程式碼品質與可維護性。

## Technical Context

**Language/Version**: Java 21 (LTS)
**Build Tool**: Gradle 8.x with Kotlin DSL
**Primary Dependencies**:
- Spring Boot 3.4.x (Web, Data JPA, AMQP, Data Redis, Data Elasticsearch, OAuth2, Kafka)
- Testcontainers 1.20.x
- Resilience4j 2.2.x
- AWS SDK 2.25.x / Azure Storage Blob 12.25.x
- Pact 4.6.x

**Storage**: PostgreSQL 16, Redis 7, Elasticsearch 8.x, Kafka (KRaft), DynamoDB (LocalStack)
**Testing**: JUnit 5, Testcontainers, REST Assured, Awaitility, Pact
**Target Platform**: Linux server (CI), macOS/Windows (local development)
**Project Type**: Gradle Monorepo Multi-Module
**Performance Goals**:
- 單一模組測試套件 ≤ 90 秒
- 全模組測試套件 ≤ 8 分鐘
- 容器啟動成功率 ≥ 99%

**Constraints**:
- 本機最低 8GB RAM
- Docker Engine ≥ 20.10
- 模組間零耦合

**Scale/Scope**: 8 個場景模組 + 1 個共用模組，12 個使用者故事

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. TDD (NON-NEGOTIABLE) | ✅ PASS | 測試先行，Red-Green-Refactor 流程將在 tasks.md 中強制執行 |
| II. BDD | ✅ PASS | spec.md 已採用 Given-When-Then 格式定義驗收情境 |
| III. DDD | ✅ PASS | 各場景模組有獨立的 domain 層（Order, Customer, Transaction 等） |
| IV. SOLID | ✅ PASS | Container Factory 採用單一職責，依賴注入模式 |
| V. Hexagonal Architecture | ✅ PASS | 各模組採用 domain/service/repository/web 分層 |
| VI. Layered Architecture | ⚠️ JUSTIFIED | PoC 性質允許簡化層級，但核心業務邏輯仍保持在 domain 層 |
| VII. Data Mapping | ✅ PASS | DTO 與 Domain Entity 分離，使用 Mapper 轉換 |

**Justification for VI**: 本專案為測試基礎設施 PoC，主要目的是驗證 Testcontainers 整合能力，而非建立完整的生產應用。各場景模組的業務邏輯較為簡單（訂單建立、客戶查詢等），採用簡化的三層架構（domain/service/web）足以展示整合測試模式，同時保持程式碼清晰易懂。

## Project Structure

### Documentation (this feature)

```text
specs/001-testcontainers-poc/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI specs)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (Gradle Monorepo)

```text
testcontainers-poc/
├── settings.gradle.kts                     # 模組註冊
├── build.gradle.kts                        # Root: 共用 plugin & config
├── gradle/
│   └── libs.versions.toml                  # Version Catalog (集中版本管理)
├── gradle.properties                       # Gradle 全域設定
│
├── tc-common/                              # ═══ 共用測試基礎設施 ═══
│   ├── build.gradle.kts
│   └── src/main/java/com/example/tc/
│       ├── containers/                     # 可重用的容器定義 (Factory Pattern)
│       │   ├── PostgresContainerFactory.java
│       │   ├── RabbitMqContainerFactory.java
│       │   ├── RedisContainerFactory.java
│       │   ├── ElasticsearchContainerFactory.java
│       │   ├── KafkaContainerFactory.java
│       │   ├── SchemaRegistryContainerFactory.java
│       │   ├── DebeziumContainerFactory.java
│       │   ├── WireMockContainerFactory.java
│       │   ├── ToxiproxyContainerFactory.java
│       │   ├── KeycloakContainerFactory.java
│       │   ├── VaultContainerFactory.java
│       │   ├── LocalStackContainerFactory.java
│       │   ├── AzuriteContainerFactory.java
│       │   └── PactBrokerContainerFactory.java
│       ├── base/                           # 測試 Base Classes
│       │   └── IntegrationTestBase.java
│       ├── dto/                            # 共用 DTO
│       │   ├── CreateOrderRequest.java
│       │   └── OrderResponse.java
│       └── util/                           # 共用工具
│           ├── AwaitHelper.java
│           └── TokenHelper.java
│
├── scenario-s1-core/                       # ═══ S1: DB + MQ + API ═══
│   ├── build.gradle.kts
│   ├── README.md
│   └── src/
│       ├── main/java/com/example/s1/
│       │   ├── S1Application.java
│       │   ├── domain/
│       │   │   ├── Order.java              # Entity
│       │   │   └── OrderStatus.java        # Enum
│       │   ├── repository/
│       │   │   └── OrderRepository.java    # Port (interface)
│       │   ├── service/
│       │   │   └── OrderService.java       # Application Service
│       │   ├── messaging/
│       │   │   ├── OrderEventPublisher.java
│       │   │   └── OrderEventConsumer.java
│       │   ├── web/
│       │   │   └── OrderController.java    # Adapter (REST)
│       │   └── config/
│       │       └── RabbitMqConfig.java
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/
│       │       └── V1__create_orders_table.sql
│       └── test/java/com/example/s1/
│           ├── S1TestApplication.java
│           ├── OrderRepositoryIT.java
│           ├── OrderMessagingIT.java
│           └── OrderApiIT.java
│
├── scenario-s2-multistore/                 # ═══ S2: PostgreSQL + Redis + ES ═══
│   └── [structure similar to s1]
│
├── scenario-s3-kafka/                      # ═══ S3: Kafka + Schema Registry ═══
│   └── [structure similar to s1 + Avro schemas]
│
├── scenario-s4-cdc/                        # ═══ S4: Debezium CDC ═══
│   └── [structure similar to s1 + CDC processor]
│
├── scenario-s5-resilience/                 # ═══ S5: WireMock + Toxiproxy ═══
│   └── [structure similar to s1 + resilience config]
│
├── scenario-s6-security/                   # ═══ S6: Keycloak + Vault ═══
│   └── [structure similar to s1 + security config]
│
├── scenario-s7-cloud/                      # ═══ S7: LocalStack + Azurite ═══
│   └── [structure similar to s1 + AWS/Azure services]
│
├── scenario-s8-contract/                   # ═══ S8: Pact Broker ═══
│   └── [structure similar to s1 + Pact tests]
│
└── .github/
    └── workflows/
        └── ci.yml                          # Matrix-based CI pipeline
```

**Structure Decision**: 採用 Gradle Monorepo Multi-Module 架構，每個場景為獨立子模組。此架構符合 PRD 與 TECH.md 的設計，實現：
- 依賴隔離：每個場景模組僅宣告自己所需的容器依賴
- 獨立建置：可單獨執行 `./gradlew :scenario-s1-core:test`
- 平行 CI：GitHub Actions 可同時觸發多個模組的 pipeline
- 共用基礎設施：`tc-common` 模組集中管理容器定義與測試基底類別

## Module Dependencies

```text
tc-common (java-library)
    ↑
    │ testImplementation
    │
┌───┴───┬───────┬───────┬───────┬───────┬───────┬───────┐
│       │       │       │       │       │       │       │
s1    s2      s3      s4      s5      s6      s7      s8
core  multi   kafka   cdc     resil   sec     cloud   pact
      store                   ience   urity
```

**Key Rule**: 場景模組之間無相互依賴，僅依賴 `tc-common`。

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 簡化層級 (VI) | PoC 展示性質，快速驗證整合測試模式 | 完整六角形架構會增加程式碼量，降低 PoC 可讀性 |
| 多模組架構 | 各場景需獨立啟動容器，避免資源競爭 | 單一模組無法實現場景隔離與平行測試 |

## Phase Summary

| Phase | Deliverable | Description |
|-------|-------------|-------------|
| Phase 0 | research.md | 技術研究：Testcontainers 最佳實踐、Container Factory 模式、CI 配置 |
| Phase 1 | data-model.md, contracts/, quickstart.md | 資料模型設計、API 契約、快速入門指南 |
| Phase 2 | tasks.md | 實作任務清單（由 /speckit.tasks 產生） |
