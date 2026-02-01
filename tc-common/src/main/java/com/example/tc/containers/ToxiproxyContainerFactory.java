package com.example.tc.containers;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating Toxiproxy test containers.
 * Used for simulating network failures and latency.
 */
public final class ToxiproxyContainerFactory {

    private static final String IMAGE = "ghcr.io/shopify/toxiproxy:2.9.0";

    private ToxiproxyContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a Toxiproxy container on the given network.
     *
     * @param network the Docker network
     * @return a configured Toxiproxy container
     */
    public static ToxiproxyContainer create(Network network) {
        return new ToxiproxyContainer(DockerImageName.parse(IMAGE))
            .withNetwork(network)
            .withNetworkAliases("toxiproxy");
    }

    /**
     * Creates a standalone Toxiproxy container.
     *
     * @return a Toxiproxy container
     */
    public static ToxiproxyContainer createStandalone() {
        return new ToxiproxyContainer(DockerImageName.parse(IMAGE));
    }
}
