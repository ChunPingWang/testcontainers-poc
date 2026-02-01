# Testcontainers Integration Testing PoC

企業級金融系統整合測試解決方案，使用 Testcontainers 框架實現環境隔離的自動化測試。

## 專案概述

本專案透過 Gradle Monorepo Multi-Module 架構，建立涵蓋 8 大場景的標準化整合測試方案，解決傳統整合測試面臨的環境依賴、資料汙染、CI 瓶頸等問題。

## 先決條件

- Java 21+
- Docker Engine 20.10+
- Gradle 8.x
- 本機至少 8GB RAM（執行完整測試套件）

## 快速開始

```bash
# 執行單一場景測試
./gradlew :scenario-s1-core:test

# 執行所有測試
./gradlew test
```

## CI 環境 Docker 存取策略

Testcontainers 在 CI 環境中需要存取 Docker daemon，本專案支援兩種策略，系統會依據 CI 平台自動選擇最適合的方式。

### 方法一：Docker Socket 掛載（推薦）

將主機的 Docker socket 掛載至 CI runner 容器內，讓 Testcontainers 直接與主機 Docker daemon 通訊。

**優點：**
- 效能較佳，無額外虛擬化開銷
- 設定較簡單
- Testcontainers 官方推薦方式

**缺點：**
- 需要適當的安全控管
- CI runner 容器需要存取主機 Docker socket 的權限

**GitHub Actions 設定範例：**

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run integration tests
        run: ./gradlew test
```

**GitLab CI 設定範例：**

```yaml
integration-test:
  image: eclipse-temurin:21-jdk
  services:
    - docker:dind
  variables:
    DOCKER_HOST: tcp://docker:2375
  script:
    - ./gradlew test
