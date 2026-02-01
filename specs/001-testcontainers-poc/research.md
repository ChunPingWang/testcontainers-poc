# Research: Testcontainers Integration Testing PoC

**Date**: 2026-02-01
**Branch**: `001-testcontainers-poc`
**Plan Reference**: [plan.md](./plan.md)

## Research Topics

### 1. Testcontainers Container Lifecycle Management

**Decision**: 採用 Singleton Container Pattern，同一測試類別共享容器實例

**Rationale**:
- 大幅減少容器啟動時間（每個測試類別僅啟動一次）
- Testcontainers 官方推薦的最佳實踐
- 透過 `@DirtiesContext` 或測試資料清理確保隔離性
- 配合 Spring Boot 3.1+ 的 `@ServiceConnection` 自動注入連線參數

**Alternatives Considered**:
| Alternative | Pros | Cons | Rejected Because |
|-------------|------|------|------------------|
| Per-method container | 完全隔離 | 啟動時間過長，CI 超時風險 | 不符合 SC-002 (≤90s) |
| Global singleton | 最快 | 需複雜的資料清理邏輯 | 增加測試維護成本 |

**Implementation Pattern**:
```java
// Container Factory (in tc-common)
public final class PostgresContainerFactory {
    private static final PostgreSQLContainer<?> INSTANCE =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withReuse(true);

    public static PostgreSQLContainer<?> getInstance() {
        return INSTANCE;
    }
}

// Test Configuration (in scenario module)
@TestConfiguration
public class S1TestApplication {
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgres() {
        return PostgresContainerFactory.getInstance();
    }
}
```

---

### 2. Container Factory Pattern Design

**Decision**: 採用靜態工廠方法 + Singleton Pattern

**Rationale**:
- 比 `@TestConfiguration` Bean 更靈活，支援 singleton 與 fresh instance
- 集中管理所有容器定義於 `tc-common` 模組
- 各場景模組可選擇性使用所需的容器
- 符合 SOLID 的 SRP（每個 Factory 負責一種容器）

**Alternatives Considered**:
| Alternative | Pros | Cons | Rejected Because |
|-------------|------|------|------------------|
| @TestConfiguration in tc-common | Spring 原生支援 | 無法選擇性啟用容器 | 場景模組無法自訂 |
| Abstract test base class | 繼承簡單 | 多重繼承限制、緊耦合 | 違反組合優於繼承原則 |

**Container Factory List**:
| Factory | Container | Default Image | Used By |
|---------|-----------|---------------|---------|
| PostgresContainerFactory | PostgreSQL | postgres:16-alpine | S1, S2, S4, S6 |
| RabbitMqContainerFactory | RabbitMQ | rabbitmq:3.13-management-alpine | S1 |
| RedisContainerFactory | Redis | redis:7-alpine | S2 |
| ElasticsearchContainerFactory | Elasticsearch | elasticsearch:8.13.0 | S2 |
| KafkaContainerFactory | Kafka (KRaft) | confluentinc/cp-kafka:7.6.0 | S3, S4 |
| SchemaRegistryContainerFactory | Schema Registry | confluentinc/cp-schema-registry:7.6.0 | S3 |
| DebeziumContainerFactory | Debezium Connect | debezium/connect:2.6 | S4 |
| WireMockContainerFactory | WireMock | wiremock/wiremock:3.5.2 | S5 |
| ToxiproxyContainerFactory | Toxiproxy | ghcr.io/shopify/toxiproxy:2.9.0 | S5 |
| KeycloakContainerFactory | Keycloak | quay.io/keycloak/keycloak:24.0 | S6 |
| VaultContainerFactory | Vault | hashicorp/vault:1.16 | S6 |
| LocalStackContainerFactory | LocalStack | localstack/localstack:3.4 | S7 |
| AzuriteContainerFactory | Azurite | mcr.microsoft.com/azure-storage/azurite:3.30.0 | S7 |
| PactBrokerContainerFactory | Pact Broker | pactfoundation/pact-broker:latest | S8 |

---

### 3. Spring Boot 3.x Testcontainers Integration

**Decision**: 使用 `@ServiceConnection` 取代 `@DynamicPropertySource`

**Rationale**:
- Spring Boot 3.1+ 原生支援，自動注入連線參數
- 減少樣板程式碼
- 與 Spring Boot Testcontainers starter 整合良好

**Alternatives Considered**:
| Alternative | Pros | Cons | Rejected Because |
|-------------|------|------|------------------|
| @DynamicPropertySource | 向後相容 | 需手動設定每個屬性 | 增加維護成本 |
| application-test.yml | 靜態設定 | 無法動態取得容器 port | 不適用於 Testcontainers |

