# PRD: Testcontainers Integration Testing PoC

## Status

| Field          | Value                                      |
|----------------|--------------------------------------------|
| Status         | Draft v3                                   |
| Author(s)      | Rex (Application Architect)                |
| Created        | 2026-02-01                                 |
| Last Updated   | 2026-02-01                                 |
| Target Release | Sprint 2026-Q1 ~ Q2                       |
| Stakeholders   | Development Team, QA Team, DevOps, Architect Office, Security Team |

---

## Problem Statement

在企業級金融系統中，整合測試長期面臨以下痛點：

1. **環境依賴性高**：開發人員需要連接共享的測試資料庫與 Message Queue，導致測試不穩定且互相干擾。
2. **環境準備成本高**：搭建完整的測試環境（Database + MQ + 外部 API Mock）耗時且容易出錯。
3. **CI/CD 瓶頸**：共享測試環境成為 pipeline 的瓶頸，多團隊並行開發時測試排隊等候。
4. **測試資料汙染**：不同測試案例之間的資料殘留，造成偶發性測試失敗（flaky tests）。
5. **本地開發體驗差**：開發人員在本機難以重現完整的整合測試場景。
6. **外部系統不可控**：第三方 API（聯徵、SWIFT、支付閘道）在測試環境中不穩定或不可用。
7. **安全測試缺口**：OAuth2/OIDC 與密鑰管理的整合測試難以在隔離環境中執行。
8. **雲端服務綁定**：依賴 AWS/Azure 的測試需要真實雲端帳號，成本高且難以隔離。
9. **跨服務契約脆弱**：微服務之間的 API 契約變更常導致隱性破壞，缺乏自動化驗證機制。
10. **韌性驗證不足**：Circuit Breaker、Retry、Timeout 等韌性機制缺乏在真實故障情境下的驗證。

這些問題直接影響了開發效率、交付速度與軟體品質信心。

---

## Proposed Solution

導入 **Testcontainers** 框架，以 **Gradle Monorepo Multi-Module** 架構建立涵蓋 **8 大場景** 的標準化整合測試方案。每個場景為獨立的 Gradle sub module，共享一個 `tc-common` 通用模組，三個階段漸進推進。

### Monorepo 架構優勢

- **依賴隔離**：每個場景模組僅宣告自己所需的容器依賴，避免 classpath 膨脹
- **獨立建置**：可單獨執行 `./gradlew :scenario-s1-core:test`，無需啟動不相關容器
- **平行 CI**：GitHub Actions 可同時觸發多個模組的 pipeline，縮短總執行時間
- **漸進導入**：團隊可逐步採用場景模組，不需一次引入全部依賴
- **共用基礎設施**：`tc-common` 模組集中管理容器定義、測試基底類別與共用工具

### PoC 場景與模組對照

| #  | 場景名稱                  | Gradle Module             | 階段    | 容器技術                          | 業務價值                             |
|----|--------------------------|---------------------------|---------|----------------------------------|-------------------------------------|
| S1 | 基礎整合：DB + MQ + API   | `scenario-s1-core`        | Phase 1 | PostgreSQL, RabbitMQ             | 驗證核心流程端對端整合                 |
| S2 | 多資料庫整合               | `scenario-s2-multistore`  | Phase 1 | PostgreSQL, Redis, Elasticsearch | 驗證多儲存層資料一致性                 |
| S3 | 事件驅動架構 (EDA)         | `scenario-s3-kafka`       | Phase 2 | Kafka, Schema Registry           | 驗證事件串流與 Schema Evolution        |
| S4 | Change Data Capture (CDC) | `scenario-s4-cdc`         | Phase 2 | Kafka, Debezium, PostgreSQL      | 驗證資料庫變更事件捕獲                 |
| S5 | 外部系統模擬與故障注入     | `scenario-s5-resilience`  | Phase 2 | WireMock, Toxiproxy              | 驗證第三方整合韌性                     |
| S6 | 安全與身份驗證             | `scenario-s6-security`    | Phase 3 | Keycloak, Vault                  | 驗證 OAuth2/OIDC 與密鑰管理           |
| S7 | 雲端服務模擬               | `scenario-s7-cloud`       | Phase 3 | LocalStack, Azurite              | 脫離雲端帳號執行 AWS/Azure 整合測試    |
| S8 | 消費者驅動契約測試         | `scenario-s8-contract`    | Phase 3 | Pact Broker                      | 自動化微服務 API 契約驗證              |
| —  | 共用測試基礎設施           | `tc-common`               | Phase 1 | —                                | 容器定義、Base Class、共用工具         |

