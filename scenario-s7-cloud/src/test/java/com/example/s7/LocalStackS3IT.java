package com.example.s7;

import com.example.s7.aws.S3FileService;
import com.example.s7.config.AwsConfig;
import com.example.tc.base.IntegrationTestBase;
import com.example.tc.containers.LocalStackContainerFactory;
import org.junit.jupiter.api.BeforeAll;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for S3 file operations using LocalStack.
 * Tests upload, download, delete, and list operations.
 *
 * Validates cloud service operations work identically to real AWS S3.
 */
@SpringBootTest(classes = {S7Application.class, AwsConfig.class, S3FileService.class})
@Testcontainers
@ActiveProfiles("test")
class LocalStackS3IT extends IntegrationTestBase {

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
    private S3FileService s3FileService;

    private String testBucketName;

    @BeforeEach
    void setUp() {
        testBucketName = "test-bucket-" + UUID.randomUUID().toString().substring(0, 8);
        s3FileService.createBucketIfNotExists(testBucketName);
    }

    @Test
    void shouldUploadAndDownloadFile() {
        // Given
        String key = "test-file.txt";
        byte[] content = "Hello, S3!".getBytes(StandardCharsets.UTF_8);
        String contentType = "text/plain";

        // When
        s3FileService.upload(testBucketName, key, content, contentType);
        Optional<byte[]> downloaded = s3FileService.download(testBucketName, key);

        // Then
        assertThat(downloaded).isPresent();
        assertThat(new String(downloaded.get(), StandardCharsets.UTF_8)).isEqualTo("Hello, S3!");
    }

    @Test
    void shouldUploadAndDownloadJsonFile() {
        // Given
        String key = "data/config.json";
        String jsonContent = "{\"name\":\"test\",\"value\":123}";
        byte[] content = jsonContent.getBytes(StandardCharsets.UTF_8);
        String contentType = "application/json";

        // When
        s3FileService.upload(testBucketName, key, content, contentType);
        Optional<byte[]> downloaded = s3FileService.download(testBucketName, key);

        // Then
        assertThat(downloaded).isPresent();
        assertThat(new String(downloaded.get(), StandardCharsets.UTF_8)).isEqualTo(jsonContent);
    }

    @Test
    void shouldReturnEmptyWhenDownloadingNonExistentFile() {
        // Given
        String key = "non-existent-file.txt";

        // When
        Optional<byte[]> downloaded = s3FileService.download(testBucketName, key);

        // Then
        assertThat(downloaded).isEmpty();
    }

    @Test
    void shouldDeleteExistingFile() {
        // Given
        String key = "file-to-delete.txt";
        byte[] content = "Delete me".getBytes(StandardCharsets.UTF_8);
        s3FileService.upload(testBucketName, key, content, "text/plain");
        assertThat(s3FileService.exists(testBucketName, key)).isTrue();

        // When
        boolean deleted = s3FileService.delete(testBucketName, key);

        // Then
        assertThat(deleted).isTrue();
        assertThat(s3FileService.exists(testBucketName, key)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentFile() {
        // Given
        String key = "non-existent-file.txt";

        // When
        boolean deleted = s3FileService.delete(testBucketName, key);

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    void shouldListAllObjectsInBucket() {
        // Given
        s3FileService.upload(testBucketName, "file1.txt", "content1".getBytes(), "text/plain");
        s3FileService.upload(testBucketName, "file2.txt", "content2".getBytes(), "text/plain");
        s3FileService.upload(testBucketName, "file3.txt", "content3".getBytes(), "text/plain");

        // When
        List<String> keys = s3FileService.listObjects(testBucketName);

        // Then
        assertThat(keys).hasSize(3);
        assertThat(keys).containsExactlyInAnyOrder("file1.txt", "file2.txt", "file3.txt");
    }

    @Test
    void shouldListObjectsWithPrefix() {
        // Given
        s3FileService.upload(testBucketName, "documents/doc1.pdf", "doc1".getBytes(), "application/pdf");
        s3FileService.upload(testBucketName, "documents/doc2.pdf", "doc2".getBytes(), "application/pdf");
        s3FileService.upload(testBucketName, "images/img1.png", "img1".getBytes(), "image/png");

        // When
        List<String> documentKeys = s3FileService.listObjectsWithPrefix(testBucketName, "documents/");
        List<String> imageKeys = s3FileService.listObjectsWithPrefix(testBucketName, "images/");

        // Then
        assertThat(documentKeys).hasSize(2);
        assertThat(documentKeys).containsExactlyInAnyOrder("documents/doc1.pdf", "documents/doc2.pdf");
        assertThat(imageKeys).hasSize(1);
        assertThat(imageKeys).contains("images/img1.png");
    }

    @Test
    void shouldCheckIfObjectExists() {
        // Given
        String existingKey = "existing-file.txt";
        String nonExistingKey = "non-existing-file.txt";
        s3FileService.upload(testBucketName, existingKey, "content".getBytes(), "text/plain");

        // When/Then
        assertThat(s3FileService.exists(testBucketName, existingKey)).isTrue();
        assertThat(s3FileService.exists(testBucketName, nonExistingKey)).isFalse();
    }

    @Test
    void shouldHandleLargeFile() {
        // Given
        String key = "large-file.bin";
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }

        // When
        s3FileService.upload(testBucketName, key, largeContent, "application/octet-stream");
        Optional<byte[]> downloaded = s3FileService.download(testBucketName, key);

        // Then
        assertThat(downloaded).isPresent();
        assertThat(downloaded.get()).hasSize(largeContent.length);
        assertThat(downloaded.get()).isEqualTo(largeContent);
    }

    @Test
    void shouldHandleSpecialCharactersInKey() {
        // Given
        String key = "folder/sub folder/file with spaces.txt";
        byte[] content = "special content".getBytes(StandardCharsets.UTF_8);

        // When
        s3FileService.upload(testBucketName, key, content, "text/plain");
        Optional<byte[]> downloaded = s3FileService.download(testBucketName, key);

        // Then
        assertThat(downloaded).isPresent();
        assertThat(new String(downloaded.get(), StandardCharsets.UTF_8)).isEqualTo("special content");
    }

    @Test
    void shouldOverwriteExistingFile() {
        // Given
        String key = "overwrite-test.txt";
        byte[] originalContent = "Original content".getBytes(StandardCharsets.UTF_8);
        byte[] newContent = "New content".getBytes(StandardCharsets.UTF_8);
        s3FileService.upload(testBucketName, key, originalContent, "text/plain");

        // When
        s3FileService.upload(testBucketName, key, newContent, "text/plain");
        Optional<byte[]> downloaded = s3FileService.download(testBucketName, key);

        // Then
        assertThat(downloaded).isPresent();
        assertThat(new String(downloaded.get(), StandardCharsets.UTF_8)).isEqualTo("New content");
    }
}
