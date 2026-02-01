package com.example.tc.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating WireMock test containers.
 * Used for mocking external HTTP APIs.
 */
public final class WireMockContainerFactory {

    private static final String IMAGE = "wiremock/wiremock:3.5.2";
    private static final int WIREMOCK_PORT = 8080;

    private static final GenericContainer<?> INSTANCE = createContainer();

    private WireMockContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton WireMock container instance.
     *
     * @return the WireMock container
     */
    public static GenericContainer<?> getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new WireMock container instance.
     *
     * @return a new WireMock container
     */
    public static GenericContainer<?> createNew() {
        return createContainer();
    }

    private static GenericContainer<?> createContainer() {
        return new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withExposedPorts(WIREMOCK_PORT)
            .withCommand("--global-response-templating", "--disable-gzip", "--verbose")
            .withReuse(true);
    }

    /**
     * Gets the WireMock HTTP port.
     *
     * @return the WireMock port
     */
    public static int getWireMockPort() {
        return WIREMOCK_PORT;
    }
}
