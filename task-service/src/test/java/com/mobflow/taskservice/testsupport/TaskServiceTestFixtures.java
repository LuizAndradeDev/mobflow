package com.mobflow.taskservice.testsupport;

import com.mobflow.taskservice.client.UserServiceClient;
import com.mobflow.taskservice.model.dto.request.CreateBoardRequest;
import com.mobflow.taskservice.model.dto.request.CreateTaskListRequest;
import com.mobflow.taskservice.model.dto.request.CreateTaskRequest;
import com.mobflow.taskservice.model.dto.request.MoveTaskRequest;
import com.mobflow.taskservice.model.dto.request.ReorderListsRequest;
import com.mobflow.taskservice.model.dto.request.UpdateBoardRequest;
import com.mobflow.taskservice.model.dto.request.UpdateTaskListRequest;
import com.mobflow.taskservice.model.dto.request.UpdateTaskRequest;
import com.mobflow.taskservice.model.entities.Board;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.model.enums.TaskPriority;
import com.mobflow.taskservice.model.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class TaskServiceTestFixtures {

    private TaskServiceTestFixtures() {
    }

    public static Board board(UUID workspaceId) {
        return Board.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name("Product")
                .color("#2563eb")
                .position(0)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    public static TaskList taskList(Board board) {
        return TaskList.builder()
                .id(UUID.randomUUID())
                .board(board)
                .name("Todo")
                .position(0)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    public static Task task(TaskList list, UUID workspaceId, UUID createdByAuthId, UUID assigneeAuthId) {
        return Task.builder()
                .id(UUID.randomUUID())
                .list(list)
                .workspaceId(workspaceId)
                .title("Prepare release")
                .description("Coordinate deployment")
                .priority(TaskPriority.HIGH)
                .assigneeAuthId(assigneeAuthId)
                .createdByAuthId(createdByAuthId)
                .dueDate(LocalDate.now().plusDays(2))
                .position(0)
                .status(TaskStatus.TODO)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static CreateTaskRequest createTaskRequest(UUID assigneeAuthId) {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Prepare release");
        request.setDescription("Coordinate deployment");
        request.setPriority(TaskPriority.HIGH);
        request.setAssigneeAuthId(assigneeAuthId);
        request.setDueDate(LocalDate.now().plusDays(2));
        return request;
    }

    public static UpdateTaskRequest updateTaskRequest() {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Prepare hotfix release");
        request.setDescription("Coordinate deployment and validation");
        request.setPriority(TaskPriority.MEDIUM);
        request.setDueDate(LocalDate.now().plusDays(3));
        return request;
    }

    public static MoveTaskRequest moveTaskRequest(UUID targetListId, int position) {
        MoveTaskRequest request = new MoveTaskRequest();
        request.setTargetListId(targetListId);
        request.setPosition(position);
        return request;
    }

    public static CreateBoardRequest createBoardRequest() {
        CreateBoardRequest request = new CreateBoardRequest();
        request.setName("Platform");
        request.setColor("#111827");
        return request;
    }

    public static UpdateBoardRequest updateBoardRequest() {
        UpdateBoardRequest request = new UpdateBoardRequest();
        request.setName("Platform Ops");
        request.setColor("#0f766e");
        return request;
    }

    public static CreateTaskListRequest createTaskListRequest() {
        CreateTaskListRequest request = new CreateTaskListRequest();
        request.setName("Doing");
        return request;
    }

    public static UpdateTaskListRequest updateTaskListRequest() {
        UpdateTaskListRequest request = new UpdateTaskListRequest();
        request.setName("Review");
        return request;
    }

    public static ReorderListsRequest reorderListsRequest(List<UUID> orderedIds) {
        ReorderListsRequest request = new ReorderListsRequest();
        request.setOrderedIds(orderedIds);
        return request;
    }

    public static UserServiceClient.UserProfileResponse profile(UUID authId, String displayName) {
        return new UserServiceClient.UserProfileResponse(authId, displayName, "https://cdn.mobflow.test/" + displayName + ".png");
    }
}
