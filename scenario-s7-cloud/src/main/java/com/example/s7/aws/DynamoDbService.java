package com.example.s7.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for DynamoDB operations.
 * Provides CRUD and query operations for DynamoDB tables.
 */
@Service
public class DynamoDbService {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbService.class);

    private final DynamoDbClient dynamoDbClient;

    public DynamoDbService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Creates a table with a simple primary key.
     *
     * @param tableName the table name
     * @param partitionKeyName the partition key attribute name
     * @param partitionKeyType the partition key type (S, N, B)
     */
    public void createTable(String tableName, String partitionKeyName, ScalarAttributeType partitionKeyType) {
        if (tableExists(tableName)) {
            log.debug("Table {} already exists", tableName);
            return;
        }

        log.info("Creating table: {}", tableName);

        CreateTableRequest request = CreateTableRequest.builder()
            .tableName(tableName)
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName(partitionKeyName)
                    .keyType(KeyType.HASH)
                    .build()
            )
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName(partitionKeyName)
                    .attributeType(partitionKeyType)
                    .build()
            )
            .provisionedThroughput(
                ProvisionedThroughput.builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build()
            )
            .build();

        dynamoDbClient.createTable(request);
        waitForTableActive(tableName);
        log.debug("Table {} created successfully", tableName);
    }

    /**
     * Creates a table with partition and sort keys, plus a GSI.
     *
     * @param tableName the table name
     * @param partitionKeyName the partition key attribute name
     * @param sortKeyName the sort key attribute name
     * @param gsiName the global secondary index name
     * @param gsiPartitionKeyName the GSI partition key attribute name
     */
    public void createTableWithGsi(String tableName, String partitionKeyName, String sortKeyName,
                                   String gsiName, String gsiPartitionKeyName) {
        if (tableExists(tableName)) {
            log.debug("Table {} already exists", tableName);
            return;
        }

        log.info("Creating table with GSI: {}", tableName);

        CreateTableRequest request = CreateTableRequest.builder()
            .tableName(tableName)
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName(partitionKeyName)
                    .keyType(KeyType.HASH)
                    .build(),
                KeySchemaElement.builder()
                    .attributeName(sortKeyName)
                    .keyType(KeyType.RANGE)
                    .build()
            )
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName(partitionKeyName)
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName(sortKeyName)
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName(gsiPartitionKeyName)
                    .attributeType(ScalarAttributeType.S)
                    .build()
            )
            .globalSecondaryIndexes(
                GlobalSecondaryIndex.builder()
                    .indexName(gsiName)
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName(gsiPartitionKeyName)
                            .keyType(KeyType.HASH)
                            .build()
                    )
                    .projection(
                        Projection.builder()
                            .projectionType(ProjectionType.ALL)
                            .build()
                    )
                    .provisionedThroughput(
                        ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build()
                    )
                    .build()
            )
            .provisionedThroughput(
                ProvisionedThroughput.builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build()
            )
            .build();

        dynamoDbClient.createTable(request);
        waitForTableActive(tableName);
        log.debug("Table {} with GSI created successfully", tableName);
    }

    /**
     * Checks if a table exists.
     *
     * @param tableName the table name
     * @return true if the table exists
     */
    public boolean tableExists(String tableName) {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder()
                .tableName(tableName)
                .build());
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Waits for a table to become active.
     *
     * @param tableName the table name
     */
    private void waitForTableActive(String tableName) {
        int maxWaitSeconds = 60;
        int waited = 0;
        while (waited < maxWaitSeconds) {
            try {
                var response = dynamoDbClient.describeTable(DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build());
                if (response.table().tableStatus() == TableStatus.ACTIVE) {
                    return;
                }
            } catch (ResourceNotFoundException e) {
                // Table not yet created
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for table", e);
            }
            waited++;
        }
        throw new RuntimeException("Table " + tableName + " did not become active within " + maxWaitSeconds + " seconds");
    }

    /**
     * Puts an item into a table.
     *
     * @param tableName the table name
     * @param item the item attributes
     */
    public void putItem(String tableName, Map<String, AttributeValue> item) {
        log.debug("Putting item into table: {}", tableName);

        PutItemRequest request = PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .build();

        dynamoDbClient.putItem(request);
        log.debug("Item put successfully");
    }

    /**
     * Gets an item by its primary key.
     *
     * @param tableName the table name
     * @param key the primary key
     * @return the item, or empty if not found
     */
    public Optional<Map<String, AttributeValue>> getItem(String tableName, Map<String, AttributeValue> key) {
        log.debug("Getting item from table: {}", tableName);

        GetItemRequest request = GetItemRequest.builder()
            .tableName(tableName)
            .key(key)
            .build();

        GetItemResponse response = dynamoDbClient.getItem(request);
        if (response.hasItem() && !response.item().isEmpty()) {
            return Optional.of(response.item());
        }
        return Optional.empty();
    }

    /**
     * Deletes an item by its primary key.
     *
     * @param tableName the table name
     * @param key the primary key
     */
    public void deleteItem(String tableName, Map<String, AttributeValue> key) {
        log.debug("Deleting item from table: {}", tableName);

        DeleteItemRequest request = DeleteItemRequest.builder()
            .tableName(tableName)
            .key(key)
            .build();

        dynamoDbClient.deleteItem(request);
        log.debug("Item deleted successfully");
    }

    /**
     * Queries items by partition key.
     *
     * @param tableName the table name
     * @param partitionKeyName the partition key attribute name
     * @param partitionKeyValue the partition key value
     * @return the list of matching items
     */
    public List<Map<String, AttributeValue>> queryByPartitionKey(String tableName,
                                                                  String partitionKeyName,
                                                                  AttributeValue partitionKeyValue) {
        log.debug("Querying table {} by partition key: {}", tableName, partitionKeyName);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":pk", partitionKeyValue);

        QueryRequest request = QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression(partitionKeyName + " = :pk")
            .expressionAttributeValues(expressionValues)
            .build();

        List<Map<String, AttributeValue>> items = dynamoDbClient.query(request).items();
        log.debug("Query returned {} items", items.size());
        return items;
    }

    /**
     * Queries items using a global secondary index.
     *
     * @param tableName the table name
     * @param indexName the index name
     * @param partitionKeyName the GSI partition key attribute name
     * @param partitionKeyValue the partition key value
     * @return the list of matching items
     */
    public List<Map<String, AttributeValue>> queryByGsi(String tableName, String indexName,
                                                        String partitionKeyName,
                                                        AttributeValue partitionKeyValue) {
        log.debug("Querying table {} by GSI {}: {}", tableName, indexName, partitionKeyName);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":pk", partitionKeyValue);

        QueryRequest request = QueryRequest.builder()
            .tableName(tableName)
            .indexName(indexName)
            .keyConditionExpression(partitionKeyName + " = :pk")
            .expressionAttributeValues(expressionValues)
            .build();

        List<Map<String, AttributeValue>> items = dynamoDbClient.query(request).items();
        log.debug("GSI query returned {} items", items.size());
        return items;
    }

    /**
     * Scans all items in a table.
     *
     * @param tableName the table name
     * @return the list of all items
     */
    public List<Map<String, AttributeValue>> scanAll(String tableName) {
        log.debug("Scanning all items from table: {}", tableName);

        ScanRequest request = ScanRequest.builder()
            .tableName(tableName)
            .build();

        List<Map<String, AttributeValue>> items = dynamoDbClient.scan(request).items();
        log.debug("Scan returned {} items", items.size());
        return items;
    }
}
