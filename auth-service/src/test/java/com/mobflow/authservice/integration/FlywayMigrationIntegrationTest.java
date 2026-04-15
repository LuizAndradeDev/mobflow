package com.mobflow.authservice.integration;

import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.model.enums.Role;
import com.mobflow.authservice.repository.UserCredentialRepository;
import com.mobflow.authservice.testsupport.AbstractPostgresIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayMigrationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @BeforeEach
    void cleanUp() {
        userCredentialRepository.deleteAll();
    }

    @Test
    void shouldApplyExpectedFlywayMigrationsOnStartup() {
        // When
        List<String> appliedVersions = Arrays.stream(flyway.info().applied())
                .map(MigrationInfo::getVersion)
                .map(Object::toString)
                .toList();

        // Then
        assertThat(appliedVersions).contains("1", "2");
    }

    @Test
    void shouldCreateUserCredentialTableWithExpectedColumns() {
        // When
        List<String> columns = jdbcTemplate.queryForList("""
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_name = 'user_credential'
                        ORDER BY ordinal_position
                        """, String.class);

        // Then
        assertThat(columns).contains(
                "id",
                "username_credential",
                "email_credential",
                "password_credential",
                "authority_credential",
                "account_enable",
                "failed_login_attempts",
                "account_non_locked",
                "lock_time",
                "created_time",
                "updated_time",
                "last_login"
        );
    }

    @Test
    void shouldCreateExpectedUsernameIndexFromSecondMigration() {
        // When
        List<String> indexes = jdbcTemplate.queryForList("""
                        SELECT indexname
                        FROM pg_indexes
                        WHERE tablename = 'user_credential'
                        """, String.class);

        // Then
        assertThat(indexes).contains("idx_users_username");
    }

    @Test
    void schemaShouldRemainCompatibleWithUserCredentialEntityMapping() {
        // Given
        UserCredential user = UserCredential.builder()
                .username("john")
                .email("john@mobflow.dev")
                .passwordHash("encoded-password")
                .role(Role.ROLE_USER)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        // When
        UserCredential saved = userCredentialRepository.saveAndFlush(user);
        Map<String, Object> persistedRow = jdbcTemplate.queryForMap("""
                SELECT username_credential, email_credential, authority_credential
                FROM user_credential
                WHERE id = ?
                """, saved.getId());

        // Then
        assertThat(persistedRow.get("username_credential")).isEqualTo("john");
        assertThat(persistedRow.get("email_credential")).isEqualTo("john@mobflow.dev");
        assertThat(persistedRow.get("authority_credential")).isEqualTo("ROLE_USER");
    }
}
