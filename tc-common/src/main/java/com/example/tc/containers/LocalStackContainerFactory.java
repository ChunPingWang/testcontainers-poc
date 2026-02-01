package com.example.tc.containers;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating LocalStack test containers.
 * Used for AWS service emulation (S3, SQS, DynamoDB).
 */
public final class LocalStackContainerFactory {

    private static final String IMAGE = "localstack/localstack:3.4";

    private static final LocalStackContainer INSTANCE = createContainer();

    private LocalStackContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton LocalStack container instance.
     *
     * @return the LocalStack container
     */
    public static LocalStackContainer getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new LocalStack container instance.
     *
     * @return a new LocalStack container
     */
    public static LocalStackContainer createNew() {
        return createContainer();
    }

    /**
     * Creates a LocalStack container with specific services.
     *
     * @param services the AWS services to enable
     * @return a LocalStack container
     */
    public static LocalStackContainer createWithServices(Service... services) {
        return new LocalStackContainer(DockerImageName.parse(IMAGE))
            .withServices(services)
            .withReuse(true);
    }

    private static LocalStackContainer createContainer() {
        return new LocalStackContainer(DockerImageName.parse(IMAGE))
            .withServices(Service.S3, Service.SQS, Service.DYNAMODB)
            .withReuse(true);
    }
}
