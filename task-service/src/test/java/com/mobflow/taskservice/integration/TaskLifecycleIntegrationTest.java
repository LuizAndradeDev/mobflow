package com.mobflow.taskservice.integration;

import com.mobflow.taskservice.client.UserServiceClient;
import com.mobflow.taskservice.client.WorkspaceClient;
import com.mobflow.taskservice.events.TaskEventPublisher;
import com.mobflow.taskservice.model.dto.request.CreateBoardRequest;
import com.mobflow.taskservice.model.dto.request.CreateTaskListRequest;
import com.mobflow.taskservice.model.dto.request.CreateTaskRequest;
import com.mobflow.taskservice.model.dto.request.UpdateTaskRequest;
import com.mobflow.taskservice.model.dto.response.BoardResponseDTO;
import com.mobflow.taskservice.model.dto.response.TaskListResponseDTO;
import com.mobflow.taskservice.model.dto.response.TaskResponseDTO;
import com.mobflow.taskservice.model.enums.TaskStatus;
import com.mobflow.taskservice.repository.BoardRepository;
import com.mobflow.taskservice.repository.TaskListRepository;
import com.mobflow.taskservice.repository.TaskRepository;
import com.mobflow.taskservice.service.BoardService;
import com.mobflow.taskservice.service.TaskListService;
import com.mobflow.taskservice.service.TaskService;
import com.mobflow.taskservice.testsupport.AbstractPostgresTaskServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "security.jwt.secret-key=c3VwZXItc2VjdXJlLXRlc3Qta2V5LWZvci10YXNrLXNlcnZpY2UtMTIzNDU2Nzg5MA==",
        "workspace.service.url=http://localhost:8082",
        "user.service.url=http://localhost:8081",
        "internal.secret=test-secret"
})
class TaskLifecycleIntegrationTest extends AbstractPostgresTaskServiceTest {

    @Autowired
    private BoardService boardService;

    @Autowired
    private TaskListService taskListService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TaskListRepository taskListRepository;

    @Autowired
    private TaskRepository taskRepository;

    @MockBean
    private WorkspaceClient workspaceClient;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private TaskEventPublisher taskEventPublisher;

    @BeforeEach
    void cleanUp() {
        taskRepository.deleteAll();
        taskListRepository.deleteAll();
        boardRepository.deleteAll();
    }

    @Test
    void createUpdateAndDeleteTask_validFlow_persistsChangesAcrossLayers() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        UUID assigneeAuthId = UUID.randomUUID();
        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(true);
        when(workspaceClient.getMemberRole(workspaceId, authId)).thenReturn(new WorkspaceClient.MemberRoleResponse("OWNER"));
        when(userServiceClient.fetchProfiles(anyList())).thenReturn(List.of(
                new UserServiceClient.UserProfileResponse(assigneeAuthId, "mary", null)
        ));

        CreateBoardRequest boardRequest = new CreateBoardRequest();
        boardRequest.setName("Platform");
        boardRequest.setColor("#111827");
        BoardResponseDTO board = boardService.createBoard(workspaceId, authId, boardRequest);

        CreateTaskListRequest listRequest = new CreateTaskListRequest();
        listRequest.setName("Todo");
        TaskListResponseDTO list = taskListService.createList(workspaceId, board.getId(), authId, listRequest);

        CreateTaskRequest taskRequest = new CreateTaskRequest();
        taskRequest.setTitle("Prepare release");
        taskRequest.setAssigneeAuthId(assigneeAuthId);
        TaskResponseDTO created = taskService.createTask(workspaceId, list.getId(), authId, taskRequest);

        UpdateTaskRequest updateTaskRequest = new UpdateTaskRequest();
        updateTaskRequest.setStatus(TaskStatus.COMPLETED);
        updateTaskRequest.setCompletedByAuthId(authId);
        TaskResponseDTO updated = taskService.updateTask(workspaceId, created.getId(), authId, updateTaskRequest);

        assertThat(taskRepository.findById(created.getId())).isPresent();
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(updated.getCompletedByAuthId()).isEqualTo(authId);

        taskService.deleteTask(workspaceId, created.getId(), authId);

        assertThat(taskRepository.findById(created.getId())).isEmpty();
        verify(taskEventPublisher).publish(eq("TASK_ASSIGNED"), any(), eq(authId), eq("Platform"));
    }
}
