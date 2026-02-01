package com.example.s7.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Azure client configuration.
 * Configures Azure Blob Storage client to connect to Azurite or real Azure.
 * Only activated when azure.storage.connection-string property has a non-blank value.
 */
@Configuration
@ConditionalOnExpression("!'${azure.storage.connection-string:}'.isBlank()")
public class AzureConfig {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    /**
     * Creates an Azure Blob Service client configured for Azurite or real Azure.
     *
     * @return the Blob Service client
     */
    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
    }
}