---

## Scenario Details

### S1: 基礎整合 — DB + MQ + API

**業務場景：訂單處理流程**

```
[Client] --POST /api/orders--> [Order Service]
                                     |
                               [Save to DB]
                                     |
                            [Publish Event to MQ]
                                     |
                         [Consumer processes event]
                                     |
                           [Update order status]
```

1. 客戶透過 REST API 建立訂單
2. 系統將訂單儲存至 PostgreSQL
3. 系統發佈 `OrderCreated` 事件至 RabbitMQ
4. 消費者接收事件，更新訂單狀態為 `CONFIRMED`
5. 客戶可透過 API 查詢訂單狀態

---

### S2: 多資料庫整合 — PostgreSQL + Redis + Elasticsearch

**業務場景：客戶資料查詢加速**

```
[API Request] --> [Service Layer]
                       |
           ┌───────────┼───────────┐
           ▼           ▼           ▼
     [PostgreSQL]   [Redis]   [Elasticsearch]
     主資料儲存    熱資料快取    全文檢索索引
```

驗證三層儲存之間的資料一致性：寫入 PostgreSQL 後 Redis 快取更新、Elasticsearch 索引同步、快取失效後 fallback 回 PostgreSQL。

---

### S3: 事件驅動架構 — Kafka + Schema Registry

**業務場景：跨服務事件串流**

```
[Order Service]                          [Payment Service]
      |                                        |
      ▼                                        ▼
 [Produce Event] --> [Kafka Topic] --> [Consume Event]
                         |
                  [Schema Registry]
                  (Avro Schema 管理)
```

驗證事件的可靠傳遞與 Avro schema evolution 的向前/向後相容、多 partition 順序保證、exactly-once 語義。

---

### S4: Change Data Capture — Debezium + Kafka

**業務場景：即時資料同步管道**

```
[PostgreSQL] --WAL--> [Debezium Connector]
                              |
                              ▼
                      [Kafka Topic]  (CDC Events)
                              |
                   ┌──────────┼──────────┐
                   ▼          ▼          ▼
             [ES Indexer] [Analytics] [Audit Log]
```

驗證 CDC pipeline 完整性：INSERT/UPDATE/DELETE 捕獲、before/after 快照、schema 變更後 CDC 適應、Connector 故障恢復。

---

### S5: 外部系統模擬與故障注入 — WireMock + Toxiproxy

**業務場景：第三方 API 韌性驗證**

```
[Order Service] --> [Toxiproxy] --> [WireMock]
                    (故障注入)       (API Mock)
                    - Latency       - 聯徵中心 API
                    - Timeout       - SWIFT API
                    - Connection    - 支付閘道 API
                      Reset
```

驗證 Circuit Breaker、Retry、Fallback 在真實網路故障下的行為。

---

### S6: 安全與身份驗證 — Keycloak + Vault

**業務場景：OAuth2/OIDC 與密鑰管理**

```
[Client] --(1) Login--> [Keycloak]
         <-(2) JWT-----
         --(3) API+Token--> [Service] --> [Vault]
                                          (Dynamic Credentials)
```

驗證 JWT 簽發與驗證、RBAC 角色控管、Token Refresh、Vault 動態憑證與密鑰輪替。

---

### S7: 雲端服務模擬 — LocalStack + Azurite

**業務場景：雲端服務整合（脫離雲端帳號）**

```
[Application]
      ├──▶ [LocalStack]  S3 / SQS / DynamoDB / Lambda
      └──▶ [Azurite]     Blob / Queue / Table Storage
```

驗證 S3/Blob 文件操作、SQS/Queue 訊息處理含 DLQ、DynamoDB/Table CRUD。

---

### S8: 消費者驅動契約測試 — Pact Broker

**業務場景：微服務 API 契約自動化驗證**

```
[Consumer] --(1) Generate Pact--> [Pact Broker] <--(2) Fetch Pact-- [Provider]
                                       |
                                 (3) Verify & Publish Result
                                       |
                              (4) Can-I-Deploy Check
```

驗證 Consumer 契約生成、Provider 自動驗證、破壞性變更偵測、`can-i-deploy` 部署閘門。

