package com.mobflow.workspaceservice.model.entities;

import com.mobflow.workspaceservice.model.enums.InviteStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "workspace_invite")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class WorkspaceInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "target_auth_id", nullable = false)
    private UUID targetAuthId;

    @Column(name = "invited_by_auth_id", nullable = false)
    private UUID invitedByAuthId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InviteStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public static WorkspaceInvite create(Workspace workspace, UUID targetAuthId, UUID invitedByAuthId) {
        return WorkspaceInvite.builder()
                .workspace(workspace)
                .targetAuthId(targetAuthId)
                .invitedByAuthId(invitedByAuthId)
                .status(InviteStatus.PENDING)
                .build();
    }
}
