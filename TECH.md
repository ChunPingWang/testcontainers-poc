# TECH: Testcontainers Integration Testing PoC

## Status

| Field          | Value                                    |
|----------------|------------------------------------------|
| Status         | Draft v3                                 |
| Author(s)      | Rex (Application Architect)              |
| Created        | 2026-02-01                               |
| Last Updated   | 2026-02-01                               |
| PRD Reference  | [PRD.md](./PRD.md)                       |
| Review Status  | Pending                                  |

---

## Overview

本文件定義 Testcontainers PoC 的技術架構、實作細節與程式碼規範。PoC 採用 **Gradle Monorepo Multi-Module** 架構，每個場景為獨立子模組，共享 `tc-common` 通用模組。使用 **Gradle Version Catalog** 集中管理依賴版本，確保所有模組版本一致。

---

## Architecture

### Monorepo Module Dependency Graph

```
                           ┌──────────────────┐
                           │   Root Project    │
                           │  (build config,   │
                           │   version catalog)│
                           └────────┬─────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
            ┌──────────────┐ ┌────────────┐  ┌────────────────┐
            │  tc-common    │ │  scenario  │  │   scenario     │
            │              │ │  modules   │  │   modules      │
            │ • Container  │ │  (S1..S4)  │  │   (S5..S8)     │
            │   Configs    │ │            │  │                │
            │ • Base Class │ └─────┬──────┘  └───────┬────────┘
            │ • Shared DTO │       │                 │
            └──────┬───────┘       │                 │
                   │               │                 │
                   └───────depends on────────────────┘

    Every scenario-* module depends on tc-common (testImplementation)
    No scenario module depends on another scenario module
```

### Full System Context

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                              Gradle Monorepo                                   │
│                                                                                │
│  ┌─ tc-common ─────────────────────────────────────────────────────────────┐   │
│  │  Container Definitions │ IntegrationTestBase │ Shared DTOs & Utilities  │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│       ▲          ▲          ▲          ▲          ▲         ▲        ▲         │
│       │          │          │          │          │         │        │         │
│  ┌────┴───┐ ┌───┴────┐ ┌───┴───┐ ┌───┴──┐ ┌────┴───┐ ┌──┴───┐ ┌──┴───┐    │
│  │S1:core │ │S2:multi│ │S3:kfka│ │S4:cdc│ │S5:resil│ │S6:sec│ │S7:cld│    │
│  │        │ │ store  │ │       │ │      │ │  ience │ │urity │ │ oud  │    │
│  │Pg+RMQ  │ │Pg+Rd+ES│ │Kf+SR │ │Kf+Dbz│ │WM+Toxy│ │KC+Vlt│ │LS+Az│    │
│  └────────┘ └────────┘ └───────┘ └──────┘ └────────┘ └──────┘ └──────┘    │
│                                                                     ┌──────┐  │
│                                                                     │S8:pct│  │
│                                                                     └──────┘  │
└────────────────────────────────────────────────────────────────────────────────┘
                                     │
                                Docker Engine
