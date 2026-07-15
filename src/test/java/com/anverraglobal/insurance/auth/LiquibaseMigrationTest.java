package com.anverraglobal.insurance.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test that verifies Liquibase migrations execute cleanly
 * and all expected tables are created with the correct schema.
 *
 * <p>Uses the full Spring Boot application context with a real PostgreSQL
 * container (via {@link AbstractIntegrationTest}).
 * Spring Boot's auto-configuration runs Liquibase before the first test.</p>
 *
 * <p>This test acts as a migration regression guard:
 * if a migration is invalid, the Spring context will fail to start
 * and all tests in this class will fail with a descriptive error.</p>
 */
@DisplayName("LiquibaseMigrationTest")
class LiquibaseMigrationTest extends AbstractRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = ?
                """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name   = ?
                  AND column_name  = ?
                """,
                Integer.class,
                tableName, columnName
        );
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = ?
                """,
                Integer.class,
                indexName
        );
        return count != null && count > 0;
    }

    // -----------------------------------------------------------------------
    // Table existence
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("all auth domain tables should be created by Liquibase")
    void allTablesShouldExist() {
        List<String> expectedTables = List.of(
                "users",
                "roles",
                "user_roles",
                "user_profiles",
                "refresh_tokens",
                "mobile_otps"
        );

        for (String table : expectedTables) {
            assertThat(tableExists(table))
                    .as("Table '%s' should exist", table)
                    .isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // users table schema
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("users table should have all required columns")
    void usersTableShouldHaveAllColumns() {
        List<String> expectedColumns = List.of(
                "id", "uuid", "email", "password", "name", "phone",
                "profile_image", "status", "email_verified", "phone_verified",
                "login_attempts", "locked_until", "last_login", "reward_points",
                "created_by_user_id", "deleted", "deleted_at",
                "created_at", "updated_at", "created_by", "updated_by"
        );

        for (String col : expectedColumns) {
            assertThat(columnExists("users", col))
                    .as("Column 'users.%s' should exist", col)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("users table should have all required indexes")
    void usersTableIndexesShouldExist() {
        assertThat(indexExists("idx_users_email")).isTrue();
        assertThat(indexExists("idx_users_uuid")).isTrue();
        assertThat(indexExists("idx_users_phone")).isTrue();
        assertThat(indexExists("idx_users_status")).isTrue();
        assertThat(indexExists("idx_users_deleted")).isTrue();
    }

    // -----------------------------------------------------------------------
    // roles table and seed data
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("roles table should be seeded with all 5 roles")
    void rolesTableShouldHaveAllFiveRoles() {
        List<String> roles = jdbcTemplate.queryForList(
                "SELECT name FROM roles ORDER BY name", String.class);

        assertThat(roles).containsExactlyInAnyOrder(
                "ADMIN", "BROKER", "AGENT", "SUB_AGENT", "DE_AGENT");
    }

    // -----------------------------------------------------------------------
    // user_roles table schema
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("user_roles table should have required columns and indexes")
    void userRolesTableShouldHaveCorrectSchema() {
        assertThat(columnExists("user_roles", "id")).isTrue();
        assertThat(columnExists("user_roles", "user_id")).isTrue();
        assertThat(columnExists("user_roles", "role")).isTrue();
        assertThat(indexExists("idx_user_roles_user_id")).isTrue();
        assertThat(indexExists("idx_user_roles_role")).isTrue();
    }

    // -----------------------------------------------------------------------
    // user_profiles table schema
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("user_profiles table should have all Indian-market KYC columns")
    void userProfilesShouldHaveKycColumns() {
        assertThat(columnExists("user_profiles", "pan_number")).isTrue();
        assertThat(columnExists("user_profiles", "aadhar_number")).isTrue();
        assertThat(columnExists("user_profiles", "gstin")).isTrue();
        assertThat(columnExists("user_profiles", "bank_ifsc_code")).isTrue();
        assertThat(columnExists("user_profiles", "agent_code")).isTrue();
        assertThat(columnExists("user_profiles", "broker_code")).isTrue();
    }

    // -----------------------------------------------------------------------
    // refresh_tokens table schema
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("refresh_tokens table should have required columns and indexes")
    void refreshTokensTableShouldHaveCorrectSchema() {
        assertThat(columnExists("refresh_tokens", "token")).isTrue();
        assertThat(columnExists("refresh_tokens", "user_id")).isTrue();
        assertThat(columnExists("refresh_tokens", "expires_at")).isTrue();
        assertThat(columnExists("refresh_tokens", "revoked")).isTrue();
        assertThat(columnExists("refresh_tokens", "created_at")).isTrue();
        assertThat(indexExists("idx_refresh_tokens_token")).isTrue();
        assertThat(indexExists("idx_refresh_tokens_user_id")).isTrue();
        assertThat(indexExists("idx_refresh_tokens_expires")).isTrue();
    }

    // -----------------------------------------------------------------------
    // mobile_otps table schema
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("mobile_otps table should have required columns and composite index")
    void mobileOtpsTableShouldHaveCorrectSchema() {
        assertThat(columnExists("mobile_otps", "phone_number")).isTrue();
        assertThat(columnExists("mobile_otps", "otp_code")).isTrue();
        assertThat(columnExists("mobile_otps", "expires_at")).isTrue();
        assertThat(columnExists("mobile_otps", "verified")).isTrue();
        assertThat(columnExists("mobile_otps", "purpose")).isTrue();
        assertThat(columnExists("mobile_otps", "attempt_count")).isTrue();
        assertThat(columnExists("mobile_otps", "created_at")).isTrue();

        // Composite index for lookup + rate-limit queries
        assertThat(indexExists("idx_mobile_otps_lookup")).isTrue();
        assertThat(indexExists("idx_mobile_otps_expires_at")).isTrue();
    }

    // -----------------------------------------------------------------------
    // Liquibase tracking
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Liquibase DATABASECHANGELOG should record all 5 changesets")
    void liquibaseChangelogShouldHaveFiveChangesets() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog", Integer.class);

        // 1 (001-initial) + 4 (002-1,002-2,002-3,002-4) + 1 (003-1) + 1 (004-1) = 7
        assertThat(count).isEqualTo(7);
    }
}
