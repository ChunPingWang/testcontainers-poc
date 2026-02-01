package com.example.tc.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating Pact Broker test containers.
 * Used for consumer-driven contract testing.
 */
public final class PactBrokerContainerFactory {

    private static final String IMAGE = "pactfoundation/pact-broker:latest";
    private static final int PACT_BROKER_PORT = 9292;

    private static final GenericContainer<?> INSTANCE = createContainer();

    private PactBrokerContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton Pact Broker container instance.
     *
     * @return the Pact Broker container
     */
    public static GenericContainer<?> getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new Pact Broker container instance.
     *
     * @return a new Pact Broker container
     */
    public static GenericContainer<?> createNew() {
        return createContainer();
    }

    private static GenericContainer<?> createContainer() {
        return new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withExposedPorts(PACT_BROKER_PORT)
            .withEnv("PACT_BROKER_DATABASE_ADAPTER", "sqlite")
            .withEnv("PACT_BROKER_DATABASE_NAME", "/tmp/pact_broker.sqlite")
            .withEnv("PACT_BROKER_ALLOW_PUBLIC_READ", "true")
            .waitingFor(Wait.forHttp("/").forPort(PACT_BROKER_PORT))
            .withReuse(true);
    }

    /**
     * Gets the Pact Broker port.
     *
     * @return the Pact Broker port
     */
    public static int getPactBrokerPort() {
        return PACT_BROKER_PORT;
    }
}
