package com.mobflow.taskservice.service;

import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.model.dto.response.TaskCommentContextResponseDTO;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.board;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.task;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.taskList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskContextServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskContextService taskContextService;

    @Test
    void getTaskCommentContext_returnsContextUsingFetchedAssociations() {
        UUID workspaceId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        Task task = task(taskList(board(workspaceId)), workspaceId, creatorId, assigneeId);

        when(taskRepository.findTaskContextById(task.getId())).thenReturn(Optional.of(task));

        TaskCommentContextResponseDTO response = taskContextService.getTaskCommentContext(task.getId());

        assertThat(response.taskId()).isEqualTo(task.getId());
        assertThat(response.workspaceId()).isEqualTo(workspaceId);
        assertThat(response.boardId()).isEqualTo(task.getList().getBoard().getId());
        assertThat(response.assigneeAuthId()).isEqualTo(assigneeId);
    }

    @Test
    void getTaskCommentContext_missingTask_throwsNotFound() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findTaskContextById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskContextService.getTaskCommentContext(taskId))
                .isInstanceOf(TaskServiceException.class)
                .hasMessage("Task not found");
    }
}
