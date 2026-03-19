package com.mobflow.workspaceservice.model.entities;

import com.mobflow.workspaceservice.model.enums.WorkspaceRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(
        name = "workspace_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "auth_id"})
)
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "auth_id", nullable = false)
    private UUID authId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private WorkspaceRole role;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public static WorkspaceMember create(Workspace workspace, UUID authId, WorkspaceRole role) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .authId(authId)
                .role(role)
                .build();
    }
}
