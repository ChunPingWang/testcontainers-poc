package com.example.s7;

import com.example.s7.aws.DynamoDbService;
import com.example.s7.config.AwsConfig;
import com.example.tc.base.IntegrationTestBase;
import com.example.tc.containers.LocalStackContainerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DynamoDB operations using LocalStack.
 * Tests CRUD and query operations.
 *
 * Validates cloud service operations work identically to real AWS DynamoDB.
 */
@SpringBootTest(classes = {S7Application.class, AwsConfig.class, DynamoDbService.class})
@Testcontainers
@ActiveProfiles("test")
class LocalStackDynamoDbIT extends IntegrationTestBase {

    @Container
    static LocalStackContainer localStack = LocalStackContainerFactory.getInstance();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.endpoint", () -> localStack.getEndpoint().toString());
        registry.add("aws.region", () -> localStack.getRegion());
        registry.add("aws.access-key-id", () -> localStack.getAccessKey());
        registry.add("aws.secret-access-key", () -> localStack.getSecretKey());
    }

    @Autowired
    private DynamoDbService dynamoDbService;

    private String testTableName;

    @BeforeEach
    void setUp() {
        testTableName = "test-table-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void shouldCreateTableAndPutItem() {
        // Given
        dynamoDbService.createTable(testTableName, "id", ScalarAttributeType.S);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("item-1").build());
        item.put("name", AttributeValue.builder().s("Test Item").build());
        item.put("price", AttributeValue.builder().n("99.99").build());

        // When
        dynamoDbService.putItem(testTableName, item);

        // Then
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s("item-1").build());
        Optional<Map<String, AttributeValue>> retrieved = dynamoDbService.getItem(testTableName, key);

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().get("name").s()).isEqualTo("Test Item");
        assertThat(retrieved.get().get("price").n()).isEqualTo("99.99");
    }

    @Test
    void shouldReturnEmptyWhenItemNotFound() {
        // Given
        dynamoDbService.createTable(testTableName, "id", ScalarAttributeType.S);

        // When
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s("non-existent").build());
        Optional<Map<String, AttributeValue>> retrieved = dynamoDbService.getItem(testTableName, key);

        // Then
        assertThat(retrieved).isEmpty();
    }

    @Test
    void shouldDeleteItem() {
        // Given
        dynamoDbService.createTable(testTableName, "id", ScalarAttributeType.S);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("item-to-delete").build());
        item.put("name", AttributeValue.builder().s("Delete Me").build());
        dynamoDbService.putItem(testTableName, item);

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s("item-to-delete").build());

        // Verify item exists
        assertThat(dynamoDbService.getItem(testTableName, key)).isPresent();

        // When
        dynamoDbService.deleteItem(testTableName, key);

        // Then
        assertThat(dynamoDbService.getItem(testTableName, key)).isEmpty();
    }

    @Test
    void shouldUpdateItem() {
        // Given
        dynamoDbService.createTable(testTableName, "id", ScalarAttributeType.S);

        Map<String, AttributeValue> originalItem = new HashMap<>();
        originalItem.put("id", AttributeValue.builder().s("item-1").build());
        originalItem.put("name", AttributeValue.builder().s("Original Name").build());
        originalItem.put("status", AttributeValue.builder().s("PENDING").build());
        dynamoDbService.putItem(testTableName, originalItem);

        // When - update by putting item with same key
        Map<String, AttributeValue> updatedItem = new HashMap<>();
        updatedItem.put("id", AttributeValue.builder().s("item-1").build());
        updatedItem.put("name", AttributeValue.builder().s("Updated Name").build());
        updatedItem.put("status", AttributeValue.builder().s("COMPLETED").build());
        dynamoDbService.putItem(testTableName, updatedItem);

        // Then
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s("item-1").build());
        Optional<Map<String, AttributeValue>> retrieved = dynamoDbService.getItem(testTableName, key);

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().get("name").s()).isEqualTo("Updated Name");
        assertThat(retrieved.get().get("status").s()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldScanAllItems() {
        // Given
        dynamoDbService.createTable(testTableName, "id", ScalarAttributeType.S);

        for (int i = 1; i <= 5; i++) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s("item-" + i).build());
            item.put("name", AttributeValue.builder().s("Item " + i).build());
            dynamoDbService.putItem(testTableName, item);
        }

        // When
        List<Map<String, AttributeValue>> items = dynamoDbService.scanAll(testTableName);

        // Then
        assertThat(items).hasSize(5);
    }

    @Test
    void shouldQueryByPartitionKey() {
        // Given - create table with partition key only
        String ordersTable = "orders-" + UUID.randomUUID().toString().substring(0, 8);
        dynamoDbService.createTable(ordersTable, "orderId", ScalarAttributeType.S);

        // Put multiple items
        for (int i = 1; i <= 3; i++) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("orderId", AttributeValue.builder().s("order-" + i).build());
            item.put("status", AttributeValue.builder().s("PENDING").build());
            dynamoDbService.putItem(ordersTable, item);
        }

        // When
        List<Map<String, AttributeValue>> results = dynamoDbService.queryByPartitionKey(
            ordersTable,
            "orderId",
            AttributeValue.builder().s("order-1").build()
        );

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("orderId").s()).isEqualTo("order-1");
    }

    @Test
    void shouldCreateTableWithGsiAndQuery() {
        // Given - create table with GSI
        String ordersTable = "orders-gsi-" + UUID.randomUUID().toString().substring(0, 8);
        dynamoDbService.createTableWithGsi(
            ordersTable,
            "customerId",
            "orderId",
            "StatusIndex",
            "status"
        );

        // Put items with different statuses
        Map<String, AttributeValue> order1 = new HashMap<>();
        order1.put("customerId", AttributeValue.builder().s("customer-1").build());
        order1.put("orderId", AttributeValue.builder().s("order-1").build());
        order1.put("status", AttributeValue.builder().s("PENDING").build());
        order1.put("amount", AttributeValue.builder().n("100.00").build());
        dynamoDbService.putItem(ordersTable, order1);

        Map<String, AttributeValue> order2 = new HashMap<>();
        order2.put("customerId", AttributeValue.builder().s("customer-1").build());
        order2.put("orderId", AttributeValue.builder().s("order-2").build());
        order2.put("status", AttributeValue.builder().s("COMPLETED").build());
        order2.put("amount", AttributeValue.builder().n("200.00").build());
        dynamoDbService.putItem(ordersTable, order2);

        Map<String, AttributeValue> order3 = new HashMap<>();
        order3.put("customerId", AttributeValue.builder().s("customer-2").build());
        order3.put("orderId", AttributeValue.builder().s("order-3").build());
        order3.put("status", AttributeValue.builder().s("PENDING").build());
        order3.put("amount", AttributeValue.builder().n("150.00").build());
        dynamoDbService.putItem(ordersTable, order3);

        // When - query by GSI (status)
        List<Map<String, AttributeValue>> pendingOrders = dynamoDbService.queryByGsi(
            ordersTable,
            "StatusIndex",
            "status",
            AttributeValue.builder().s("PENDING").build()
        );

        // Then
        assertThat(pendingOrders).hasSize(2);
        assertThat(pendingOrders).allMatch(item -> item.get("status").s().equals("PENDING"));
    }

    @Test
    void shouldStoreComplexObject() {
        // Given
        dynamoDbService.createTable(testTableName, "id", ScalarAttributeType.S);

        Map<String, AttributeValue> address = new HashMap<>();
        address.put("street", AttributeValue.builder().s("123 Main St").build());
        address.put("city", AttributeValue.builder().s("Test City").build());
        address.put("zip", AttributeValue.builder().s("12345").build());

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("user-1").build());
        item.put("name", AttributeValue.builder().s("John Doe").build());
        item.put("age", AttributeValue.builder().n("30").build());
        item.put("active", AttributeValue.builder().bool(true).build());
        item.put("tags", AttributeValue.builder().ss("premium", "verified").build());
        item.put("address", AttributeValue.builder().m(address).build());

        // When
        dynamoDbService.putItem(testTableName, item);

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s("user-1").build());
        Optional<Map<String, AttributeValue>> retrieved = dynamoDbService.getItem(testTableName, key);

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().get("name").s()).isEqualTo("John Doe");
        assertThat(retrieved.get().get("age").n()).isEqualTo("30");
        assertThat(retrieved.get().get("active").bool()).isTrue();
        assertThat(retrieved.get().get("tags").ss()).containsExactlyInAnyOrder("premium", "verified");
        assertThat(retrieved.get().get("address").m().get("city").s()).isEqualTo("Test City");
    }

    @Test
    void shouldHandleNumericPrimaryKey() {
        // Given
        String numericTable = "numeric-pk-" + UUID.randomUUID().toString().substring(0, 8);
        dynamoDbService.createTable(numericTable, "id", ScalarAttributeType.N);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().n("12345").build());
        item.put("data", AttributeValue.builder().s("Test data").build());

        // When
        dynamoDbService.putItem(numericTable, item);

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().n("12345").build());
        Optional<Map<String, AttributeValue>> retrieved = dynamoDbService.getItem(numericTable, key);

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().get("id").n()).isEqualTo("12345");
        assertThat(retrieved.get().get("data").s()).isEqualTo("Test data");
    }

    @Test
    void shouldVerifyTableExists() {
        // Given
        dynamoDbService.createTable(testTableName, "id", ScalarAttributeType.S);

        // When/Then
        assertThat(dynamoDbService.tableExists(testTableName)).isTrue();
        assertThat(dynamoDbService.tableExists("non-existent-table")).isFalse();
    }

    @Test
    void shouldNotRecreateExistingTable() {
        // Given
        dynamoDbService.createTable(testTableName, "id", ScalarAttributeType.S);

        // When - try to create again (should not throw)
        dynamoDbService.createTable(testTableName, "id", ScalarAttributeType.S);

        // Then
        assertThat(dynamoDbService.tableExists(testTableName)).isTrue();
    }
}
