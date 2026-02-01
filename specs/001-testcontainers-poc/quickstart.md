# Quickstart: Testcontainers Integration Testing PoC

**Date**: 2026-02-01
**Branch**: `001-testcontainers-poc`

## 先決條件

在開始之前，請確認您的環境已安裝：

| 軟體 | 版本要求 | 檢查指令 |
|------|----------|----------|
| Java | 21+ | `java --version` |
| Docker | 20.10+ | `docker --version` |
| Gradle | 8.x (或使用 wrapper) | `./gradlew --version` |

**系統需求**:
- RAM: 最低 8GB（執行完整測試套件）
- 磁碟空間: 至少 10GB（容器映像）

## 快速開始

### 1. Clone 專案

```bash
git clone https://github.com/ChunPingWang/testcontainers-poc.git
cd testcontainers-poc
git checkout 001-testcontainers-poc
```

### 2. 驗證環境

```bash
# 確認 Docker 運行中
docker info

# 確認 Java 版本
java --version
```

### 3. 執行單一場景測試

```bash
# S1: 基礎整合 (DB + MQ + API)
./gradlew :scenario-s1-core:test

# S2: 多儲存層 (PostgreSQL + Redis + ES)
./gradlew :scenario-s2-multistore:test

# S3: 事件驅動 (Kafka + Schema Registry)
./gradlew :scenario-s3-kafka:test

# S5: 韌性測試 (WireMock + Toxiproxy)
./gradlew :scenario-s5-resilience:test
```

### 4. 執行所有測試

```bash
./gradlew test
```

### 5. 查看測試報告

```bash
# 開啟瀏覽器查看報告
open scenario-s1-core/build/reports/tests/test/index.html
```

## 場景模組概覽

| 模組 | 描述 | 容器 | 執行指令 |
|------|------|------|----------|
| `scenario-s1-core` | DB + MQ + API | PostgreSQL, RabbitMQ | `./gradlew :scenario-s1-core:test` |
| `scenario-s2-multistore` | 多儲存層 | PostgreSQL, Redis, ES | `./gradlew :scenario-s2-multistore:test` |
| `scenario-s3-kafka` | 事件串流 | Kafka, Schema Registry | `./gradlew :scenario-s3-kafka:test` |
| `scenario-s4-cdc` | CDC | PostgreSQL, Kafka, Debezium | `./gradlew :scenario-s4-cdc:test` |
| `scenario-s5-resilience` | 韌性 | WireMock, Toxiproxy | `./gradlew :scenario-s5-resilience:test` |
| `scenario-s6-security` | 安全 | Keycloak, Vault | `./gradlew :scenario-s6-security:test` |
| `scenario-s7-cloud` | 雲端模擬 | LocalStack, Azurite | `./gradlew :scenario-s7-cloud:test` |
| `scenario-s8-contract` | 契約測試 | Pact Broker | `./gradlew :scenario-s8-contract:test` |

## 常用指令

### 建置與測試

```bash
# 建置所有模組（不含測試）
./gradlew build -x test

# 執行 Phase 1 場景測試
./gradlew :scenario-s1-core:test :scenario-s2-multistore:test

# 執行 Phase 2 場景測試
./gradlew :scenario-s3-kafka:test :scenario-s4-cdc:test :scenario-s5-resilience:test

# 執行 Phase 3 場景測試
./gradlew :scenario-s6-security:test :scenario-s7-cloud:test :scenario-s8-contract:test
```

### 診斷與除錯

```bash
# 查看模組相依
./gradlew dependencies --configuration testRuntimeClasspath

# 查看測試輸出
./gradlew :scenario-s1-core:test --info

# 清理所有建置產物
./gradlew clean
```

### 產生報告

```bash
# 單一模組覆蓋率報告
./gradlew :scenario-s1-core:jacocoTestReport
open scenario-s1-core/build/reports/jacoco/test/html/index.html

# 整合覆蓋率報告
./gradlew jacocoAggregatedReport
```

## 預先拉取容器映像

首次執行測試時，Testcontainers 會自動拉取所需的容器映像。若要加速後續執行，可預先拉取：

```bash
# Phase 1 映像
docker pull postgres:16-alpine
docker pull rabbitmq:3.13-management-alpine
docker pull redis:7-alpine
docker pull docker.elastic.co/elasticsearch/elasticsearch:8.13.0

# Phase 2 映像
docker pull confluentinc/cp-kafka:7.6.0
docker pull confluentinc/cp-schema-registry:7.6.0
docker pull debezium/connect:2.6
docker pull wiremock/wiremock:3.5.2
docker pull ghcr.io/shopify/toxiproxy:2.9.0

# Phase 3 映像
docker pull quay.io/keycloak/keycloak:24.0
docker pull hashicorp/vault:1.16
docker pull localstack/localstack:3.4
docker pull mcr.microsoft.com/azure-storage/azurite:3.30.0
docker pull pactfoundation/pact-broker:latest
```

## 疑難排解

### Docker 未運行

```
Error: Could not find a valid Docker environment
```

**解決方案**: 啟動 Docker Desktop 或 Docker daemon

```bash
# macOS/Windows: 啟動 Docker Desktop
# Linux:
sudo systemctl start docker
```

### 記憶體不足

```
Error: Container failed to start (OOM)
```

**解決方案**: 增加 Docker 記憶體限制或僅執行單一模組測試

```bash
# 僅執行單一模組
./gradlew :scenario-s1-core:test
```

### 埠號衝突

Testcontainers 使用動態埠號，通常不會發生衝突。若發生問題：

```bash
# 清理所有 Testcontainers 容器
docker rm -f $(docker ps -aq --filter "label=org.testcontainers")

# 清理 Ryuk 容器
docker rm -f $(docker ps -aq --filter "name=testcontainers-ryuk")
```

### 容器映像拉取失敗

```
Error: Failed to pull image
```

**解決方案**:
1. 檢查網路連線
2. 檢查 Docker Hub rate limit
3. 使用內部 registry（參考 README.md 的映像快取策略）

## 驗證安裝

執行以下指令驗證環境設定正確：

```bash
# 1. 建置共用模組
./gradlew :tc-common:build

# 2. 執行最簡單的場景（S1）
./gradlew :scenario-s1-core:test

# 預期結果:
# - 容器自動啟動
# - 測試全部通過
# - 容器自動清理
```

若所有測試通過，恭喜您的環境已準備就緒！

## 下一步

1. 閱讀 [功能規格](./spec.md) 了解各場景的驗收標準
2. 閱讀 [研究文件](./research.md) 了解技術決策
3. 閱讀 [資料模型](./data-model.md) 了解實體設計
4. 執行 `/speckit.tasks` 產生實作任務清單
