package com.mobflow.authservice.repository;

import com.mobflow.authservice.model.entities.UserCredential;
import com.mobflow.authservice.model.enums.Role;
import com.mobflow.authservice.testsupport.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class UserCredentialRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanUp() {
        userCredentialRepository.deleteAll();
    }

    @Test
    void shouldPersistAndLoadUserCredential() {
        // Given
        UserCredential user = buildUser("john", "john@mobflow.dev", Role.ROLE_USER);

        // When
        UserCredential saved = userCredentialRepository.saveAndFlush(user);
        entityManager.clear();

        // Then
        UserCredential reloaded = userCredentialRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getUsername()).isEqualTo("john");
        assertThat(reloaded.getEmail()).isEqualTo("john@mobflow.dev");
        assertThat(reloaded.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindByUsername() {
        // Given
        UserCredential saved = userCredentialRepository.saveAndFlush(buildUser("john", "john@mobflow.dev", Role.ROLE_USER));
        entityManager.clear();

        // When / Then
        assertThat(userCredentialRepository.findByUsername("john"))
                .isPresent()
                .get()
                .extracting(UserCredential::getId)
                .isEqualTo(saved.getId());
    }

    @Test
    void shouldFindByEmail() {
        // Given
        UserCredential saved = userCredentialRepository.saveAndFlush(buildUser("john", "john@mobflow.dev", Role.ROLE_USER));
        entityManager.clear();

        // When / Then
        assertThat(userCredentialRepository.findByEmail("john@mobflow.dev"))
                .isPresent()
                .get()
                .extracting(UserCredential::getId)
                .isEqualTo(saved.getId());
    }

    @Test
    void shouldReportExistingUsernameAndEmail() {
        // Given
        userCredentialRepository.saveAndFlush(buildUser("john", "john@mobflow.dev", Role.ROLE_USER));

        // When / Then
        assertThat(userCredentialRepository.existsByUsername("john")).isTrue();
        assertThat(userCredentialRepository.existsByEmail("john@mobflow.dev")).isTrue();
        assertThat(userCredentialRepository.existsByUsername("missing")).isFalse();
        assertThat(userCredentialRepository.existsByEmail("missing@mobflow.dev")).isFalse();
    }

    @Test
    void shouldEnforceUniqueUsernameConstraint() {
        // Given
        userCredentialRepository.saveAndFlush(buildUser("john", "john@mobflow.dev", Role.ROLE_USER));
        entityManager.clear();

        // When / Then
        assertThatThrownBy(() -> userCredentialRepository.saveAndFlush(buildUser("john", "other@mobflow.dev", Role.ROLE_USER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceUniqueEmailConstraint() {
        // Given
        userCredentialRepository.saveAndFlush(buildUser("john", "john@mobflow.dev", Role.ROLE_USER));
        entityManager.clear();

        // When / Then
        assertThatThrownBy(() -> userCredentialRepository.saveAndFlush(buildUser("other", "john@mobflow.dev", Role.ROLE_USER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPersistRoleEnumAsStringCompatibleWithEntityMapping() {
        // Given
        UserCredential saved = userCredentialRepository.saveAndFlush(buildUser("admin", "admin@mobflow.dev", Role.ROLE_ADMIN));
        entityManager.clear();

        // When
        UserCredential reloaded = userCredentialRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(reloaded.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    private UserCredential buildUser(String username, String email, Role role) {
        return UserCredential.builder()
                .username(username)
                .email(email)
                .passwordHash("encoded-password")
                .role(role)
                .enabled(true)
                .accountNonLocked(true)
                .build();
    }
}