```

**Jenkins 設定範例（Docker socket 掛載）：**

```groovy
pipeline {
    agent {
        docker {
            image 'eclipse-temurin:21-jdk'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }
    stages {
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
    }
}
```

### 方法二：Docker-in-Docker (DinD)

在 CI runner 容器內啟動一個獨立的 Docker daemon，實現完全隔離的 Docker 環境。

**優點：**
- 完全隔離，不影響主機 Docker 環境
- 安全性較高
- 適合多租戶 CI 環境

**缺點：**
- 效能較低，有額外虛擬化開銷
- 設定較複雜
- 需要 privileged 模式運行

**GitHub Actions 設定範例（使用 DinD service）：**

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      dind:
        image: docker:dind
        options: --privileged
        ports:
          - 2375:2375
    env:
      DOCKER_HOST: tcp://localhost:2375
      DOCKER_TLS_CERTDIR: ""
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Wait for Docker
        run: |
          until docker info; do
            echo "Waiting for Docker..."
            sleep 1
          done

      - name: Run integration tests
        run: ./gradlew test
```

**GitLab CI 設定範例（DinD）：**

```yaml
integration-test:
  image: eclipse-temurin:21-jdk
  services:
    - name: docker:dind
      command: ["--tls=false"]
  variables:
    DOCKER_HOST: tcp://docker:2375
    DOCKER_TLS_CERTDIR: ""
  script:
    - ./gradlew test
```

### 自動偵測機制

本專案的 `tc-common` 模組會自動偵測 CI 環境並選擇適當的 Docker 存取策略：

1. 檢查 `DOCKER_HOST` 環境變數是否已設定
2. 檢查 `/var/run/docker.sock` 是否存在且可存取
3. 嘗試連線至 `tcp://localhost:2375`（DinD 預設端口）
4. 根據偵測結果自動配置 Testcontainers

開發人員無需手動配置，系統會自動處理。

## 容器映像快取策略

為確保 CI 測試的穩定性與效能，建議採用以下映像快取策略：

### 策略一：內部 Registry 快取（正式環境推薦）

在企業內部架設容器映像 registry（如 Harbor、Nexus、Artifactory），定期從公開 registry 同步所需映像。

**優點：**
- 避免外部 registry 限流（Docker Hub rate limit）或故障影響
- 確保映像版本一致性
- 符合企業安全政策

**設定方式：**

```properties
# testcontainers.properties
docker.registry=harbor.internal.company.com
```

```yaml
# GitHub Actions 範例
env:
  TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX: harbor.internal.company.com/
```

### 策略二：CI Runner 本地快取（建議選項）

利用 CI runner 的本地 Docker 映像快取，避免重複拉取。適合沒有內部 registry 的團隊快速導入。

**優點：**
- 設定簡單，無需額外基礎設施
- 後續執行速度快
- 適合中小型團隊

**缺點：**
- 首次執行或快取失效時較慢
- 需要足夠的磁碟空間

**GitHub Actions 設定範例：**

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      # 預先拉取常用映像以利用 runner 快取
      - name: Pre-pull container images
        run: |
          docker pull postgres:16-alpine
          docker pull rabbitmq:3.13-management-alpine
          docker pull redis:7-alpine

      - name: Run integration tests
        run: ./gradlew test
```

**GitLab CI 設定範例（使用 cache）：**

```yaml
variables:
  DOCKER_DRIVER: overlay2

integration-test:
  image: eclipse-temurin:21-jdk
  services:
    - docker:dind
  before_script:
    # 預先拉取映像
    - docker pull postgres:16-alpine
    - docker pull rabbitmq:3.13-management-alpine
  script:
    - ./gradlew test
  cache:
    key: docker-images
    paths:
      - /var/lib/docker
```

**Jenkins 設定範例（持久化 Docker 層）：**

```groovy
pipeline {
    agent {
        docker {
            image 'eclipse-temurin:21-jdk'
            args '''
                -v /var/run/docker.sock:/var/run/docker.sock
                -v docker-cache:/var/lib/docker
            '''
        }
    }
    stages {
        stage('Pre-pull Images') {
            steps {
                sh '''
                    docker pull postgres:16-alpine
                    docker pull rabbitmq:3.13-management-alpine
                    docker pull redis:7-alpine
                '''
            }
        }
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
    }
}
```

### 本專案使用的容器映像

| 場景 | 映像 | 用途 |
|------|------|------|
| S1, S2, S4, S6 | `postgres:16-alpine` | 主資料庫 |
| S1 | `rabbitmq:3.13-management-alpine` | 訊息佇列 |
| S2 | `redis:7-alpine` | 快取 |
| S2 | `elasticsearch:8.x` | 搜尋索引 |
| S3, S4 | `confluentinc/cp-kafka` | 事件串流 |
| S3 | `confluentinc/cp-schema-registry` | Schema 管理 |
| S4 | `debezium/connect` | CDC 連接器 |
| S5 | `wiremock/wiremock` | API Mock |
| S5 | `shopify/toxiproxy` | 故障注入 |
| S6 | `quay.io/keycloak/keycloak` | 身份驗證 |
| S6 | `hashicorp/vault` | 密鑰管理 |
| S7 | `localstack/localstack` | AWS 模擬 |
| S7 | `mcr.microsoft.com/azure-storage/azurite` | Azure 模擬 |
| S8 | `pactfoundation/pact-broker` | 契約管理 |

## 專案結構

```
testcontainers-poc/
├── tc-common/                    # 共用測試基礎設施
├── scenario-s1-core/             # Phase 1: DB + MQ + API
├── scenario-s2-multistore/       # Phase 1: PostgreSQL + Redis + ES
├── scenario-s3-kafka/            # Phase 2: Kafka + Schema Registry
├── scenario-s4-cdc/              # Phase 2: Debezium CDC
├── scenario-s5-resilience/       # Phase 2: WireMock + Toxiproxy
├── scenario-s6-security/         # Phase 3: Keycloak + Vault
├── scenario-s7-cloud/            # Phase 3: LocalStack + Azurite
└── scenario-s8-contract/         # Phase 3: Pact Broker
```

## 相關文件

- [功能規格](specs/001-testcontainers-poc/spec.md)
- [PRD](PRD.md)

## 授權

MIT License