---

## Goals & Success Metrics

### Goals

| #   | Goal                                                          | Phase   | Priority    |
|-----|---------------------------------------------------------------|---------|-------------|
| G1  | 驗證 Testcontainers 可穩定管理 PostgreSQL + RabbitMQ 容器       | Phase 1 | Must Have   |
| G2  | 驗證整合測試可在無外部環境依賴下完成全流程                        | Phase 1 | Must Have   |
| G3  | 建立 Monorepo multi-module 可複用的測試基礎設施                  | Phase 1 | Must Have   |
| G4  | 驗證 CI/CD pipeline 中 per-module 獨立建置的可行性               | Phase 1 | Should Have |
| G5  | 驗證 Redis + Elasticsearch 多儲存層資料一致性                    | Phase 1 | Must Have   |
| G6  | 驗證 Kafka + Schema Registry 事件串流與 Schema Evolution         | Phase 2 | Must Have   |
| G7  | 驗證 Debezium CDC pipeline 資料變更捕獲的完整性                  | Phase 2 | Must Have   |
| G8  | 驗證 WireMock + Toxiproxy 外部系統模擬與故障注入                  | Phase 2 | Must Have   |
| G9  | 驗證 Keycloak OAuth2/OIDC 完整認證授權流程                       | Phase 3 | Should Have |
| G10 | 驗證 Vault 動態憑證與密鑰輪替機制                                | Phase 3 | Should Have |
| G11 | 驗證 LocalStack/Azurite 雲端服務模擬可行性                       | Phase 3 | Should Have |
| G12 | 驗證 Pact Broker 消費者驅動契約測試流程                           | Phase 3 | Should Have |
| G13 | 產出團隊導入指南與最佳實踐文件                                    | Phase 3 | Nice to Have |

### Success Metrics

| Metric                         | Target               | Measurement Method          |
|--------------------------------|----------------------|-----------------------------|
| 測試容器啟動成功率              | ≥ 99%               | CI pipeline 統計             |
| 單模組測試套件執行時間          | ≤ 90 seconds          | `./gradlew :module:test`     |
| 全模組測試套件執行時間          | ≤ 8 minutes           | CI matrix pipeline 統計      |
| Flaky test 發生率              | 0%                   | 連續 10 次執行無隨機失敗       |
| 開發人員本機可執行率            | 100%                 | 團隊回饋                     |
| 模組間零耦合率                 | 100%                 | 每模組可獨立 build & test     |
| 程式碼覆蓋率（整合測試）        | ≥ 80% per module     | JaCoCo aggregate report      |
| 契約測試覆蓋率                 | ≥ 90% API endpoints  | Pact Broker dashboard        |
| 故障注入場景通過率              | 100%                 | Toxiproxy test report        |

---

## User Stories

### Phase 1: 核心基礎

#### US-1: 開發人員本機執行單一場景測試

**As a** 開發人員
**I want to** 執行 `./gradlew :scenario-s1-core:test` 只啟動 S1 所需的容器
**So that** 我不需要等待所有容器啟動，也不需要本機有 8GB+ 記憶體

**Acceptance Criteria:**
- 執行單一模組測試時，僅啟動該模組宣告的容器
- 不相關容器不會被拉取或啟動
- 每個模組可獨立執行 `./gradlew :module:test`，無需先 build 其他模組
- 僅需本機安裝 Docker 即可運行

#### US-2: 訂單建立端對端測試

**As a** QA 工程師
**I want to** 驗證訂單從 API 建立到資料庫寫入再到事件發佈的完整流程
**So that** 我可以確認系統各元件之間的整合正確無誤

**Acceptance Criteria:**
- POST /api/orders 回傳 201 Created
- 資料庫中可查詢到新建立的訂單
- RabbitMQ 中可接收到 `OrderCreated` 事件
- 訂單狀態在事件消費後更新為 `CONFIRMED`

#### US-3: 資料庫 Schema 自動遷移測試

**As a** 資料庫管理者
**I want to** 驗證 Flyway migration 在乾淨的資料庫上可正確執行
**So that** 我可以確信每次部署的 schema 變更不會造成問題

**Acceptance Criteria:**
- 每次測試啟動時，Flyway migration 自動在新的 PostgreSQL 容器上執行
- Migration 執行失敗時測試立即失敗並顯示明確錯誤訊息
- Schema 版本與預期一致

