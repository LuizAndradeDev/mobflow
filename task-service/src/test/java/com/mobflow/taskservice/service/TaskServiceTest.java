package com.mobflow.taskservice.service;

import com.mobflow.taskservice.client.UserServiceClient;
import com.mobflow.taskservice.client.WorkspaceClient;
import com.mobflow.taskservice.events.TaskEventPublisher;
import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.model.dto.request.MoveTaskRequest;
import com.mobflow.taskservice.model.dto.request.UpdateTaskRequest;
import com.mobflow.taskservice.model.dto.response.TaskResponseDTO;
import com.mobflow.taskservice.model.entities.Board;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.model.enums.TaskStatus;
import com.mobflow.taskservice.repository.TaskListRepository;
import com.mobflow.taskservice.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.createTaskRequest;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.profile;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.task;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.taskList;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.updateTaskRequest;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.board;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskListRepository taskListRepository;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private TaskEventPublisher taskEventPublisher;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, taskListRepository, workspaceClient, userServiceClient, taskEventPublisher);
    }

    @Test
    void createTask_memberRequestWithAssignee_persistsTaskAndPublishesAssignmentEvent() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        UUID assigneeAuthId = UUID.randomUUID();
        Board board = board(workspaceId);
        TaskList list = taskList(board);

        when(workspaceClient.getMemberRole(workspaceId, authId)).thenReturn(new WorkspaceClient.MemberRoleResponse("MEMBER"));
        when(taskListRepository.findById(list.getId())).thenReturn(Optional.of(list));
        when(taskRepository.countByListId(list.getId())).thenReturn(2);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userServiceClient.fetchProfiles(List.of(assigneeAuthId))).thenReturn(List.of(profile(assigneeAuthId, "mary")));

        TaskResponseDTO response = taskService.createTask(workspaceId, list.getId(), authId, createTaskRequest(assigneeAuthId));

        assertThat(response.getTitle()).isEqualTo("Prepare release");
        assertThat(response.getPosition()).isEqualTo(2);
        assertThat(response.getAssigneeDisplayName()).isEqualTo("mary");
        verify(taskEventPublisher).publish(eq("TASK_ASSIGNED"), any(Task.class), eq(authId), eq(list.getBoard().getName()));
    }

    @Test
    void createTask_listBelongsToDifferentWorkspace_throwsAccessDenied() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        TaskList list = taskList(board(UUID.randomUUID()));

        when(workspaceClient.getMemberRole(workspaceId, authId)).thenReturn(new WorkspaceClient.MemberRoleResponse("MEMBER"));
        when(taskListRepository.findById(list.getId())).thenReturn(Optional.of(list));

        assertThatThrownBy(() -> taskService.createTask(workspaceId, list.getId(), authId, createTaskRequest(UUID.randomUUID())))
                .isInstanceOf(TaskServiceException.class);
    }

    @Test
    void updateTask_statusCompleted_setsCompletionFieldsAndPublishesCompletedEvent() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        UUID assigneeAuthId = UUID.randomUUID();
        Task task = task(taskList(board(workspaceId)), workspaceId, authId, assigneeAuthId);
        UpdateTaskRequest request = updateTaskRequest();
        request.setStatus(TaskStatus.COMPLETED);
        request.setCompletedByAuthId(authId);

        when(workspaceClient.getMemberRole(workspaceId, authId)).thenReturn(new WorkspaceClient.MemberRoleResponse("MEMBER"));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(userServiceClient.fetchProfiles(List.of(assigneeAuthId))).thenReturn(List.of(profile(assigneeAuthId, "mary")));

        TaskResponseDTO response = taskService.updateTask(workspaceId, task.getId(), authId, request);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();
        assertThat(task.getCompletedByAuthId()).isEqualTo(authId);
        verify(taskEventPublisher).publish("TASK_COMPLETED", task, authId, task.getList().getBoard().getName());
    }

    @Test
    void updateTask_assigneeChanged_publishesAssignedEvent() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        UUID previousAssignee = UUID.randomUUID();
        UUID newAssignee = UUID.randomUUID();
        Task task = task(taskList(board(workspaceId)), workspaceId, authId, previousAssignee);
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssigneeAuthId(newAssignee);

        when(workspaceClient.getMemberRole(workspaceId, authId)).thenReturn(new WorkspaceClient.MemberRoleResponse("MEMBER"));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(userServiceClient.fetchProfiles(List.of(newAssignee))).thenReturn(List.of(profile(newAssignee, "sarah")));

        TaskResponseDTO response = taskService.updateTask(workspaceId, task.getId(), authId, request);

        assertThat(response.getAssigneeAuthId()).isEqualTo(newAssignee);
        verify(taskEventPublisher).publish("TASK_ASSIGNED", task, authId, task.getList().getBoard().getName());
    }

    @Test
    void moveTask_requesterIsNotAdminOrOwner_throwsAccessDenied() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        MoveTaskRequest request = new MoveTaskRequest();
        request.setTargetListId(UUID.randomUUID());
        request.setPosition(1);

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(false);

        assertThatThrownBy(() -> taskService.moveTask(workspaceId, UUID.randomUUID(), authId, request))
                .isInstanceOf(TaskServiceException.class);
    }

    @Test
    void moveTask_validRequest_updatesTargetListAndReordersTargetTasks() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board currentBoard = board(workspaceId);
        Board targetBoard = board(workspaceId);
        TaskList currentList = taskList(currentBoard);
        TaskList targetList = taskList(targetBoard);
        Task task = task(currentList, workspaceId, authId, null);
        Task existing = task(targetList, workspaceId, authId, null);
        existing.setPosition(1);
        MoveTaskRequest request = com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.moveTaskRequest(targetList.getId(), 1);

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(true);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskListRepository.findById(targetList.getId())).thenReturn(Optional.of(targetList));
        when(taskRepository.findByListIdOrderByPositionAsc(currentList.getId())).thenReturn(List.of(task));
        when(taskRepository.findByListIdOrderByPositionAsc(targetList.getId())).thenReturn(List.of(existing));

        TaskResponseDTO response = taskService.moveTask(workspaceId, task.getId(), authId, request);

        assertThat(response.getListId()).isEqualTo(targetList.getId());
        assertThat(existing.getPosition()).isEqualTo(0);
        assertThat(task.getPosition()).isEqualTo(1);
        verify(taskRepository).saveAll(List.of());
        verify(taskRepository).saveAll(List.of(existing, task));
    }

    @Test
    void moveTask_sameListMoveToLastPosition_reordersSequentially() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board board = board(workspaceId);
        TaskList list = taskList(board);
        Task first = task(list, workspaceId, authId, null);
        first.setPosition(0);
        Task second = task(list, workspaceId, authId, null);
        second.setPosition(1);
        Task third = task(list, workspaceId, authId, null);
        third.setPosition(2);
        MoveTaskRequest request = com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.moveTaskRequest(list.getId(), 2);

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(true);
        when(taskRepository.findById(first.getId())).thenReturn(Optional.of(first));
        when(taskListRepository.findById(list.getId())).thenReturn(Optional.of(list));
        when(taskRepository.findByListIdOrderByPositionAsc(list.getId())).thenReturn(List.of(first, second, third));

        TaskResponseDTO response = taskService.moveTask(workspaceId, first.getId(), authId, request);

        assertThat(response.getPosition()).isEqualTo(2);
        assertThat(second.getPosition()).isEqualTo(0);
        assertThat(third.getPosition()).isEqualTo(1);
        assertThat(first.getPosition()).isEqualTo(2);
        verify(taskRepository).saveAll(List.of(first, second, third));
    }

    @Test
    void moveTask_acrossLists_normalizesSourceAndTargetPositions() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board board = board(workspaceId);
        TaskList sourceList = taskList(board);
        TaskList targetList = taskList(board);
        Task moving = task(sourceList, workspaceId, authId, null);
        moving.setPosition(1);
        Task sourceFirst = task(sourceList, workspaceId, authId, null);
        sourceFirst.setPosition(0);
        Task sourceThird = task(sourceList, workspaceId, authId, null);
        sourceThird.setPosition(2);
        Task targetFirst = task(targetList, workspaceId, authId, null);
        targetFirst.setPosition(0);
        MoveTaskRequest request = com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.moveTaskRequest(targetList.getId(), 1);

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(true);
        when(taskRepository.findById(moving.getId())).thenReturn(Optional.of(moving));
        when(taskListRepository.findById(targetList.getId())).thenReturn(Optional.of(targetList));
        when(taskRepository.findByListIdOrderByPositionAsc(sourceList.getId())).thenReturn(List.of(sourceFirst, moving, sourceThird));
        when(taskRepository.findByListIdOrderByPositionAsc(targetList.getId())).thenReturn(List.of(targetFirst));

        TaskResponseDTO response = taskService.moveTask(workspaceId, moving.getId(), authId, request);

        assertThat(response.getListId()).isEqualTo(targetList.getId());
        assertThat(sourceFirst.getPosition()).isEqualTo(0);
        assertThat(sourceThird.getPosition()).isEqualTo(1);
        assertThat(targetFirst.getPosition()).isEqualTo(0);
        assertThat(moving.getPosition()).isEqualTo(1);
        verify(taskRepository).saveAll(List.of(sourceFirst, sourceThird));
        verify(taskRepository).saveAll(List.of(targetFirst, moving));
    }

    @Test
    void deleteTask_taskHasAssignee_publishesDeleteEventAndDeletesEntity() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Task task = task(taskList(board(workspaceId)), workspaceId, authId, UUID.randomUUID());

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(true);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        taskService.deleteTask(workspaceId, task.getId(), authId);

        verify(taskEventPublisher).publish("TASK_DELETED", task, authId, task.getList().getBoard().getName());
        verify(taskRepository).delete(task);
    }

    @Test
    void listTasksByList_assigneeProfilesAvailable_enrichesResponse() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        UUID assigneeAuthId = UUID.randomUUID();
        Task task = task(taskList(board(workspaceId)), workspaceId, authId, assigneeAuthId);

        when(taskRepository.findByListIdOrderByPositionAsc(task.getList().getId())).thenReturn(List.of(task));
        when(userServiceClient.fetchProfiles(List.of(assigneeAuthId))).thenReturn(List.of(profile(assigneeAuthId, "mary")));

        List<TaskResponseDTO> responses = taskService.listTasksByList(task.getList().getId());

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.getAssigneeDisplayName()).isEqualTo("mary");
            assertThat(response.getTitle()).isEqualTo("Prepare release");
        });
    }

    @Test
    void getTask_withoutAssignee_returnsEntityMappingWithoutProfileLookup() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Task task = task(taskList(board(workspaceId)), workspaceId, authId, null);
        task.setDueDate(LocalDate.now().plusDays(1));

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        TaskResponseDTO response = taskService.getTask(task.getId());

        assertThat(response.getAssigneeDisplayName()).isNull();
        verify(userServiceClient, never()).fetchProfiles(any());
    }
}