```

### Monorepo Project Structure

```
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
│       ├── containers/                     #   可重用的容器定義
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
│       ├── base/                           #   測試 Base Classes
│       │   └── IntegrationTestBase.java
│       ├── dto/                            #   共用 DTO
│       │   ├── CreateOrderRequest.java
│       │   └── OrderResponse.java
│       └── util/                           #   共用工具
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
│       │   │   ├── Order.java
│       │   │   └── OrderStatus.java
│       │   ├── repository/
│       │   │   └── OrderRepository.java
│       │   ├── service/
│       │   │   └── OrderService.java
│       │   ├── messaging/
│       │   │   ├── OrderEventPublisher.java
│       │   │   └── OrderEventConsumer.java
│       │   ├── web/
│       │   │   └── OrderController.java
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
│   ├── build.gradle.kts
│   ├── README.md
│   └── src/
│       ├── main/java/com/example/s2/
│       │   ├── S2Application.java
│       │   ├── domain/
│       │   │   └── Customer.java
│       │   ├── repository/
│       │   │   └── CustomerRepository.java
│       │   ├── service/
│       │   │   ├── CacheService.java
│       │   │   ├── SearchService.java
│       │   │   └── CustomerService.java
│       │   └── config/
│       │       ├── RedisConfig.java
│       │       └── ElasticsearchConfig.java
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/
│       │       └── V1__create_customers_table.sql
│       └── test/java/com/example/s2/
│           ├── S2TestApplication.java
│           ├── RedisCacheIT.java
│           ├── ElasticsearchSyncIT.java
│           └── MultiStoreConsistencyIT.java
│
├── scenario-s3-kafka/                      # ═══ S3: Kafka + Schema Registry ═══
│   ├── build.gradle.kts
│   ├── README.md
│   └── src/
│       ├── main/java/com/example/s3/
│       │   ├── S3Application.java
│       │   ├── producer/
│       │   │   └── OrderEventProducer.java
│       │   ├── consumer/
│       │   │   └── OrderEventConsumer.java
│       │   └── config/
│       │       └── KafkaConfig.java
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── avro/
│       │       ├── order-event-v1.avsc
│       │       └── order-event-v2.avsc
│       └── test/java/com/example/s3/
│           ├── S3TestApplication.java
│           ├── KafkaProducerConsumerIT.java
│           └── SchemaEvolutionIT.java
│
├── scenario-s4-cdc/                        # ═══ S4: Debezium CDC ═══
│   ├── build.gradle.kts
│   ├── README.md
│   └── src/
│       ├── main/java/com/example/s4/
│       │   ├── S4Application.java
│       │   ├── domain/
│       │   │   └── Transaction.java
│       │   ├── repository/
│       │   │   └── TransactionRepository.java
│       │   └── cdc/
│       │       └── CdcEventProcessor.java
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/
│       │       └── V1__create_transactions_table.sql
│       └── test/java/com/example/s4/
│           ├── S4TestApplication.java
│           ├── DebeziumCdcIT.java
│           └── CdcSchemaChangeIT.java
│
├── scenario-s5-resilience/                 # ═══ S5: WireMock + Toxiproxy ═══
│   ├── build.gradle.kts
│   ├── README.md
│   └── src/
│       ├── main/java/com/example/s5/
│       │   ├── S5Application.java
│       │   ├── client/
│       │   │   └── ExternalApiClient.java
│       │   ├── service/
│       │   │   └── CreditCheckService.java
│       │   └── config/
│       │       └── ResilienceConfig.java
│       ├── main/resources/
│       │   └── application.yml
│       └── test/java/com/example/s5/
│           ├── S5TestApplication.java
│           ├── WireMockApiIT.java
│           ├── ToxiproxyFaultIT.java
│           └── CircuitBreakerIT.java
│
├── scenario-s6-security/                   # ═══ S6: Keycloak + Vault ═══
│   ├── build.gradle.kts
│   ├── README.md
│   └── src/
│       ├── main/java/com/example/s6/
│       │   ├── S6Application.java
│       │   ├── web/
│       │   │   ├── SecuredOrderController.java
│       │   │   └── AdminController.java
│       │   └── config/
│       │       └── SecurityConfig.java
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── keycloak/
│       │       └── realm-export.json
│       └── test/java/com/example/s6/
│           ├── S6TestApplication.java
│           ├── KeycloakAuthIT.java
│           └── VaultCredentialIT.java
│
├── scenario-s7-cloud/                      # ═══ S7: LocalStack + Azurite ═══
│   ├── build.gradle.kts
│   ├── README.md
│   └── src/
│       ├── main/java/com/example/s7/
│       │   ├── S7Application.java
│       │   ├── aws/
│       │   │   ├── S3FileService.java
│       │   │   ├── SqsMessageService.java
│       │   │   └── DynamoDbService.java
│       │   ├── azure/
│       │   │   └── BlobStorageService.java
│       │   └── config/
│       │       ├── AwsConfig.java
│       │       └── AzureConfig.java
│       ├── main/resources/
│       │   └── application.yml
│       └── test/java/com/example/s7/
│           ├── S7TestApplication.java
│           ├── LocalStackS3IT.java
│           ├── LocalStackSqsIT.java
│           ├── LocalStackDynamoDbIT.java
│           └── AzuriteBlobIT.java
│
├── scenario-s8-contract/                   # ═══ S8: Pact Broker ═══
│   ├── build.gradle.kts
│   ├── README.md
│   └── src/
│       ├── main/java/com/example/s8/
│       │   ├── S8Application.java
│       │   ├── web/
│       │   │   └── OrderController.java
│       │   └── service/
│       │       └── OrderService.java
│       ├── main/resources/
│       │   └── application.yml
│       └── test/java/com/example/s8/
│           ├── S8TestApplication.java
│           ├── OrderConsumerPactIT.java
│           └── OrderProviderPactIT.java
│
└── .github/
    └── workflows/
        └── ci.yml                          # Matrix-based CI pipeline
