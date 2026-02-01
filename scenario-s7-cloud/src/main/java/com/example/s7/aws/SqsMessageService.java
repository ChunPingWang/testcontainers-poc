package com.example.s7.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for SQS message operations.
 * Provides send, receive, and dead-letter queue operations.
 */
@Service
public class SqsMessageService {

    private static final Logger log = LoggerFactory.getLogger(SqsMessageService.class);

    private final SqsClient sqsClient;

    public SqsMessageService(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    /**
     * Creates a queue if it doesn't exist.
     *
     * @param queueName the queue name
     * @return the queue URL
     */
    public String createQueueIfNotExists(String queueName) {
        try {
            return getQueueUrl(queueName);
        } catch (QueueDoesNotExistException e) {
            log.info("Creating queue: {}", queueName);
            CreateQueueRequest request = CreateQueueRequest.builder()
                .queueName(queueName)
                .build();
            return sqsClient.createQueue(request).queueUrl();
        }
    }

    /**
     * Creates a queue with a dead-letter queue.
     *
     * @param queueName the main queue name
     * @param dlqName the dead-letter queue name
     * @param maxReceiveCount the maximum receive count before moving to DLQ
     * @return the main queue URL
     */
    public String createQueueWithDlq(String queueName, String dlqName, int maxReceiveCount) {
        // Create DLQ first
        String dlqUrl = createQueueIfNotExists(dlqName);
        String dlqArn = getQueueArn(dlqUrl);

        log.info("Creating queue {} with DLQ {}", queueName, dlqName);

        // Create main queue with redrive policy
        String redrivePolicy = String.format(
            "{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":\"%d\"}",
            dlqArn, maxReceiveCount
        );

        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.REDRIVE_POLICY, redrivePolicy);

        CreateQueueRequest request = CreateQueueRequest.builder()
            .queueName(queueName)
            .attributes(attributes)
            .build();

        return sqsClient.createQueue(request).queueUrl();
    }

    /**
     * Gets the queue URL.
     *
     * @param queueName the queue name
     * @return the queue URL
     */
    public String getQueueUrl(String queueName) {
        GetQueueUrlRequest request = GetQueueUrlRequest.builder()
            .queueName(queueName)
            .build();
        return sqsClient.getQueueUrl(request).queueUrl();
    }

    /**
     * Gets the queue ARN.
     *
     * @param queueUrl the queue URL
     * @return the queue ARN
     */
    public String getQueueArn(String queueUrl) {
        GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
            .queueUrl(queueUrl)
            .attributeNames(QueueAttributeName.QUEUE_ARN)
            .build();
        return sqsClient.getQueueAttributes(request)
            .attributes()
            .get(QueueAttributeName.QUEUE_ARN);
    }

    /**
     * Sends a message to a queue.
     *
     * @param queueUrl the queue URL
     * @param messageBody the message body
     * @return the message ID
     */
    public String sendMessage(String queueUrl, String messageBody) {
        log.info("Sending message to queue: {}", queueUrl);

        SendMessageRequest request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(messageBody)
            .build();

        SendMessageResponse response = sqsClient.sendMessage(request);
        log.debug("Message sent with ID: {}", response.messageId());
        return response.messageId();
    }

    /**
     * Sends a message with a delay.
     *
     * @param queueUrl the queue URL
     * @param messageBody the message body
     * @param delaySeconds the delay in seconds (0-900)
     * @return the message ID
     */
    public String sendMessageWithDelay(String queueUrl, String messageBody, int delaySeconds) {
        log.info("Sending delayed message to queue: {} (delay: {}s)", queueUrl, delaySeconds);

        SendMessageRequest request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(messageBody)
            .delaySeconds(delaySeconds)
            .build();

        SendMessageResponse response = sqsClient.sendMessage(request);
        log.debug("Delayed message sent with ID: {}", response.messageId());
        return response.messageId();
    }

    /**
     * Receives messages from a queue.
     *
     * @param queueUrl the queue URL
     * @param maxMessages the maximum number of messages to receive (1-10)
     * @param waitTimeSeconds the long polling wait time (0-20)
     * @return the list of messages
     */
    public List<Message> receiveMessages(String queueUrl, int maxMessages, int waitTimeSeconds) {
        log.info("Receiving messages from queue: {} (max: {}, wait: {}s)",
            queueUrl, maxMessages, waitTimeSeconds);

        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(maxMessages)
            .waitTimeSeconds(waitTimeSeconds)
            .attributeNamesWithStrings("All")
            .messageAttributeNames("All")
            .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();
        log.debug("Received {} messages", messages.size());
        return messages;
    }

    /**
     * Receives a single message from a queue.
     *
     * @param queueUrl the queue URL
     * @param waitTimeSeconds the long polling wait time
     * @return the message, or empty if no message available
     */
    public Optional<Message> receiveMessage(String queueUrl, int waitTimeSeconds) {
        List<Message> messages = receiveMessages(queueUrl, 1, waitTimeSeconds);
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }

    /**
     * Deletes a message from the queue.
     *
     * @param queueUrl the queue URL
     * @param receiptHandle the message receipt handle
     */
    public void deleteMessage(String queueUrl, String receiptHandle) {
        log.debug("Deleting message from queue: {}", queueUrl);

        DeleteMessageRequest request = DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(receiptHandle)
            .build();

        sqsClient.deleteMessage(request);
        log.debug("Message deleted successfully");
    }

    /**
     * Gets the approximate number of messages in a queue.
     *
     * @param queueUrl the queue URL
     * @return the approximate message count
     */
    public int getApproximateMessageCount(String queueUrl) {
        GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
            .queueUrl(queueUrl)
            .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
            .build();

        String count = sqsClient.getQueueAttributes(request)
            .attributes()
            .get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);

        return Integer.parseInt(count);
    }

    /**
     * Gets the approximate number of messages in flight (received but not deleted).
     *
     * @param queueUrl the queue URL
     * @return the approximate in-flight message count
     */
    public int getApproximateMessagesInFlight(String queueUrl) {
        GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
            .queueUrl(queueUrl)
            .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)
            .build();

        String count = sqsClient.getQueueAttributes(request)
            .attributes()
            .get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE);

        return Integer.parseInt(count);
    }
}
