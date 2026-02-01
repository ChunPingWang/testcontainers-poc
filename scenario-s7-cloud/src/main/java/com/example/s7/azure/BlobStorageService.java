package com.example.s7.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

/**
 * Service for Azure Blob Storage operations.
 * Provides upload, download, delete, and list operations for blobs.
 */
@Service
public class BlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(BlobStorageService.class);

    private final BlobServiceClient blobServiceClient;

    public BlobStorageService(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    /**
     * Creates a container if it doesn't exist.
     *
     * @param containerName the container name
     */
    public void createContainerIfNotExists(String containerName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            log.info("Creating container: {}", containerName);
            containerClient.create();
        } else {
            log.debug("Container {} already exists", containerName);
        }
    }

    /**
     * Uploads a blob to a container.
     *
     * @param containerName the container name
     * @param blobName the blob name
     * @param content the blob content
     * @param contentType the content type (e.g., "text/plain", "application/json")
     */
    public void upload(String containerName, String blobName, byte[] content, String contentType) {
        log.info("Uploading blob to {}/{}", containerName, blobName);

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            blobClient.upload(inputStream, content.length, true);
            log.debug("Successfully uploaded {} bytes to {}/{}", content.length, containerName, blobName);
        } catch (Exception e) {
            log.error("Failed to upload blob: {}/{}", containerName, blobName, e);
            throw new RuntimeException("Failed to upload blob", e);
        }
    }

    /**
     * Downloads a blob from a container.
     *
     * @param containerName the container name
     * @param blobName the blob name
     * @return the blob content, or empty if not found
     */
    public Optional<byte[]> download(String containerName, String blobName) {
        log.info("Downloading blob from {}/{}", containerName, blobName);

        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                log.warn("Blob not found: {}/{}", containerName, blobName);
                return Optional.empty();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.downloadStream(outputStream);
            byte[] content = outputStream.toByteArray();
            log.debug("Successfully downloaded {} bytes from {}/{}", content.length, containerName, blobName);
            return Optional.of(content);
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                log.warn("Blob not found: {}/{}", containerName, blobName);
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Deletes a blob from a container.
     *
     * @param containerName the container name
     * @param blobName the blob name
     * @return true if the blob was deleted, false if it didn't exist
     */
    public boolean delete(String containerName, String blobName) {
        log.info("Deleting blob from {}/{}", containerName, blobName);

        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                log.warn("Blob not found for deletion: {}/{}", containerName, blobName);
                return false;
            }

            blobClient.delete();
            log.debug("Successfully deleted {}/{}", containerName, blobName);
            return true;
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                log.warn("Blob not found for deletion: {}/{}", containerName, blobName);
                return false;
            }
            throw e;
        }
    }

    /**
     * Lists all blobs in a container.
     *
     * @param containerName the container name
     * @return the list of blob names
     */
    public List<String> listBlobs(String containerName) {
        log.info("Listing blobs in container: {}", containerName);

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        List<String> blobNames = containerClient.listBlobs()
            .stream()
            .map(BlobItem::getName)
            .toList();

        log.debug("Found {} blobs in container {}", blobNames.size(), containerName);
        return blobNames;
    }

    /**
     * Lists blobs with a specific prefix.
     *
     * @param containerName the container name
     * @param prefix the blob name prefix
     * @return the list of blob names matching the prefix
     */
    public List<String> listBlobsWithPrefix(String containerName, String prefix) {
        log.info("Listing blobs in container {} with prefix: {}", containerName, prefix);

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        List<String> blobNames = containerClient.listBlobsByHierarchy(prefix)
            .stream()
            .map(BlobItem::getName)
            .toList();

        log.debug("Found {} blobs with prefix {} in container {}", blobNames.size(), prefix, containerName);
        return blobNames;
    }

    /**
     * Checks if a blob exists.
     *
     * @param containerName the container name
     * @param blobName the blob name
     * @return true if the blob exists
     */
    public boolean exists(String containerName, String blobName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        return blobClient.exists();
    }

    /**
     * Gets the size of a blob.
     *
     * @param containerName the container name
     * @param blobName the blob name
     * @return the blob size in bytes, or empty if not found
     */
    public Optional<Long> getBlobSize(String containerName, String blobName) {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                return Optional.empty();
            }

            return Optional.of(blobClient.getProperties().getBlobSize());
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
