package com.example.s6;

import com.example.tc.base.IntegrationTestBase;
import com.example.tc.containers.VaultContainerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.vault.VaultContainer;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for HashiCorp Vault dynamic credentials.
 *
 * Tests:
 * - Vault connectivity
 * - Secret storage and retrieval
 * - Dynamic credential generation
 * - Secret rotation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(S6TestApplication.class)
@ActiveProfiles("test")
class VaultCredentialIT extends IntegrationTestBase {

    private static VaultContainer<?> vaultContainer;

    @BeforeAll
    static void setUpVault() throws IOException, InterruptedException {
        vaultContainer = VaultContainerFactory.createNew();
        vaultContainer.start();

        // Enable KV secrets engine
        vaultContainer.execInContainer(
            "vault", "secrets", "enable", "-path=secret", "kv-v2"
        );

        // Store test secrets
        vaultContainer.execInContainer(
            "vault", "kv", "put", "secret/app",
            "database.username=app_user",
            "database.password=secret123",
            "api.key=test-api-key-12345"
        );

        // Store database credentials
        vaultContainer.execInContainer(
            "vault", "kv", "put", "secret/database",
            "host=localhost",
            "port=5432",
            "name=testdb",
            "username=postgres",
            "password=postgres123"
        );
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", S6TestApplication::getIssuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", S6TestApplication::getJwkSetUri);
    }

    @Nested
    @DisplayName("Vault Connectivity Tests")
    class VaultConnectivityTests {

        @Test
        @DisplayName("Vault container is running")
        void vaultContainerIsRunning() {
            assertThat(vaultContainer.isRunning()).isTrue();
        }

        @Test
        @DisplayName("Vault is accessible via HTTP")
        void vaultIsAccessibleViaHttp() throws IOException, InterruptedException {
            var result = vaultContainer.execInContainer("vault", "status");
            // Vault status returns 0 when unsealed and running
            assertThat(result.getExitCode()).isEqualTo(0);
        }

        @Test
        @DisplayName("Vault root token is configured")
        void vaultRootTokenIsConfigured() {
            assertThat(VaultContainerFactory.getRootToken()).isEqualTo("test-root-token");
        }
    }

    @Nested
    @DisplayName("Secret Storage Tests")
    class SecretStorageTests {

        @Test
        @DisplayName("Can store and retrieve secrets")
        void canStoreAndRetrieveSecrets() throws IOException, InterruptedException {
            // Store a new secret
            var putResult = vaultContainer.execInContainer(
                "vault", "kv", "put", "secret/test",
                "key1=value1",
                "key2=value2"
            );
            assertThat(putResult.getExitCode()).isEqualTo(0);

            // Retrieve the secret
            var getResult = vaultContainer.execInContainer(
                "vault", "kv", "get", "-format=json", "secret/test"
            );
            assertThat(getResult.getExitCode()).isEqualTo(0);
            assertThat(getResult.getStdout()).contains("value1");
            assertThat(getResult.getStdout()).contains("value2");
        }

        @Test
        @DisplayName("Can retrieve app secrets")
        void canRetrieveAppSecrets() throws IOException, InterruptedException {
            var result = vaultContainer.execInContainer(
                "vault", "kv", "get", "-format=json", "secret/app"
            );
            assertThat(result.getExitCode()).isEqualTo(0);
            assertThat(result.getStdout()).contains("database.username");
            assertThat(result.getStdout()).contains("app_user");
            assertThat(result.getStdout()).contains("api.key");
            assertThat(result.getStdout()).contains("test-api-key-12345");
        }

        @Test
        @DisplayName("Can retrieve database credentials")
        void canRetrieveDatabaseCredentials() throws IOException, InterruptedException {
            var result = vaultContainer.execInContainer(
                "vault", "kv", "get", "-format=json", "secret/database"
            );
            assertThat(result.getExitCode()).isEqualTo(0);
            assertThat(result.getStdout()).contains("host");
            assertThat(result.getStdout()).contains("localhost");
            assertThat(result.getStdout()).contains("username");
            assertThat(result.getStdout()).contains("postgres");
        }
    }

    @Nested
    @DisplayName("Dynamic Credential Tests")
    class DynamicCredentialTests {

        @Test
        @DisplayName("Can update secrets - simulates rotation")
        void canUpdateSecretsSimulatesRotation() throws IOException, InterruptedException {
            // Store initial secret
            vaultContainer.execInContainer(
                "vault", "kv", "put", "secret/rotating",
                "password=initial-password"
            );

            // Verify initial value
            var initialResult = vaultContainer.execInContainer(
                "vault", "kv", "get", "-format=json", "secret/rotating"
            );
            assertThat(initialResult.getStdout()).contains("initial-password");

            // Rotate the secret
            vaultContainer.execInContainer(
                "vault", "kv", "put", "secret/rotating",
                "password=rotated-password"
            );

            // Verify rotated value
            var rotatedResult = vaultContainer.execInContainer(
                "vault", "kv", "get", "-format=json", "secret/rotating"
            );
            assertThat(rotatedResult.getStdout()).contains("rotated-password");
        }

        @Test
        @DisplayName("Can delete secrets")
        void canDeleteSecrets() throws IOException, InterruptedException {
            // Store a secret
            vaultContainer.execInContainer(
                "vault", "kv", "put", "secret/deleteme",
                "key=value"
            );

            // Verify it exists
            var existsResult = vaultContainer.execInContainer(
                "vault", "kv", "get", "secret/deleteme"
            );
            assertThat(existsResult.getExitCode()).isEqualTo(0);

            // Delete it
            var deleteResult = vaultContainer.execInContainer(
                "vault", "kv", "delete", "secret/deleteme"
            );
            assertThat(deleteResult.getExitCode()).isEqualTo(0);
        }

        @Test
        @DisplayName("Can list secrets")
        void canListSecrets() throws IOException, InterruptedException {
            var result = vaultContainer.execInContainer(
                "vault", "kv", "list", "secret"
            );
            assertThat(result.getExitCode()).isEqualTo(0);
            assertThat(result.getStdout()).contains("app");
            assertThat(result.getStdout()).contains("database");
        }
    }

    @Nested
    @DisplayName("Secret Version Tests")
    class SecretVersionTests {

        @Test
        @DisplayName("Secrets are versioned")
        void secretsAreVersioned() throws IOException, InterruptedException {
            // Create initial version
            vaultContainer.execInContainer(
                "vault", "kv", "put", "secret/versioned",
                "value=version1"
            );

            // Create second version
            vaultContainer.execInContainer(
                "vault", "kv", "put", "secret/versioned",
                "value=version2"
            );

            // Get current version (should be version 2)
            var currentResult = vaultContainer.execInContainer(
                "vault", "kv", "get", "-format=json", "secret/versioned"
            );
            assertThat(currentResult.getStdout()).contains("version2");
            assertThat(currentResult.getStdout()).contains("\"version\": 2");
        }

        @Test
        @DisplayName("Can access specific secret version")
        void canAccessSpecificSecretVersion() throws IOException, InterruptedException {
            // Create multiple versions
            vaultContainer.execInContainer(
                "vault", "kv", "put", "secret/multiversion",
                "data=first"
            );
            vaultContainer.execInContainer(
                "vault", "kv", "put", "secret/multiversion",
                "data=second"
            );
            vaultContainer.execInContainer(
                "vault", "kv", "put", "secret/multiversion",
                "data=third"
            );

            // Access version 1
            var v1Result = vaultContainer.execInContainer(
                "vault", "kv", "get", "-version=1", "-format=json", "secret/multiversion"
            );
            assertThat(v1Result.getStdout()).contains("first");

            // Access version 2
            var v2Result = vaultContainer.execInContainer(
                "vault", "kv", "get", "-version=2", "-format=json", "secret/multiversion"
            );
            assertThat(v2Result.getStdout()).contains("second");

            // Access version 3 (current)
            var v3Result = vaultContainer.execInContainer(
                "vault", "kv", "get", "-version=3", "-format=json", "secret/multiversion"
            );
            assertThat(v3Result.getStdout()).contains("third");
        }
    }

    @Nested
    @DisplayName("Policy Tests")
    class PolicyTests {

        @Test
        @DisplayName("Can create and apply policy")
        void canCreateAndApplyPolicy() throws IOException, InterruptedException {
            // Create a policy
            String policyContent = """
                path "secret/data/readonly/*" {
                  capabilities = ["read", "list"]
                }
                """;

            var policyResult = vaultContainer.execInContainer(
                "sh", "-c",
                "echo '" + policyContent + "' | vault policy write readonly-policy -"
            );
            assertThat(policyResult.getExitCode()).isEqualTo(0);

            // List policies
            var listResult = vaultContainer.execInContainer(
                "vault", "policy", "list"
            );
            assertThat(listResult.getStdout()).contains("readonly-policy");
        }

        @Test
        @DisplayName("Can read policy")
        void canReadPolicy() throws IOException, InterruptedException {
            // Create a policy first
            String policyContent = """
                path "secret/data/test/*" {
                  capabilities = ["create", "read", "update", "delete"]
                }
                """;

            vaultContainer.execInContainer(
                "sh", "-c",
                "echo '" + policyContent + "' | vault policy write test-policy -"
            );

            // Read the policy
            var readResult = vaultContainer.execInContainer(
                "vault", "policy", "read", "test-policy"
            );
            assertThat(readResult.getExitCode()).isEqualTo(0);
            assertThat(readResult.getStdout()).contains("secret/data/test/*");
            assertThat(readResult.getStdout()).contains("create");
        }
    }
}
