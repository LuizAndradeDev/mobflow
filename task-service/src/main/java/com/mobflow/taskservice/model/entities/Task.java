package com.mobflow.taskservice.model.entities;

import com.mobflow.taskservice.model.enums.TaskPriority;
import com.mobflow.taskservice.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "list")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private TaskList list;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "assignee_auth_id")
    private UUID assigneeAuthId;

    @Column(name = "created_by_auth_id", nullable = false)
    private UUID createdByAuthId;

    @Column(name = "completed_by_auth_id")
    private UUID completedByAuthId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "position", nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static Task create(TaskList list, UUID workspaceId, String title, TaskPriority priority, UUID createdBy) {
        return Task.builder()
                .list(list)
                .workspaceId(workspaceId)
                .title(title)
                .priority(priority)
                .createdByAuthId(createdBy)
                .position(0)
                .status(TaskStatus.TODO)
                .build();
    }
}
