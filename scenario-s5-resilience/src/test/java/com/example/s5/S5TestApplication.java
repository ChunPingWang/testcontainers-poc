package com.example.s5;

import com.example.tc.containers.ToxiproxyContainerFactory;
import com.example.tc.containers.WireMockContainerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

/**
 * Test configuration for S5 Resilience scenario.
 * Provides WireMock and Toxiproxy containers as Spring beans.
 *
 * This configuration can be used with @Import(S5TestApplication.class) when you need
 * container instances as Spring beans. For simpler cases, tests can use @Container
 * annotation directly with Testcontainers JUnit 5 extension.
 *
 * This configuration supports:
 * - WireMock for mocking external HTTP APIs
 * - Toxiproxy for network fault injection (latency, disconnects, etc.)
 */
@TestConfiguration(proxyBeanMethods = false)
public class S5TestApplication {

    private static final String WIREMOCK_NETWORK_ALIAS = "wiremock";
    private static final int PROXY_PORT = 8666;

    /**
     * Shared Docker network for container communication.
     */
    @Bean
    public Network testNetwork() {
        return Network.newNetwork();
    }

    /**
     * WireMock container for mocking external APIs.
     * Uses the WireMockContainerFactory from tc-common.
     */
    @Bean
    public GenericContainer<?> wireMockContainer(Network testNetwork) {
        GenericContainer<?> container = WireMockContainerFactory.createNew()
                .withNetwork(testNetwork)
                .withNetworkAliases(WIREMOCK_NETWORK_ALIAS);
        container.start();
        return container;
    }

    /**
     * Toxiproxy container for network fault injection.
     * Uses the ToxiproxyContainerFactory from tc-common.
     */
    @Bean
    public ToxiproxyContainer toxiproxyContainer(Network testNetwork) {
        ToxiproxyContainer container = ToxiproxyContainerFactory.create(testNetwork)
                .withExposedPorts(8474, PROXY_PORT);
        container.start();
        return container;
    }

    /**
     * Gets the WireMock network alias for use with Toxiproxy.
     */
    public static String getWireMockNetworkAlias() {
        return WIREMOCK_NETWORK_ALIAS;
    }

    /**
     * Gets the proxy port for Toxiproxy.
     */
    public static int getProxyPort() {
        return PROXY_PORT;
    }
}
