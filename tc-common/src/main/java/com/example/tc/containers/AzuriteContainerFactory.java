package com.example.tc.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating Azurite test containers.
 * Used for Azure Blob Storage emulation.
 */
public final class AzuriteContainerFactory {

    private static final String IMAGE = "mcr.microsoft.com/azure-storage/azurite:3.30.0";
    private static final int BLOB_PORT = 10000;
    private static final int QUEUE_PORT = 10001;
    private static final int TABLE_PORT = 10002;

    // Default Azurite connection string components
    public static final String ACCOUNT_NAME = "devstoreaccount1";
    public static final String ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    private static final GenericContainer<?> INSTANCE = createContainer();

    private AzuriteContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton Azurite container instance.
     *
     * @return the Azurite container
     */
    public static GenericContainer<?> getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new Azurite container instance.
     *
     * @return a new Azurite container
     */
    public static GenericContainer<?> createNew() {
        return createContainer();
    }

    private static GenericContainer<?> createContainer() {
        return new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withExposedPorts(BLOB_PORT, QUEUE_PORT, TABLE_PORT)
            .withReuse(true);
    }

    /**
     * Gets the Blob service port.
     *
     * @return the Blob port
     */
    public static int getBlobPort() {
        return BLOB_PORT;
    }

    /**
     * Builds the connection string for Azurite.
     *
     * @param host the container host
     * @param blobPort the mapped blob port
     * @return the connection string
     */
    public static String buildConnectionString(String host, int blobPort) {
        return String.format(
            "DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;BlobEndpoint=http://%s:%d/%s;",
            ACCOUNT_NAME, ACCOUNT_KEY, host, blobPort, ACCOUNT_NAME
        );
    }
}