#### US-4: CI per-module 平行建置

**As a** DevOps 工程師
**I want to** CI pipeline 依模組變更範圍觸發對應的測試 job
**So that** 不會因為 S7 的變更導致 S1 也重新測試，節省 CI 資源

**Acceptance Criteria:**
- 每個模組有獨立的 GitHub Actions workflow，以 `paths` filter 觸發
- 變更 `scenario-s3-kafka/` 只觸發 S3 的 pipeline
- `tc-common/` 變更時觸發所有場景的 pipeline
- 可透過 `./gradlew test` 一次執行所有模組測試

#### US-5: 多儲存層資料一致性

**As a** 開發人員
**I want to** 驗證 PostgreSQL、Redis、Elasticsearch 三層儲存的資料一致性
**So that** 我可以確信客戶查詢在任何儲存層都能取得正確且最新的資料

**Acceptance Criteria:**
- 寫入 PostgreSQL 後，Redis 快取在 1 秒內更新
- PostgreSQL 資料變更後，Elasticsearch 索引在 5 秒內同步
- Redis 快取過期後，查詢自動 fallback 回 PostgreSQL
- Elasticsearch 全文搜尋結果與 PostgreSQL 資料一致

### Phase 2: 事件驅動與韌性

#### US-6: Kafka 事件串流與 Schema 相容性

**As a** 架構師
**I want to** 驗證 Kafka 事件串流在 schema 版本演進時仍能正確運作
**So that** 我可以安全地進行 Avro schema 升級，不破壞現有 consumer

**Acceptance Criteria:**
- Producer 使用 v2 schema 發送事件，v1 consumer 仍能正確消費
- Schema Registry 拒絕不相容的 schema 變更
- 同一 partition key 的事件保持順序性
- Transactional producer + consumer 實現 exactly-once 語義

#### US-7: CDC 即時資料同步

**As a** 資料工程師
**I want to** 驗證 Debezium CDC 能正確捕獲 PostgreSQL 的資料變更
**So that** 我可以建立可靠的即時資料同步管道

**Acceptance Criteria:**
- INSERT / UPDATE / DELETE 操作在 3 秒內產生對應的 Kafka CDC 事件
- CDC 事件包含完整的 before/after 資料快照
- PostgreSQL schema 變更後，CDC 事件結構自動適應
- Debezium Connector 重啟後，從上次 offset 繼續捕獲，無遺失

#### US-8: 外部 API 故障韌性

**As a** 開發人員
**I want to** 驗證系統在外部 API 故障時能正確降級
**So that** 第三方系統故障不會導致我們的系統全面崩潰

**Acceptance Criteria:**
- 外部 API 回傳 500 時，系統使用 fallback 回應
- 網路延遲 200ms 時，request timeout 正確觸發
- 連續 5 次故障後，Circuit Breaker 開啟
- Circuit Breaker 半開啟時，成功請求使其回復 Closed 狀態

### Phase 3: 安全、雲端與契約

#### US-9: OAuth2 認證與角色授權

**As a** 資安工程師
**I want to** 驗證 OAuth2/OIDC 的完整認證授權流程
**So that** 我可以確認權限控管在各種情境下正確運作

**Acceptance Criteria:**
- 使用正確帳密從 Keycloak 取得 JWT Token
- Admin 角色可存取管理端點，User 角色取得 403 Forbidden
- Refresh Token 可成功換取新 Access Token

#### US-10: 動態憑證與密鑰管理

**As a** DevOps 工程師
**I want to** 驗證應用程式能從 Vault 動態取得資料庫憑證
**So that** 我們不需要在設定檔中硬編碼資料庫密碼

**Acceptance Criteria:**
- 應用程式啟動時從 Vault 取得 PostgreSQL 臨時帳密
- 憑證 TTL 到期後，應用程式自動更新憑證

#### US-11: 雲端服務離線測試

**As a** 開發人員
**I want to** 在本機測試 AWS S3 和 SQS 整合，不需要 AWS 帳號
**So that** 我可以快速迭代雲端相關功能的開發

**Acceptance Criteria:**
- LocalStack S3 可上傳、下載、刪除檔案
- LocalStack SQS 可發送、接收訊息，DLQ 正確運作
- Azurite Blob Storage 可上傳、下載二進位檔案

#### US-12: 微服務 API 契約驗證

