package com.example.s7;

import com.example.s7.aws.SqsMessageService;
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
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for SQS message operations using LocalStack.
 * Tests send, receive, and dead-letter queue operations.
 *
 * Validates cloud service operations work identically to real AWS SQS.
 */
@SpringBootTest(classes = {S7Application.class, AwsConfig.class, SqsMessageService.class})
@Testcontainers
@ActiveProfiles("test")
class LocalStackSqsIT extends IntegrationTestBase {

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
    private SqsMessageService sqsMessageService;

    private String testQueueName;
    private String testQueueUrl;

    @BeforeEach
    void setUp() {
        testQueueName = "test-queue-" + UUID.randomUUID().toString().substring(0, 8);
        testQueueUrl = sqsMessageService.createQueueIfNotExists(testQueueName);
    }

    @Test
    void shouldSendAndReceiveMessage() {
        // Given
        String messageBody = "Hello, SQS!";

        // When
        String messageId = sqsMessageService.sendMessage(testQueueUrl, messageBody);
        Optional<Message> received = sqsMessageService.receiveMessage(testQueueUrl, 5);

        // Then
        assertThat(messageId).isNotEmpty();
        assertThat(received).isPresent();
        assertThat(received.get().body()).isEqualTo(messageBody);
    }

    @Test
    void shouldSendAndReceiveJsonMessage() {
        // Given
        String jsonMessage = "{\"orderId\":\"12345\",\"status\":\"PENDING\",\"amount\":99.99}";

        // When
        String messageId = sqsMessageService.sendMessage(testQueueUrl, jsonMessage);
        Optional<Message> received = sqsMessageService.receiveMessage(testQueueUrl, 5);

        // Then
        assertThat(messageId).isNotEmpty();
        assertThat(received).isPresent();
        assertThat(received.get().body()).isEqualTo(jsonMessage);
    }

    @Test
    void shouldReturnEmptyWhenNoMessagesAvailable() {
        // When - queue is empty, short poll
        Optional<Message> received = sqsMessageService.receiveMessage(testQueueUrl, 0);

        // Then
        assertThat(received).isEmpty();
    }

    @Test
    void shouldReceiveMultipleMessages() {
        // Given
        for (int i = 1; i <= 5; i++) {
            sqsMessageService.sendMessage(testQueueUrl, "Message " + i);
        }

        // When - receive in batches
        List<Message> allMessages = new ArrayList<>();
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            List<Message> batch = sqsMessageService.receiveMessages(testQueueUrl, 10, 1);
            allMessages.addAll(batch);
            // Delete received messages
            for (Message msg : batch) {
                sqsMessageService.deleteMessage(testQueueUrl, msg.receiptHandle());
            }
            return allMessages.size() >= 5;
        });

        // Then
        assertThat(allMessages).hasSize(5);
    }

    @Test
    void shouldDeleteMessage() {
        // Given
        String messageBody = "Message to delete";
        sqsMessageService.sendMessage(testQueueUrl, messageBody);
        Optional<Message> received = sqsMessageService.receiveMessage(testQueueUrl, 5);
        assertThat(received).isPresent();

        // When
        sqsMessageService.deleteMessage(testQueueUrl, received.get().receiptHandle());

        // Wait a moment for delete to take effect
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - message should not be available again
        Optional<Message> afterDelete = sqsMessageService.receiveMessage(testQueueUrl, 1);
        assertThat(afterDelete).isEmpty();
    }

    @Test
    void shouldGetApproximateMessageCount() {
        // Given - send multiple messages
        for (int i = 0; i < 3; i++) {
            sqsMessageService.sendMessage(testQueueUrl, "Message " + i);
        }

        // Wait for messages to be available
        await().atMost(5, TimeUnit.SECONDS).until(() ->
            sqsMessageService.getApproximateMessageCount(testQueueUrl) >= 3
        );

        // When
        int count = sqsMessageService.getApproximateMessageCount(testQueueUrl);

        // Then
        assertThat(count).isGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldCreateQueueWithDeadLetterQueue() {
        // Given
        String mainQueueName = "main-queue-" + UUID.randomUUID().toString().substring(0, 8);
        String dlqName = "dlq-" + UUID.randomUUID().toString().substring(0, 8);
        int maxReceiveCount = 3;

        // When
        String mainQueueUrl = sqsMessageService.createQueueWithDlq(mainQueueName, dlqName, maxReceiveCount);

        // Then
        assertThat(mainQueueUrl).isNotEmpty();
        assertThat(mainQueueUrl).contains(mainQueueName);

        // Verify DLQ was created
        String dlqUrl = sqsMessageService.getQueueUrl(dlqName);
        assertThat(dlqUrl).isNotEmpty();
        assertThat(dlqUrl).contains(dlqName);
    }

    @Test
    void shouldSendDelayedMessage() {
        // Given
        String messageBody = "Delayed message";
        int delaySeconds = 2;

        // When
        long startTime = System.currentTimeMillis();
        String messageId = sqsMessageService.sendMessageWithDelay(testQueueUrl, messageBody, delaySeconds);

        // Immediately try to receive - should not be available
        Optional<Message> immediateReceive = sqsMessageService.receiveMessage(testQueueUrl, 0);
        assertThat(immediateReceive).isEmpty();

        // Wait for delay to expire and receive
        await().atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> sqsMessageService.receiveMessage(testQueueUrl, 1).isPresent());

        // Then
        long elapsedTime = System.currentTimeMillis() - startTime;
        assertThat(messageId).isNotEmpty();
        assertThat(elapsedTime).isGreaterThanOrEqualTo(delaySeconds * 1000L - 500); // Allow some tolerance
    }

    @Test
    void shouldProcessMessageAndAcknowledge() {
        // Given
        String messageBody = "{\"action\":\"process\",\"data\":\"test-data\"}";
        sqsMessageService.sendMessage(testQueueUrl, messageBody);

        // When - simulate message processing
        Optional<Message> received = sqsMessageService.receiveMessage(testQueueUrl, 5);
        assertThat(received).isPresent();

        // Process the message (simulated)
        String processedBody = received.get().body();
        assertThat(processedBody).contains("process");

        // Acknowledge by deleting
        sqsMessageService.deleteMessage(testQueueUrl, received.get().receiptHandle());

        // Then - verify message count decreased
        await().atMost(5, TimeUnit.SECONDS).until(() ->
            sqsMessageService.getApproximateMessageCount(testQueueUrl) == 0
        );
    }

    @Test
    void shouldHandleFifoLikeOrdering() {
        // Given - send ordered messages
        for (int i = 1; i <= 10; i++) {
            sqsMessageService.sendMessage(testQueueUrl, "Order-" + String.format("%02d", i));
        }

        // When - receive all messages
        List<String> receivedBodies = new ArrayList<>();
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            List<Message> batch = sqsMessageService.receiveMessages(testQueueUrl, 10, 1);
            for (Message msg : batch) {
                receivedBodies.add(msg.body());
                sqsMessageService.deleteMessage(testQueueUrl, msg.receiptHandle());
            }
            return receivedBodies.size() >= 10;
        });

        // Then - all messages should be received (order not guaranteed in standard queue)
        assertThat(receivedBodies).hasSize(10);
        assertThat(receivedBodies).allMatch(body -> body.startsWith("Order-"));
    }
}
