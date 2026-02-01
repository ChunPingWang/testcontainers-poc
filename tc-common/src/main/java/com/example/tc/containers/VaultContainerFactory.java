package com.example.tc.containers;

import org.testcontainers.vault.VaultContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for creating HashiCorp Vault test containers.
 * Used for secrets management and dynamic credentials testing.
 */
public final class VaultContainerFactory {

    private static final String IMAGE = "hashicorp/vault:1.16";
    private static final String ROOT_TOKEN = "test-root-token";

    private static final VaultContainer<?> INSTANCE = createContainer();

    private VaultContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton Vault container instance.
     *
     * @return the Vault container
     */
    public static VaultContainer<?> getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new Vault container instance.
     *
     * @return a new Vault container
     */
    public static VaultContainer<?> createNew() {
        return createContainer();
    }

    private static VaultContainer<?> createContainer() {
        return new VaultContainer<>(DockerImageName.parse(IMAGE))
            .withVaultToken(ROOT_TOKEN)
            .withReuse(true);
    }

    /**
     * Gets the root token for Vault access.
     *
     * @return the root token
     */
    public static String getRootToken() {
        return ROOT_TOKEN;
    }
}