```

---

## Gradle Build Configuration

### Version Catalog (`gradle/libs.versions.toml`)

```toml
[versions]
spring-boot = "3.4.1"
spring-dependency-management = "1.1.7"
testcontainers = "1.20.4"
avro = "1.11.3"
confluent = "7.6.0"
resilience4j = "2.2.0"
aws-sdk = "2.25.0"
azure-blob = "12.25.0"
wiremock = "3.5.2"
pact = "4.6.14"
keycloak-tc = "3.3.0"
rest-assured = "5.4.0"
awaitility = "4.2.0"
avro-plugin = "1.9.1"

[libraries]
# ── Spring Boot Starters ──
spring-boot-starter-web           = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-starter-data-jpa      = { module = "org.springframework.boot:spring-boot-starter-data-jpa" }
spring-boot-starter-validation    = { module = "org.springframework.boot:spring-boot-starter-validation" }
spring-boot-starter-amqp          = { module = "org.springframework.boot:spring-boot-starter-amqp" }
spring-boot-starter-data-redis    = { module = "org.springframework.boot:spring-boot-starter-data-redis" }
spring-boot-starter-data-es       = { module = "org.springframework.boot:spring-boot-starter-data-elasticsearch" }
spring-boot-starter-oauth2-rs     = { module = "org.springframework.boot:spring-boot-starter-oauth2-resource-server" }
spring-boot-starter-test          = { module = "org.springframework.boot:spring-boot-starter-test" }
spring-boot-testcontainers        = { module = "org.springframework.boot:spring-boot-testcontainers" }

# ── Kafka & Avro ──
spring-kafka                      = { module = "org.springframework.kafka:spring-kafka" }
spring-kafka-test                 = { module = "org.springframework.kafka:spring-kafka-test" }
kafka-avro-serializer             = { module = "io.confluent:kafka-avro-serializer", version.ref = "confluent" }
avro                              = { module = "org.apache.avro:avro", version.ref = "avro" }

# ── Resilience ──
resilience4j-spring-boot3         = { module = "io.github.resilience4j:resilience4j-spring-boot3", version.ref = "resilience4j" }

# ── AWS & Azure ──
aws-bom                           = { module = "software.amazon.awssdk:bom", version.ref = "aws-sdk" }
aws-s3                            = { module = "software.amazon.awssdk:s3" }
aws-sqs                           = { module = "software.amazon.awssdk:sqs" }
aws-dynamodb                      = { module = "software.amazon.awssdk:dynamodb" }
azure-storage-blob                = { module = "com.azure:azure-storage-blob", version.ref = "azure-blob" }

# ── Database ──
postgresql                        = { module = "org.postgresql:postgresql" }
flyway-core                       = { module = "org.flywaydb:flyway-core" }
flyway-postgresql                 = { module = "org.flywaydb:flyway-database-postgresql" }

