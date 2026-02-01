package com.example.tc.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Factory for creating Keycloak test containers.
 * Used for OAuth2/OIDC authentication testing.
 */
public final class KeycloakContainerFactory {

    private static final String IMAGE = "quay.io/keycloak/keycloak:24.0";
    private static final int KEYCLOAK_PORT = 8080;
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private KeycloakContainerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a Keycloak container with realm import.
     *
     * @param realmImportPath the classpath path to the realm JSON file
     * @return a configured Keycloak container
     */
    public static GenericContainer<?> create(String realmImportPath) {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withExposedPorts(KEYCLOAK_PORT)
            .withEnv("KEYCLOAK_ADMIN", ADMIN_USERNAME)
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASSWORD)
            .withEnv("KC_HEALTH_ENABLED", "true")
            .withCommand("start-dev", "--import-realm")
            .waitingFor(Wait.forHttp("/health/ready").forPort(KEYCLOAK_PORT))
            .withReuse(true);

        if (realmImportPath != null && !realmImportPath.isEmpty()) {
            container.withCopyFileToContainer(
                MountableFile.forClasspathResource(realmImportPath),
                "/opt/keycloak/data/import/realm.json"
            );
        }

        return container;
    }

    /**
     * Creates a basic Keycloak container without realm import.
     *
     * @return a basic Keycloak container
     */
    public static GenericContainer<?> createBasic() {
        return create(null);
    }

    /**
     * Gets the admin username.
     *
     * @return the admin username
     */
    public static String getAdminUsername() {
        return ADMIN_USERNAME;
    }

    /**
     * Gets the admin password.
     *
     * @return the admin password
     */
    public static String getAdminPassword() {
        return ADMIN_PASSWORD;
    }
}
