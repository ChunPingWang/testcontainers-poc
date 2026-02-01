package com.example.s2;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Schema Migration in S2 scenario.
 * Validates US4: 資料庫 Schema 遷移測試
 *
 * Given 啟動乾淨的資料庫容器
 * When 執行 Schema 遷移
 * Then 所有遷移腳本按順序執行成功
 */
@SpringBootTest
@ActiveProfiles("test")
class SchemaMigrationIT extends S2IntegrationTestBase {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldExecuteAllMigrationsSuccessfully() {
        // When
        MigrationInfoService migrationInfoService = flyway.info();
        MigrationInfo[] applied = migrationInfoService.applied();

        // Then
        assertThat(applied).isNotEmpty();
        assertThat(applied[0].getVersion().toString()).isEqualTo("1");
        assertThat(applied[0].getDescription()).contains("create customers table");
    }

    @Test
    void shouldHaveCorrectSchemaVersion() {
        // When
        MigrationInfo current = flyway.info().current();

        // Then
        assertThat(current).isNotNull();
        assertThat(current.getVersion().toString()).isEqualTo("1");
    }

    @Test
    void shouldHaveNoPendingMigrations() {
        // When
        MigrationInfo[] pending = flyway.info().pending();

        // Then
        assertThat(pending).isEmpty();
    }

    @Test
    void shouldCreateCustomersTableWithCorrectStructure() {
        // When - Query table structure
        var columns = jdbcTemplate.queryForList(
            """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'customers'
            ORDER BY ordinal_position
            """
        );

        // Then
        assertThat(columns).hasSize(7);

        // Verify essential columns exist
        assertThat(columns)
            .extracting(row -> row.get("column_name"))
            .contains("id", "name", "email", "phone", "address", "created_at", "updated_at");
    }

    @Test
    void shouldCreateUniqueIndexOnEmail() {
        // When
        var indexes = jdbcTemplate.queryForList(
            """
            SELECT indexname FROM pg_indexes
            WHERE tablename = 'customers' AND indexname = 'idx_customers_email'
            """
        );

        // Then
        assertThat(indexes).hasSize(1);
    }

    @Test
    void shouldEnforceEmailUniqueness() {
        // Given - Clean state
        jdbcTemplate.execute("DELETE FROM customers WHERE email = 'test@example.com'");

        // When - Insert first customer
        jdbcTemplate.update(
            """
            INSERT INTO customers (id, name, email, created_at, updated_at)
            VALUES (gen_random_uuid(), 'Test', 'test@example.com', NOW(), NOW())
            """
        );

        // Then - Second insert with same email should fail
        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.dao.DuplicateKeyException.class,
            () -> jdbcTemplate.update(
                """
                INSERT INTO customers (id, name, email, created_at, updated_at)
                VALUES (gen_random_uuid(), 'Test2', 'test@example.com', NOW(), NOW())
                """
            )
        );
    }
}