# ── Testcontainers ──
tc-junit-jupiter                  = { module = "org.testcontainers:junit-jupiter", version.ref = "testcontainers" }
tc-postgresql                     = { module = "org.testcontainers:postgresql", version.ref = "testcontainers" }
tc-rabbitmq                       = { module = "org.testcontainers:rabbitmq", version.ref = "testcontainers" }
tc-kafka                          = { module = "org.testcontainers:kafka", version.ref = "testcontainers" }
tc-elasticsearch                  = { module = "org.testcontainers:elasticsearch", version.ref = "testcontainers" }
tc-toxiproxy                      = { module = "org.testcontainers:toxiproxy", version.ref = "testcontainers" }
tc-vault                          = { module = "org.testcontainers:vault", version.ref = "testcontainers" }
tc-localstack                     = { module = "org.testcontainers:localstack", version.ref = "testcontainers" }

# ── Additional Test Libraries ──
wiremock-standalone               = { module = "org.wiremock:wiremock-standalone", version.ref = "wiremock" }
keycloak-testcontainer            = { module = "com.github.dasniko:testcontainers-keycloak", version.ref = "keycloak-tc" }
pact-consumer-junit5              = { module = "au.com.dius.pact.consumer:junit5", version.ref = "pact" }
pact-provider-junit5              = { module = "au.com.dius.pact.provider:junit5", version.ref = "pact" }
rest-assured                      = { module = "io.rest-assured:rest-assured", version.ref = "rest-assured" }
awaitility                        = { module = "org.awaitility:awaitility", version.ref = "awaitility" }
spring-rabbit-test                = { module = "org.springframework.amqp:spring-rabbit-test" }
spring-security-test              = { module = "org.springframework.security:spring-security-test" }

[plugins]
spring-boot                       = { id = "org.springframework.boot", version.ref = "spring-boot" }
spring-dependency-management      = { id = "io.spring.dependency-management", version.ref = "spring-dependency-management" }
avro-gradle                       = { id = "com.github.davidmc24.gradle.plugin.avro", version.ref = "avro-plugin" }
pact                              = { id = "au.com.dius.pact", version.ref = "pact" }
```

### Root `settings.gradle.kts`

```kotlin
rootProject.name = "testcontainers-poc"

// ── Shared module ──
include("tc-common")

// ── Scenario modules ──
include("scenario-s1-core")
include("scenario-s2-multistore")
include("scenario-s3-kafka")
include("scenario-s4-cdc")
include("scenario-s5-resilience")
include("scenario-s6-security")
include("scenario-s7-cloud")
include("scenario-s8-contract")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
```

### Root `build.gradle.kts`

```kotlin
plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

