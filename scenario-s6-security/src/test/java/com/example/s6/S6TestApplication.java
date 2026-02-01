package com.example.s6;

import com.example.tc.containers.KeycloakContainerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

/**
 * Test configuration for S6 Security scenario.
 * Provides Keycloak container for OAuth2/OIDC testing.
 *
 * Features:
 * - Keycloak container with pre-configured realm
 * - Automatic Spring Boot OAuth2 configuration
 * - Token generation utilities
 */
@TestConfiguration(proxyBeanMethods = false)
public class S6TestApplication {

    private static final String REALM_EXPORT_PATH = "keycloak/realm-export.json";
    private static final String REALM = "testcontainers-poc";

    private static GenericContainer<?> keycloakContainer;

    static {
        keycloakContainer = KeycloakContainerFactory.create(REALM_EXPORT_PATH);
        keycloakContainer.start();
    }

    /**
     * Gets the Keycloak container instance.
     *
     * @return the Keycloak container
     */
    @Bean
    public GenericContainer<?> keycloakContainer() {
        return keycloakContainer;
    }

    /**
     * Gets the Keycloak server URL.
     *
     * @return the Keycloak URL
     */
    public static String getKeycloakUrl() {
        return String.format("http://%s:%d",
            keycloakContainer.getHost(),
            keycloakContainer.getMappedPort(8080));
    }

    /**
     * Gets the Keycloak issuer URI for the realm.
     *
     * @return the issuer URI
     */
    public static String getIssuerUri() {
        return getKeycloakUrl() + "/realms/" + REALM;
    }

    /**
     * Gets the JWK Set URI for token validation.
     *
     * @return the JWK Set URI
     */
    public static String getJwkSetUri() {
        return getIssuerUri() + "/protocol/openid-connect/certs";
    }

    /**
     * Gets the token endpoint.
     *
     * @return the token endpoint URL
     */
    public static String getTokenEndpoint() {
        return getIssuerUri() + "/protocol/openid-connect/token";
    }

    /**
     * Gets the realm name.
     *
     * @return the realm name
     */
    public static String getRealm() {
        return REALM;
    }

    /**
     * Gets the client ID.
     *
     * @return the client ID
     */
    public static String getClientId() {
        return "tc-client";
    }

    /**
     * Registers dynamic properties for Spring Boot OAuth2 configuration.
     *
     * @param registry the property registry
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", S6TestApplication::getIssuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", S6TestApplication::getJwkSetUri);
    }
}
