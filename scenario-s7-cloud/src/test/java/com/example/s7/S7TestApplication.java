package com.example.s7;

import com.example.tc.containers.AzuriteContainerFactory;
import com.example.tc.containers.LocalStackContainerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;

/**
 * Test configuration for S7 scenario.
 * Provides LocalStack and Azurite containers for cloud service testing.
 */
@TestConfiguration(proxyBeanMethods = false)
public class S7TestApplication {

    /**
     * LocalStack container for AWS service emulation.
     * Provides S3, SQS, and DynamoDB.
     */
    @Bean
    public LocalStackContainer localStackContainer() {
        LocalStackContainer container = LocalStackContainerFactory.getInstance();
        container.start();
        return container;
    }

    /**
     * Azurite container for Azure Storage emulation.
     * Provides Blob, Queue, and Table storage.
     */
    @Bean
    public GenericContainer<?> azuriteContainer() {
        GenericContainer<?> container = AzuriteContainerFactory.getInstance();
        container.start();
        return container;
    }
}