allprojects {
    group = "com.example"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // Testcontainers 相關 JVM 參數
        jvmArgs("-Xms512m", "-Xmx1g")
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
}
```

### Root `gradle.properties`

```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx2g -XX:+HeapDumpOnOutOfMemoryError
```

---

## Module Build Configurations

### `tc-common/build.gradle.kts`

```kotlin
// tc-common 是 Java Library，不是 Spring Boot application
plugins {
    `java-library`
    alias(libs.plugins.spring.dependency.management)
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    // ── 所有容器定義都在這裡，以 api 曝露給子模組 ──
    api(libs.spring.boot.testcontainers)
    api(libs.tc.junit.jupiter)
    api(libs.spring.boot.starter.test)
    api(libs.awaitility)
    api(libs.rest.assured)

    // ── 各場景容器模組 (api scope → 子模組可直接使用) ──
    api(libs.tc.postgresql)
    api(libs.tc.rabbitmq)
    api(libs.tc.kafka)
    api(libs.tc.elasticsearch)
    api(libs.tc.toxiproxy)
    api(libs.tc.vault)
    api(libs.tc.localstack)
    api(libs.wiremock.standalone)
    api(libs.keycloak.testcontainer)
}
```

### `scenario-s1-core/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // ── Production ──
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.amqp)
    runtimeOnly(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    // ── Test: 僅引入 tc-common + S1 所需容器 ──
    testImplementation(project(":tc-common"))
    testImplementation(libs.spring.rabbit.test)
}
```

### `scenario-s2-multistore/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.data.es)
    runtimeOnly(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    testImplementation(project(":tc-common"))
}
```

### `scenario-s3-kafka/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.avro.gradle)
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.kafka)
    implementation(libs.kafka.avro.serializer)
    implementation(libs.avro)

    testImplementation(project(":tc-common"))
    testImplementation(libs.spring.kafka.test)
}
```

### `scenario-s4-cdc/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.kafka)
    runtimeOnly(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    testImplementation(project(":tc-common"))
    testImplementation(libs.spring.kafka.test)
}
```

### `scenario-s5-resilience/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.resilience4j.spring.boot3)

    testImplementation(project(":tc-common"))
}
```

### `scenario-s6-security/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.oauth2.rs)
    runtimeOnly(libs.postgresql)

    testImplementation(project(":tc-common"))
    testImplementation(libs.spring.security.test)
}
```

### `scenario-s7-cloud/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(platform(libs.aws.bom))
    implementation(libs.aws.s3)
    implementation(libs.aws.sqs)
    implementation(libs.aws.dynamodb)
    implementation(libs.azure.storage.blob)

    testImplementation(project(":tc-common"))
}
```

### `scenario-s8-contract/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.pact)
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    runtimeOnly(libs.postgresql)

    testImplementation(project(":tc-common"))
    testImplementation(libs.pact.consumer.junit5)
    testImplementation(libs.pact.provider.junit5)
}
```

---

## tc-common: Shared Test Infrastructure

### Container Factory Pattern

每個容器定義為靜態工廠方法，場景模組在 `@TestConfiguration` 中選擇性調用。

```java
// tc-common: PostgresContainerFactory.java
public final class PostgresContainerFactory {

    private PostgresContainerFactory() {}

    private static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    /** Singleton instance — shared across all tests within a JVM */
    public static PostgreSQLContainer<?> getInstance() {
        return INSTANCE;
    }

    /** Fresh instance — isolated per test class */
    public static PostgreSQLContainer<?> create() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
    }
}
```

```java
// tc-common: KafkaContainerFactory.java
public final class KafkaContainerFactory {

    private KafkaContainerFactory() {}

    private static final KafkaContainer INSTANCE =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
                    .withKraft()
                    .withReuse(true);

    public static KafkaContainer getInstance() { return INSTANCE; }

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
}
```

```java
// tc-common: IntegrationTestBase.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected TestRestTemplate restTemplate;
}
```

---

## Scenario Implementation Examples

### S1: Test Configuration (using tc-common factories)

```java
// scenario-s1-core: S1TestApplication.java
@TestConfiguration(proxyBeanMethods = false)
public class S1TestApplication {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgres() {
        return PostgresContainerFactory.getInstance();
    }

    @Bean
    @ServiceConnection
    public RabbitMQContainer rabbitmq() {
        return RabbitMqContainerFactory.getInstance();
    }
}
```

```java
// scenario-s1-core: OrderApiIT.java
@Import(S1TestApplication.class)
class OrderApiIT extends IntegrationTestBase {

    @Autowired private OrderRepository orderRepository;

    @Test
    @DisplayName("Should create order via REST API and verify full flow")
    void shouldCreateOrderEndToEnd() {
        var request = new CreateOrderRequest(
                "金控客戶", "信用卡服務", 2, new BigDecimal("25000.00"));

        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                "/api/orders", request, OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID orderId = response.getBody().id();
        assertThat(orderRepository.findById(orderId)).isPresent();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<OrderResponse> get = restTemplate.getForEntity(
                    "/api/orders/" + orderId, OrderResponse.class);
            assertThat(get.getBody().status()).isEqualTo("CONFIRMED");
        });
    }
}
```

### S5: Resilience Test (module-scoped containers)

```java
// scenario-s5-resilience: S5TestApplication.java
@TestConfiguration(proxyBeanMethods = false)
public class S5TestApplication {

    @Bean
    public WireMockServer wireMock() {
        return WireMockContainerFactory.createServer();
    }

