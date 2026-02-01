package com.example.tc.util;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

/**
 * Helper class for obtaining OAuth2 tokens from Keycloak.
 * Used in security testing scenarios.
 */
public final class TokenHelper {

    private TokenHelper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets an access token from Keycloak using password grant.
     *
     * @param keycloakUrl the Keycloak server URL
     * @param realm       the realm name
     * @param clientId    the client ID
     * @param username    the username
     * @param password    the password
     * @return the access token
     */
    public static String getAccessToken(
            String keycloakUrl,
            String realm,
            String clientId,
            String username,
            String password) {

        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .username(username)
                .password(password)
                .build()) {

            return keycloak.tokenManager().getAccessTokenString();
        }
    }

    /**
     * Gets an access token using client credentials grant.
     *
     * @param keycloakUrl  the Keycloak server URL
     * @param realm        the realm name
     * @param clientId     the client ID
     * @param clientSecret the client secret
     * @return the access token
     */
    public static String getAccessTokenWithClientCredentials(
            String keycloakUrl,
            String realm,
            String clientId,
            String clientSecret) {

        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType("client_credentials")
                .build()) {

            return keycloak.tokenManager().getAccessTokenString();
        }
    }

    /**
     * Builds an Authorization header value.
     *
     * @param token the bearer token
     * @return the Authorization header value
     */
    public static String bearerToken(String token) {
        return "Bearer " + token;
    }
}