**As a** 架構師
**I want to** 在 CI 中自動驗證 API 變更是否破壞消費者契約
**So that** 微服務部署前能自動檢測相容性問題

**Acceptance Criteria:**
- Consumer 測試產生 Pact 契約並上傳至 Pact Broker
- Provider 測試自動從 Broker 取得契約並驗證
- `can-i-deploy` 阻擋不相容的部署

---

## Scope & Non-Goals

### In Scope

**Phase 1 (核心基礎):**
- Monorepo 專案骨架建立（root `build.gradle.kts` + `settings.gradle.kts`）
- `tc-common` 共用模組（容器定義、Base Class、共用 DTO）
- `scenario-s1-core`：PostgreSQL + RabbitMQ + REST API
- `scenario-s2-multistore`：Redis + Elasticsearch 多儲存層
- per-module CI pipeline 設定

**Phase 2 (事件驅動與韌性):**
- `scenario-s3-kafka`：Kafka + Schema Registry + Avro
- `scenario-s4-cdc`：Debezium CDC pipeline
- `scenario-s5-resilience`：WireMock + Toxiproxy + Resilience4j

**Phase 3 (安全、雲端與契約):**
- `scenario-s6-security`：Keycloak + Vault
- `scenario-s7-cloud`：LocalStack + Azurite
- `scenario-s8-contract`：Pact Broker 契約測試

### Non-Goals

- 效能壓力測試（Performance / Load Testing）
- 容器編排（Kubernetes / Docker Compose）
- 正式環境部署方案
- UI / E2E 瀏覽器測試
- 多雲（Multi-Cloud）架構設計
- 合規審計報告產出

---

## Risks & Mitigations

| Risk                                     | Impact | Likelihood | Mitigation                                      |
|------------------------------------------|--------|------------|--------------------------------------------------|
| Docker Desktop 授權問題（企業環境）        | High   | Medium     | 評估 Podman / Colima 等替代方案                    |
| CI 環境 Docker-in-Docker 限制             | Medium | Medium     | 確認 CI runner 支援 DinD 或 Docker socket mount   |
| Monorepo build 時間隨模組增長             | Medium | Medium     | Gradle build cache + per-module CI trigger        |
| 多容器同時啟動導致記憶體不足               | High   | Medium     | 各模組獨立啟動容器、最低 8GB RAM for full build    |
| 跨模組依賴管理複雜度                       | Medium | Low        | `tc-common` BOM + version catalog 集中管控        |
| Kafka + Schema Registry 啟動時間過長       | Medium | High       | 預先拉取映像、KRaft mode                           |
| Debezium WAL 設定複雜                     | Medium | Medium     | 標準化 PostgreSQL WAL 設定模板放入 `tc-common`     |
| Keycloak Realm 初始化耗時                  | Low    | Medium     | JSON realm import 自動化                          |
| LocalStack 與 AWS API 行為差異             | Medium | Medium     | 針對差異撰寫文件、邊界測試                          |
| 開發人員對 multi-module 學習曲線            | Low    | High       | 每模組獨立 README + Quick Start                    |

---

## Dependencies

| Dependency                   | Type       | Owner         | Modules                          | Status    |
|------------------------------|------------|---------------|----------------------------------|-----------|
| Docker Engine (≥ 20.10)      | Runtime    | DevOps / 個人  | All                              | Required  |
| Java 21                      | Runtime    | Development   | All                              | Required  |
| Spring Boot 3.4.x            | Framework  | Development   | All                              | Required  |
| Testcontainers 1.20.x        | Library    | Development   | All                              | Required  |
| PostgreSQL 16 Image          | Container  | Docker Hub    | s1-core, s2-multistore, s4-cdc, s6-security | Available |
| RabbitMQ 3.13 Image          | Container  | Docker Hub    | s1-core                          | Available |
| Redis 7 Image                | Container  | Docker Hub    | s2-multistore                    | Available |
| Elasticsearch 8.x Image      | Container  | Docker Hub    | s2-multistore                    | Available |
| Kafka (KRaft) Image          | Container  | Docker Hub    | s3-kafka, s4-cdc                 | Available |
| Schema Registry Image        | Container  | Confluent Hub | s3-kafka                         | Available |
| Debezium Connect Image       | Container  | Docker Hub    | s4-cdc                           | Available |
| WireMock Image               | Container  | Docker Hub    | s5-resilience                    | Available |
| Toxiproxy Image              | Container  | Shopify Hub   | s5-resilience                    | Available |
| Keycloak 24.x Image          | Container  | Quay.io       | s6-security                      | Available |
| Vault Image                  | Container  | Docker Hub    | s6-security                      | Available |
| LocalStack 3.x Image         | Container  | Docker Hub    | s7-cloud                         | Available |
| Azurite Image                | Container  | MCR           | s7-cloud                         | Available |
| Pact Broker Image            | Container  | Docker Hub    | s8-contract                      | Available |

