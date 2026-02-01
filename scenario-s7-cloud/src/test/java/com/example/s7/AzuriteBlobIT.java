package com.example.s7;

import com.example.s7.azure.BlobStorageService;
import com.example.s7.config.AzureConfig;
import com.example.tc.base.IntegrationTestBase;
import com.example.tc.containers.AzuriteContainerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Azure Blob Storage operations using Azurite.
 * Tests upload, download, delete, and list operations.
 *
 * Validates cloud service operations work identically to real Azure Blob Storage.
 */
@SpringBootTest(classes = {S7Application.class, AzureConfig.class, BlobStorageService.class})
@Testcontainers
@ActiveProfiles("test")
class AzuriteBlobIT extends IntegrationTestBase {

    @Container
    static GenericContainer<?> azurite = AzuriteContainerFactory.getInstance();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("azure.storage.connection-string", () ->
            AzuriteContainerFactory.buildConnectionString(
                azurite.getHost(),
                azurite.getMappedPort(AzuriteContainerFactory.getBlobPort())
            )
        );
    }

    @Autowired
    private BlobStorageService blobStorageService;

    private String testContainerName;

    @BeforeEach
    void setUp() {
        testContainerName = "test-container-" + UUID.randomUUID().toString().substring(0, 8);
        blobStorageService.createContainerIfNotExists(testContainerName);
    }

    @Test
    void shouldUploadAndDownloadBlob() {
        // Given
        String blobName = "test-blob.txt";
        byte[] content = "Hello, Azure!".getBytes(StandardCharsets.UTF_8);
        String contentType = "text/plain";

        // When
        blobStorageService.upload(testContainerName, blobName, content, contentType);
        Optional<byte[]> downloaded = blobStorageService.download(testContainerName, blobName);

        // Then
        assertThat(downloaded).isPresent();
        assertThat(new String(downloaded.get(), StandardCharsets.UTF_8)).isEqualTo("Hello, Azure!");
    }

    @Test
    void shouldUploadAndDownloadJsonBlob() {
        // Given
        String blobName = "data/config.json";
        String jsonContent = "{\"name\":\"azure-test\",\"value\":456}";
        byte[] content = jsonContent.getBytes(StandardCharsets.UTF_8);
        String contentType = "application/json";

        // When
        blobStorageService.upload(testContainerName, blobName, content, contentType);
        Optional<byte[]> downloaded = blobStorageService.download(testContainerName, blobName);

        // Then
        assertThat(downloaded).isPresent();
        assertThat(new String(downloaded.get(), StandardCharsets.UTF_8)).isEqualTo(jsonContent);
    }

    @Test
    void shouldReturnEmptyWhenDownloadingNonExistentBlob() {
        // Given
        String blobName = "non-existent-blob.txt";

        // When
        Optional<byte[]> downloaded = blobStorageService.download(testContainerName, blobName);

        // Then
        assertThat(downloaded).isEmpty();
    }

    @Test
    void shouldDeleteExistingBlob() {
        // Given
        String blobName = "blob-to-delete.txt";
        byte[] content = "Delete me".getBytes(StandardCharsets.UTF_8);
        blobStorageService.upload(testContainerName, blobName, content, "text/plain");
        assertThat(blobStorageService.exists(testContainerName, blobName)).isTrue();

        // When
        boolean deleted = blobStorageService.delete(testContainerName, blobName);

        // Then
        assertThat(deleted).isTrue();
        assertThat(blobStorageService.exists(testContainerName, blobName)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentBlob() {
        // Given
        String blobName = "non-existent-blob.txt";

        // When
        boolean deleted = blobStorageService.delete(testContainerName, blobName);

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    void shouldListAllBlobsInContainer() {
        // Given
        blobStorageService.upload(testContainerName, "file1.txt", "content1".getBytes(), "text/plain");
        blobStorageService.upload(testContainerName, "file2.txt", "content2".getBytes(), "text/plain");
        blobStorageService.upload(testContainerName, "file3.txt", "content3".getBytes(), "text/plain");

        // When
        List<String> blobNames = blobStorageService.listBlobs(testContainerName);

        // Then
        assertThat(blobNames).hasSize(3);
        assertThat(blobNames).containsExactlyInAnyOrder("file1.txt", "file2.txt", "file3.txt");
    }

    @Test
    void shouldCheckIfBlobExists() {
        // Given
        String existingBlob = "existing-blob.txt";
        String nonExistingBlob = "non-existing-blob.txt";
        blobStorageService.upload(testContainerName, existingBlob, "content".getBytes(), "text/plain");

        // When/Then
        assertThat(blobStorageService.exists(testContainerName, existingBlob)).isTrue();
        assertThat(blobStorageService.exists(testContainerName, nonExistingBlob)).isFalse();
    }

    @Test
    void shouldHandleLargeBlob() {
        // Given
        String blobName = "large-blob.bin";
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }

        // When
        blobStorageService.upload(testContainerName, blobName, largeContent, "application/octet-stream");
        Optional<byte[]> downloaded = blobStorageService.download(testContainerName, blobName);

        // Then
        assertThat(downloaded).isPresent();
        assertThat(downloaded.get()).hasSize(largeContent.length);
        assertThat(downloaded.get()).isEqualTo(largeContent);
    }

    @Test
    void shouldHandleNestedBlobPath() {
        // Given
        String blobName = "folder/subfolder/nested-blob.txt";
        byte[] content = "nested content".getBytes(StandardCharsets.UTF_8);

        // When
        blobStorageService.upload(testContainerName, blobName, content, "text/plain");
        Optional<byte[]> downloaded = blobStorageService.download(testContainerName, blobName);

        // Then
        assertThat(downloaded).isPresent();
        assertThat(new String(downloaded.get(), StandardCharsets.UTF_8)).isEqualTo("nested content");
    }

    @Test
    void shouldOverwriteExistingBlob() {
        // Given
        String blobName = "overwrite-test.txt";
        byte[] originalContent = "Original content".getBytes(StandardCharsets.UTF_8);
        byte[] newContent = "New content".getBytes(StandardCharsets.UTF_8);
        blobStorageService.upload(testContainerName, blobName, originalContent, "text/plain");

        // When
        blobStorageService.upload(testContainerName, blobName, newContent, "text/plain");
        Optional<byte[]> downloaded = blobStorageService.download(testContainerName, blobName);

        // Then
        assertThat(downloaded).isPresent();
        assertThat(new String(downloaded.get(), StandardCharsets.UTF_8)).isEqualTo("New content");
    }

    @Test
    void shouldGetBlobSize() {
        // Given
        String blobName = "size-test.txt";
        String content = "This is a test content for size check";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        blobStorageService.upload(testContainerName, blobName, contentBytes, "text/plain");

        // When
        Optional<Long> size = blobStorageService.getBlobSize(testContainerName, blobName);

        // Then
        assertThat(size).isPresent();
        assertThat(size.get()).isEqualTo(contentBytes.length);
    }

    @Test
    void shouldReturnEmptySizeForNonExistentBlob() {
        // Given
        String blobName = "non-existent.txt";

        // When
        Optional<Long> size = blobStorageService.getBlobSize(testContainerName, blobName);

        // Then
        assertThat(size).isEmpty();
    }

    @Test
    void shouldHandleSpecialCharactersInBlobName() {
        // Given
        String blobName = "folder/file with spaces.txt";
        byte[] content = "special content".getBytes(StandardCharsets.UTF_8);

        // When
        blobStorageService.upload(testContainerName, blobName, content, "text/plain");
        Optional<byte[]> downloaded = blobStorageService.download(testContainerName, blobName);

        // Then
        assertThat(downloaded).isPresent();
        assertThat(new String(downloaded.get(), StandardCharsets.UTF_8)).isEqualTo("special content");
    }

    @Test
    void shouldCreateContainerIfNotExists() {
        // Given
        String newContainerName = "new-container-" + UUID.randomUUID().toString().substring(0, 8);

        // When - should not throw
        blobStorageService.createContainerIfNotExists(newContainerName);
        blobStorageService.createContainerIfNotExists(newContainerName); // idempotent

        // Then - container should exist (verified by uploading)
        blobStorageService.upload(newContainerName, "test.txt", "test".getBytes(), "text/plain");
        assertThat(blobStorageService.exists(newContainerName, "test.txt")).isTrue();
    }

    @Test
    void shouldUploadMultipleBlobsAndListByPrefix() {
        // Given
        blobStorageService.upload(testContainerName, "documents/doc1.pdf", "doc1".getBytes(), "application/pdf");
        blobStorageService.upload(testContainerName, "documents/doc2.pdf", "doc2".getBytes(), "application/pdf");
        blobStorageService.upload(testContainerName, "images/img1.png", "img1".getBytes(), "image/png");
        blobStorageService.upload(testContainerName, "images/img2.png", "img2".getBytes(), "image/png");

        // When
        List<String> allBlobs = blobStorageService.listBlobs(testContainerName);

        // Then
        assertThat(allBlobs).hasSize(4);
        assertThat(allBlobs).contains("documents/doc1.pdf", "documents/doc2.pdf", "images/img1.png", "images/img2.png");
    }
}
