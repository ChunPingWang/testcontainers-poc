package com.example.s1;

import com.example.tc.base.IntegrationTestBase;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Schema Migration.
 * Validates US4: 資料庫 Schema 遷移測試
 *
 * Given 啟動乾淨的資料庫容器
 * When 執行 Schema 遷移
 * Then 所有遷移腳本按順序執行成功
 */
@SpringBootTest
@Import(S1TestApplication.class)
@ActiveProfiles("test")
class SchemaMigrationIT extends IntegrationTestBase {

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
        assertThat(applied[0].getDescription()).contains("create orders table");
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
    void shouldCreateOrdersTableWithCorrectStructure() {
        // When - Query table structure
        var columns = jdbcTemplate.queryForList(
            """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'orders'
            ORDER BY ordinal_position
            """
        );

        // Then
        assertThat(columns).hasSize(8);

        // Verify essential columns exist
        assertThat(columns)
            .extracting(row -> row.get("column_name"))
            .contains("id", "customer_name", "product_name", "quantity", "amount", "status", "created_at", "updated_at");
    }

    @Test
    void shouldCreateIndexOnStatus() {
        // When
        var indexes = jdbcTemplate.queryForList(
            """
            SELECT indexname FROM pg_indexes
            WHERE tablename = 'orders' AND indexname = 'idx_orders_status'
            """
        );

        // Then
        assertThat(indexes).hasSize(1);
    }

    @Test
    void shouldCreateIndexOnCreatedAt() {
        // When
        var indexes = jdbcTemplate.queryForList(
            """
            SELECT indexname FROM pg_indexes
            WHERE tablename = 'orders' AND indexname = 'idx_orders_created_at'
            """
        );

        // Then
        assertThat(indexes).hasSize(1);
    }
}
