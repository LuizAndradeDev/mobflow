package com.mobflow.authservice.model.entities;

import com.mobflow.authservice.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserCredentialTest {

    @Test
    void createUserCredentialShouldAssignRoleUserByDefault() {
        // Given / When
        UserCredential user = UserCredential.createUserCredential("john", "john@mobflow.dev", "encoded-password");

        // Then
        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(user.getUsername()).isEqualTo("john");
        assertThat(user.getEmail()).isEqualTo("john@mobflow.dev");
        assertThat(user.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void getAuthoritiesShouldReturnAdminAndUserForAdminRole() {
        // Given
        UserCredential admin = UserCredential.builder()
                .username("admin")
                .email("admin@mobflow.dev")
                .passwordHash("encoded-password")
                .role(Role.ROLE_ADMIN)
                .build();

        // When
        Collection<? extends GrantedAuthority> authorities = admin.getAuthorities();

        // Then
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void getAuthoritiesShouldReturnOnlyUserForUserRole() {
        // Given
        UserCredential user = UserCredential.builder()
                .username("john")
                .email("john@mobflow.dev")
                .passwordHash("encoded-password")
                .role(Role.ROLE_USER)
                .build();

        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        // Then
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }
}