---

## Timeline

| Phase                        | Duration   | Modules                               | Deliverable                                       |
|------------------------------|------------|---------------------------------------|---------------------------------------------------|
| Phase 1a: Monorepo Scaffold  | 1 day      | root, tc-common                       | Gradle monorepo 骨架、共用模組、version catalog    |
| Phase 1b: S1 Core            | 2 days     | scenario-s1-core                      | DB + MQ 容器驗證、端對端訂單流程                     |
| Phase 1c: S2 Multi-Store     | 3 days     | scenario-s2-multistore                | Redis + ES 容器整合、三層資料一致性                  |
| Phase 1d: CI Pipeline        | 1 day      | All Phase 1 modules                   | per-module GitHub Actions matrix CI                |
| Phase 2a: S3 Kafka           | 3 days     | scenario-s3-kafka                     | Kafka + Schema Registry、schema evolution           |
| Phase 2b: S4 CDC             | 3 days     | scenario-s4-cdc                       | Debezium CDC 設定、WAL 捕獲驗證                     |
| Phase 2c: S5 Resilience      | 3 days     | scenario-s5-resilience                | WireMock + Toxiproxy、Resilience4j 驗證             |
| Phase 3a: S6 Security        | 3 days     | scenario-s6-security                  | Keycloak OAuth2 + Vault 動態憑證                    |
| Phase 3b: S7 Cloud           | 2 days     | scenario-s7-cloud                     | LocalStack + Azurite 模擬                           |
| Phase 3c: S8 Contract        | 2 days     | scenario-s8-contract                  | Pact Broker 契約測試流程                             |
| Phase 3d: Documentation      | 2 days     | All                                   | 導入指南、最佳實踐文件、團隊 Demo                    |

**Total: 25 working days (~5 weeks)**

---

## Appendix

### A. 相關參考資源

- [Testcontainers Official Documentation](https://testcontainers.com/)
- [Spring Boot Testcontainers Support](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Gradle Multi-Project Builds](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [Gradle Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html)
- [Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/)
- [Debezium Documentation](https://debezium.io/documentation/)
- [WireMock Documentation](https://wiremock.org/docs/)
- [Toxiproxy](https://github.com/Shopify/toxiproxy)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [HashiCorp Vault](https://developer.hashicorp.com/vault/docs)
- [LocalStack Documentation](https://docs.localstack.cloud/)
- [Pact Documentation](https://docs.pact.io/)

### B. 術語表

| Term                    | Definition                                                        |
|-------------------------|-------------------------------------------------------------------|
| Monorepo                | 多個相關專案共用同一 Git repository 的架構模式                       |
| Gradle Sub Module       | Gradle multi-project build 中的子專案，有獨立的 build.gradle         |
| Version Catalog         | Gradle 集中管理依賴版本的機制（libs.versions.toml）                  |
| Testcontainers          | Java 函式庫，透過 Docker 為整合測試提供輕量級、一次性的容器實例         |
| Singleton Container     | Testcontainers 模式，在多個測試類別間共享同一容器實例                  |
| CDC                     | Change Data Capture，捕獲資料庫資料變更的技術                        |
| WAL                     | Write-Ahead Log，PostgreSQL 的預寫式日誌                            |
| Schema Evolution        | Avro schema 的版本演進，包含向前/向後相容性管理                       |
| Circuit Breaker         | 斷路器模式，防止故障級聯擴散的韌性設計模式                             |
| OIDC                    | OpenID Connect，基於 OAuth2 的身份驗證協定                           |
| Pact                    | 消費者驅動契約測試框架，驗證微服務之間的 API 契約                      |
| LocalStack              | AWS 服務的本機模擬器                                                |
| KRaft                   | Kafka Raft，Kafka 不依賴 ZooKeeper 的共識協定模式                     |
