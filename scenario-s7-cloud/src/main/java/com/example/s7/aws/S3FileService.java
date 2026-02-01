package com.example.s7.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

/**
 * Service for S3 file operations.
 * Provides upload, download, delete, and list operations for S3 objects.
 */
@Service
public class S3FileService {

    private static final Logger log = LoggerFactory.getLogger(S3FileService.class);

    private final S3Client s3Client;

    public S3FileService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Creates a bucket if it doesn't exist.
     *
     * @param bucketName the bucket name
     */
    public void createBucketIfNotExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.debug("Bucket {} already exists", bucketName);
        } catch (NoSuchBucketException e) {
            log.info("Creating bucket: {}", bucketName);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }
    }

    /**
     * Uploads a file to S3.
     *
     * @param bucketName the bucket name
     * @param key the object key
     * @param content the file content
     * @param contentType the content type (e.g., "text/plain", "application/json")
     */
    public void upload(String bucketName, String key, byte[] content, String contentType) {
        log.info("Uploading object to s3://{}/{}", bucketName, key);

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        log.debug("Successfully uploaded {} bytes to s3://{}/{}", content.length, bucketName, key);
    }

    /**
     * Downloads a file from S3.
     *
     * @param bucketName the bucket name
     * @param key the object key
     * @return the file content, or empty if not found
     */
    public Optional<byte[]> download(String bucketName, String key) {
        log.info("Downloading object from s3://{}/{}", bucketName, key);

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            s3Client.getObject(request, ResponseTransformer.toOutputStream(outputStream));
            byte[] content = outputStream.toByteArray();
            log.debug("Successfully downloaded {} bytes from s3://{}/{}", content.length, bucketName, key);
            return Optional.of(content);
        } catch (NoSuchKeyException e) {
            log.warn("Object not found: s3://{}/{}", bucketName, key);
            return Optional.empty();
        }
    }

    /**
     * Deletes a file from S3.
     *
     * @param bucketName the bucket name
     * @param key the object key
     * @return true if the object was deleted, false if it didn't exist
     */
    public boolean delete(String bucketName, String key) {
        log.info("Deleting object from s3://{}/{}", bucketName, key);

        try {
            // Check if object exists first
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            s3Client.deleteObject(request);
            log.debug("Successfully deleted s3://{}/{}", bucketName, key);
            return true;
        } catch (NoSuchKeyException e) {
            log.warn("Object not found for deletion: s3://{}/{}", bucketName, key);
            return false;
        }
    }

    /**
     * Lists all objects in a bucket.
     *
     * @param bucketName the bucket name
     * @return the list of object keys
     */
    public List<String> listObjects(String bucketName) {
        log.info("Listing objects in bucket: {}", bucketName);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .build();

        List<String> keys = s3Client.listObjectsV2(request)
            .contents()
            .stream()
            .map(S3Object::key)
            .toList();

        log.debug("Found {} objects in bucket {}", keys.size(), bucketName);
        return keys;
    }

    /**
     * Lists objects with a specific prefix.
     *
     * @param bucketName the bucket name
     * @param prefix the key prefix
     * @return the list of object keys matching the prefix
     */
    public List<String> listObjectsWithPrefix(String bucketName, String prefix) {
        log.info("Listing objects in bucket {} with prefix: {}", bucketName, prefix);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(prefix)
            .build();

        List<String> keys = s3Client.listObjectsV2(request)
            .contents()
            .stream()
            .map(S3Object::key)
            .toList();

        log.debug("Found {} objects with prefix {} in bucket {}", keys.size(), prefix, bucketName);
        return keys;
    }

    /**
     * Checks if an object exists.
     *
     * @param bucketName the bucket name
     * @param key the object key
     * @return true if the object exists
     */
    public boolean exists(String bucketName, String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