    @Bean
    public ToxiproxyContainer toxiproxy() {
        return ToxiproxyContainerFactory.getInstance();
    }
}
```

```java
// scenario-s5-resilience: CircuitBreakerIT.java
@Import(S5TestApplication.class)
class CircuitBreakerIT extends IntegrationTestBase {

    @Autowired private ExternalApiClient apiClient;
    @Autowired private CircuitBreakerRegistry cbRegistry;

    @Test
    @DisplayName("Circuit breaker opens after consecutive failures")
    void shouldOpenCircuitOnConsecutiveFailures() {
        wireMock.stubFor(get("/api/credit-check")
                .willReturn(serverError()));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> apiClient.checkCredit("cust-1"))
                    .isInstanceOf(ExternalServiceException.class);
        }

        CircuitBreaker cb = cbRegistry.circuitBreaker("creditCheck");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
```

---

## CI/CD Configuration

### Matrix-Based Pipeline (`ci.yml`)

```yaml
name: Testcontainers PoC CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  # ──────────────────────────────────────────
  # Detect which modules changed
  # ──────────────────────────────────────────
  changes:
    runs-on: ubuntu-latest
    outputs:
      modules: ${{ steps.filter.outputs.modules }}
    steps:
      - uses: actions/checkout@v4
      - id: filter
        uses: dorny/paths-filter@v3
        with:
          filters: |
            tc-common:
              - 'tc-common/**'
              - 'gradle/**'
              - 'build.gradle.kts'
            s1-core:
              - 'scenario-s1-core/**'
            s2-multistore:
              - 'scenario-s2-multistore/**'
            s3-kafka:
              - 'scenario-s3-kafka/**'
            s4-cdc:
              - 'scenario-s4-cdc/**'
            s5-resilience:
              - 'scenario-s5-resilience/**'
            s6-security:
              - 'scenario-s6-security/**'
            s7-cloud:
              - 'scenario-s7-cloud/**'
            s8-contract:
              - 'scenario-s8-contract/**'

  # ──────────────────────────────────────────
  # Build tc-common (triggers all if changed)
  # ──────────────────────────────────────────
  tc-common:
    needs: changes
    if: needs.changes.outputs.modules != '[]'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew :tc-common:build

  # ──────────────────────────────────────────
  # Per-module test matrix
  # ──────────────────────────────────────────
  test:
    needs: [changes, tc-common]
    runs-on: ubuntu-latest
    timeout-minutes: 15
    strategy:
      fail-fast: false
      matrix:
        include:
          - module: scenario-s1-core
            changed: ${{ needs.changes.outputs.modules == 'tc-common' || contains(needs.changes.outputs.modules, 's1-core') }}
            images: "postgres:16-alpine rabbitmq:3.13-management-alpine"
          - module: scenario-s2-multistore
            changed: ${{ needs.changes.outputs.modules == 'tc-common' || contains(needs.changes.outputs.modules, 's2-multistore') }}
            images: "postgres:16-alpine redis:7-alpine docker.elastic.co/elasticsearch/elasticsearch:8.13.0"
          - module: scenario-s3-kafka
            changed: ${{ needs.changes.outputs.modules == 'tc-common' || contains(needs.changes.outputs.modules, 's3-kafka') }}
            images: "confluentinc/cp-kafka:7.6.0 confluentinc/cp-schema-registry:7.6.0"
          - module: scenario-s4-cdc
            changed: ${{ needs.changes.outputs.modules == 'tc-common' || contains(needs.changes.outputs.modules, 's4-cdc') }}
            images: "postgres:16-alpine confluentinc/cp-kafka:7.6.0 debezium/connect:2.6"
          - module: scenario-s5-resilience
            changed: ${{ needs.changes.outputs.modules == 'tc-common' || contains(needs.changes.outputs.modules, 's5-resilience') }}
            images: "wiremock/wiremock:3.5.2 ghcr.io/shopify/toxiproxy:2.9.0"
          - module: scenario-s6-security
            changed: ${{ needs.changes.outputs.modules == 'tc-common' || contains(needs.changes.outputs.modules, 's6-security') }}
            images: "quay.io/keycloak/keycloak:24.0 hashicorp/vault:1.16 postgres:16-alpine"
          - module: scenario-s7-cloud
            changed: ${{ needs.changes.outputs.modules == 'tc-common' || contains(needs.changes.outputs.modules, 's7-cloud') }}
            images: "localstack/localstack:3.4 mcr.microsoft.com/azure-storage/azurite:3.30.0"
          - module: scenario-s8-contract
            changed: ${{ needs.changes.outputs.modules == 'tc-common' || contains(needs.changes.outputs.modules, 's8-contract') }}
            images: "pactfoundation/pact-broker:latest postgres:16-alpine"

    steps:
      - uses: actions/checkout@v4
        if: matrix.changed == 'true' || github.event_name == 'pull_request'

      - uses: actions/setup-java@v4
        if: matrix.changed == 'true' || github.event_name == 'pull_request'
        with: { java-version: '21', distribution: 'temurin' }

      - uses: gradle/actions/setup-gradle@v3
        if: matrix.changed == 'true' || github.event_name == 'pull_request'

      - name: Pre-pull Docker images
        if: matrix.changed == 'true' || github.event_name == 'pull_request'
        run: |
          for img in ${{ matrix.images }}; do
            docker pull "$img" &
          done
          wait

      - name: Test ${{ matrix.module }}
        if: matrix.changed == 'true' || github.event_name == 'pull_request'
        run: ./gradlew :${{ matrix.module }}:test --info

      - name: Publish Test Report
        uses: dorny/test-reporter@v1
        if: always() && (matrix.changed == 'true' || github.event_name == 'pull_request')
        with:
          name: "${{ matrix.module }} Results"
          path: "${{ matrix.module }}/build/test-results/test/*.xml"
          reporter: java-junit