**Implementation Pattern**:
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(S1TestApplication.class)
class OrderApiIT extends IntegrationTestBase {
    // @ServiceConnection 自動注入 spring.datasource.* 與 spring.rabbitmq.*
}
```

---

### 4. Gradle Monorepo Best Practices

**Decision**: 採用 Version Catalog + Typesafe Project Accessors

**Rationale**:
- `libs.versions.toml` 集中管理所有依賴版本
- `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` 提供 type-safe 專案參照
- `org.gradle.parallel=true` 啟用平行建置
- `org.gradle.caching=true` 啟用建置快取

**Key Configuration**:
```kotlin
// settings.gradle.kts
rootProject.name = "testcontainers-poc"
include("tc-common")
include("scenario-s1-core")
// ... other modules
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx2g -XX:+HeapDumpOnOutOfMemoryError
```

---

### 5. CI/CD Matrix Pipeline Configuration

**Decision**: 採用 `dorny/paths-filter` + Matrix strategy

**Rationale**:
- 僅觸發變更模組的 pipeline，節省 CI 資源
- `tc-common` 變更時觸發所有場景測試
- 各模組測試可平行執行
- 預先拉取容器映像提升效率

**Implementation Pattern**:
```yaml
jobs:
  changes:
    runs-on: ubuntu-latest
    outputs:
      modules: ${{ steps.filter.outputs.modules }}
    steps:
      - uses: dorny/paths-filter@v3
        with:
          filters: |
            tc-common: ['tc-common/**', 'gradle/**']
            s1-core: ['scenario-s1-core/**']
            # ...

  test:
    needs: [changes]
    strategy:
      matrix:
        include:
          - module: scenario-s1-core
            images: "postgres:16-alpine rabbitmq:3.13-management-alpine"
          # ...
    steps:
      - name: Pre-pull Docker images
        run: |
          for img in ${{ matrix.images }}; do
            docker pull "$img" &
          done
          wait
      - run: ./gradlew :${{ matrix.module }}:test
```

---

### 6. Test Failure Diagnostics Collection

**Decision**: 完整診斷包（容器日誌、網路狀態、資源使用量）

**Rationale**:
- 符合 Clarification Session 決議
- 協助快速定位整合測試失敗原因
- CI 環境中尤其重要（無法即時查看容器狀態）

**Implementation Pattern**:
```java
// IntegrationTestBase.java
@AfterEach
void collectDiagnosticsOnFailure(TestInfo testInfo) {
    if (testFailed) {
        // Collect container logs
        containers.forEach(c -> {
            String logs = c.getLogs();
            saveToFile(testInfo.getDisplayName() + "_" + c.getContainerName() + ".log", logs);
        });

        // Collect network state
        String networkInfo = docker.inspectNetwork();
        saveToFile(testInfo.getDisplayName() + "_network.json", networkInfo);

        // Collect resource usage
        String stats = docker.stats();
        saveToFile(testInfo.getDisplayName() + "_stats.json", stats);
    }
}
```

---

### 7. Kafka KRaft Mode Configuration

**Decision**: 使用 KRaft mode 取代 ZooKeeper

**Rationale**:
- 減少容器數量（無需額外的 ZooKeeper 容器）
- Kafka 3.x+ 官方推薦
- 啟動速度更快
- 資源消耗更低

**Implementation Pattern**:
```java
public final class KafkaContainerFactory {
    private static final KafkaContainer INSTANCE =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
            .withKraft()  // Enable KRaft mode
            .withReuse(true);
}
```

---

### 8. Schema Registry Integration

**Decision**: Schema Registry 容器依賴 Kafka 容器網路

**Rationale**:
- Schema Registry 需要連線至 Kafka broker
- 使用相同的 Docker network 確保服務發現
- 支援 Avro schema evolution 測試

**Implementation Pattern**:
```java
public static GenericContainer<?> createSchemaRegistry(KafkaContainer kafka) {
    return new GenericContainer<>(
            DockerImageName.parse("confluentinc/cp-schema-registry:7.6.0"))
        .withNetwork(kafka.getNetwork())
        .withExposedPorts(8081)
        .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
        .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                kafka.getBootstrapServers())
        .dependsOn(kafka);
}
```

---

### 9. Resilience4j Integration Testing

**Decision**: 使用 WireMock + Toxiproxy 組合驗證韌性機制

**Rationale**:
- WireMock 模擬外部 API 回應（正常、錯誤、延遲）
- Toxiproxy 模擬網路層故障（latency、timeout、connection reset）
- 兩者組合可完整驗證 Circuit Breaker、Retry、Fallback

**Implementation Pattern**:
```java
@Test
void shouldOpenCircuitOnConsecutiveFailures() {
    // WireMock: simulate 500 errors
    wireMock.stubFor(get("/api/credit-check")
        .willReturn(serverError()));

    // Trigger failures
    for (int i = 0; i < 5; i++) {
        assertThatThrownBy(() -> apiClient.checkCredit("cust-1"))
            .isInstanceOf(ExternalServiceException.class);
    }

    // Verify circuit opened
    CircuitBreaker cb = cbRegistry.circuitBreaker("creditCheck");
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
}
```

---

### 10. Keycloak Realm Configuration

**Decision**: 使用 JSON realm export 自動初始化

**Rationale**:
- 避免每次測試手動建立 realm、client、users
- JSON 檔案版本控制，確保一致性
- 支援 OAuth2/OIDC 完整流程測試

**Implementation Pattern**:
```java
public final class KeycloakContainerFactory {
    private static final KeycloakContainer INSTANCE =
        new KeycloakContainer("quay.io/keycloak/keycloak:24.0")
            .withRealmImportFile("/keycloak/realm-export.json")
            .withReuse(true);
}
```

---

## Unresolved Items

無。所有 NEEDS CLARIFICATION 項目均已在 Clarification Session 中解決。

## References

- [Testcontainers Official Documentation](https://testcontainers.com/)
- [Spring Boot Testcontainers Support](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Gradle Multi-Project Builds](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [Gradle Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html)
- [Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/)
- [Debezium Documentation](https://debezium.io/documentation/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
