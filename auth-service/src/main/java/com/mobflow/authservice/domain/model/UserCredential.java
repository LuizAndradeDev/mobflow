package com.mobflow.authservice.domain.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Table(name = "user_credential")
@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "passwordHash")
@EqualsAndHashCode(of = "id")
public class UserCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username_credential",  nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email_credential", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_credential", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "authority_credential",  nullable = false)
    private Role role;

    @Column(name = "account_enable", nullable = false)
    private boolean enabled = true;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts;

    @Column(name = "account_non_locked")
    private boolean accountNonLocked = true;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