```

### Developer Commands Quick Reference

```bash
# ── 單一模組測試 ──
./gradlew :scenario-s1-core:test              # 只跑 S1
./gradlew :scenario-s5-resilience:test        # 只跑 S5

# ── 全部模組測試 ──
./gradlew test                                 # 所有模組

# ── Phase 分組測試 ──
./gradlew :scenario-s1-core:test :scenario-s2-multistore:test   # Phase 1
./gradlew :scenario-s3-kafka:test :scenario-s4-cdc:test :scenario-s5-resilience:test  # Phase 2

# ── 建置不含測試 ──
./gradlew build -x test

# ── 查看模組相依 ──
./gradlew dependencies --configuration testRuntimeClasspath

# ── 清理全部 ──
./gradlew clean

# ── 單一模組產生覆蓋率報告 ──
./gradlew :scenario-s1-core:jacocoTestReport
```

---

## Key Design Decisions

| #   | Decision                                    | Rationale                                              | Alternatives Considered              |
|-----|---------------------------------------------|--------------------------------------------------------|--------------------------------------|
| D1  | Gradle Monorepo multi-module                | 依賴隔離、獨立建置、平行 CI                              | Maven multi-module, single project   |
| D2  | Version Catalog (`libs.versions.toml`)      | 集中版本管理、type-safe accessors                        | ext properties, BOM-only             |
| D3  | `tc-common` 使用 `java-library` plugin      | 僅作為 library 曝露容器定義，非 Spring Boot app          | Spring Boot module                   |
| D4  | Container Factory (static methods)          | 比 `@TestConfiguration` Bean 更靈活，支援 singleton/fresh | `@TestConfiguration` in tc-common    |
| D5  | 每場景獨立 `XXApplication.java`              | Spring Boot 各模組獨立啟動，classpath 完全隔離            | 共用單一 Application class            |
| D6  | `@ServiceConnection` 取代 `@DynamicPropertySource` | Spring Boot 3.1+ 原生支援，自動注入連線參數             | `@DynamicPropertySource`             |
| D7  | Singleton Container (`withReuse`)           | 開發階段加速，同 JVM 共享容器                            | Per-class fresh container            |
| D8  | Matrix CI with `dorny/paths-filter`         | 僅觸發變更模組的 pipeline，節省 CI 資源                   | 每模組獨立 workflow file              |
| D9  | `org.gradle.parallel=true`                  | Gradle 平行建置模組，加速本機 build                      | Sequential build                     |
| D10 | Kafka KRaft mode                            | 無 ZooKeeper 依賴，減少容器數                            | Kafka + ZooKeeper                    |
| D11 | `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` | `projects.tcCommon` 比 `project(":tc-common")` 更安全 | String-based project references      |

---

## Performance Considerations

| Metric                          | Single Module | All Modules (parallel) | Optimization                       |
|---------------------------------|---------------|------------------------|-------------------------------------|
| 首次容器啟動（Cold Start）        | ~5-25s        | ~30-60s                | Docker image 預先 pull               |
| 後續測試（Warm Container）        | ~1-3s/test    | ~1-3s/test             | Singleton Container Pattern         |
| 單模組測試套件                    | ≤ 60s         | —                      | 模組獨立、僅啟動所需容器              |
| 全模組測試套件                    | —             | ≤ 8 min                | `org.gradle.parallel=true`          |
| CI Matrix（8 parallel jobs）     | —             | ≤ 5 min                | Matrix + pre-pull + Gradle cache    |
| 本機記憶體（單模組）              | ~2-4 GB       | —                      | 僅啟動所需容器                       |
| 本機記憶體（全模組）              | —             | ~8-10 GB               | Sequential fallback for low-mem     |

---

## Container Resource Matrix

| Container              | Image                                      | RAM    | Startup | Modules                    |
|------------------------|--------------------------------------------|--------|---------|----------------------------|
| PostgreSQL 16          | postgres:16-alpine                         | 256 MB | ~3s     | s1-core, s2, s4-cdc, s6   |
| RabbitMQ 3.13          | rabbitmq:3.13-management-alpine            | 256 MB | ~5s     | s1-core                    |
| Redis 7                | redis:7-alpine                             | 64 MB  | ~1s     | s2-multistore              |
| Elasticsearch 8        | elasticsearch:8.13.0                       | 512 MB | ~15s    | s2-multistore              |
| Kafka (KRaft)          | confluentinc/cp-kafka:7.6.0               | 512 MB | ~10s    | s3-kafka, s4-cdc           |
| Schema Registry        | confluentinc/cp-schema-registry:7.6.0     | 256 MB | ~8s     | s3-kafka                   |
| Debezium Connect       | debezium/connect:2.6                       | 512 MB | ~12s    | s4-cdc                     |
| WireMock               | wiremock/wiremock:3.5.2                    | 128 MB | ~3s     | s5-resilience              |
| Toxiproxy              | ghcr.io/shopify/toxiproxy:2.9.0           | 32 MB  | ~1s     | s5-resilience              |
| Keycloak 24            | quay.io/keycloak/keycloak:24.0            | 512 MB | ~15s    | s6-security                |
| Vault                  | hashicorp/vault:1.16                       | 128 MB | ~2s     | s6-security                |
| LocalStack             | localstack/localstack:3.4                  | 512 MB | ~10s    | s7-cloud                   |
| Azurite                | mcr.microsoft.com/azure-storage/azurite    | 128 MB | ~3s     | s7-cloud                   |
| Pact Broker            | pactfoundation/pact-broker:latest          | 256 MB | ~8s     | s8-contract                |

---

## Security Notes

- 所有測試容器僅在本機或 CI 環境中執行，不暴露至外部網路
- 容器使用隨機動態 port，避免 port 衝突
- 測試完成後由 Ryuk sidecar 自動清理所有容器
- 敏感資訊（帳密、Token）僅存在於測試設定中，不進入正式設定檔
- Keycloak realm 使用測試專用帳號，與正式環境完全隔離
- Vault 使用 dev mode（test-root-token），不連接正式 Vault cluster
- LocalStack/Azurite 不連接真實雲端帳號，所有資料為測試模擬
- 每個模組有獨立的 `application-test.yml`，敏感設定不跨模組洩漏
