package com.mobflow.taskservice.service;

import com.mobflow.taskservice.client.UserServiceClient;
import com.mobflow.taskservice.client.WorkspaceClient;
import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.model.dto.request.CreateTaskRequest;
import com.mobflow.taskservice.model.dto.request.MoveTaskRequest;
import com.mobflow.taskservice.model.dto.request.UpdateTaskRequest;
import com.mobflow.taskservice.model.dto.response.TaskResponseDTO;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.repository.BoardRepository;
import com.mobflow.taskservice.repository.TaskListRepository;
import com.mobflow.taskservice.repository.TaskRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskListRepository taskListRepository;
    private final BoardRepository boardRepository;
    private final WorkspaceClient workspaceClient;
    private final UserServiceClient userServiceClient;

    public TaskService(
            TaskRepository taskRepository,
            TaskListRepository taskListRepository,
            BoardRepository boardRepository,
            WorkspaceClient workspaceClient,
            UserServiceClient userServiceClient
    ) {
        this.taskRepository = taskRepository;
        this.taskListRepository = taskListRepository;
        this.boardRepository = boardRepository;
        this.workspaceClient = workspaceClient;
        this.userServiceClient = userServiceClient;
    }

    public List<TaskResponseDTO> listTasksByList(UUID listId) {
        List<Task> tasks = taskRepository.findByListIdOrderByPositionAsc(listId);
        return enrichWithProfiles(tasks);
    }

    public TaskResponseDTO getTask(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(TaskServiceException::taskNotFound);
        return enrichSingle(task);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public TaskResponseDTO createTask(UUID workspaceId, UUID listId, UUID authId, CreateTaskRequest request) {
        // All workspace members can create tasks — only validate membership
        workspaceClient.getMemberRole(workspaceId, authId);

        TaskList list = taskListRepository.findById(listId)
                .orElseThrow(TaskServiceException::listNotFound);

        // Ensure the list belongs to the given workspace
        if (!list.getBoard().getWorkspaceId().equals(workspaceId)) {
            throw TaskServiceException.accessDenied();
        }

        int position = taskRepository.countByListId(listId);

        Task task = Task.create(list, workspaceId, request.getTitle(),
                request.getPriority() != null ? request.getPriority() : com.mobflow.taskservice.model.enums.TaskPriority.MEDIUM,
                authId);
        task.setDescription(request.getDescription());
        task.setAssigneeAuthId(request.getAssigneeAuthId());
        task.setDueDate(request.getDueDate());
        task.setPosition(position);

        taskRepository.save(task);
        return enrichSingle(task);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public TaskResponseDTO updateTask(UUID workspaceId, UUID taskId, UUID authId, UpdateTaskRequest request) {
        workspaceClient.getMemberRole(workspaceId, authId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(TaskServiceException::taskNotFound);

        if (!task.getWorkspaceId().equals(workspaceId)) {
            throw TaskServiceException.accessDenied();
        }

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getAssigneeAuthId() != null) task.setAssigneeAuthId(request.getAssigneeAuthId());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());

        taskRepository.save(task);
        return enrichSingle(task);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public TaskResponseDTO moveTask(UUID workspaceId, UUID taskId, UUID authId, MoveTaskRequest request) {
        // Moving tasks requires OWNER or ADMIN
        if (!workspaceClient.isOwnerOrAdmin(workspaceId, authId)) {
            throw TaskServiceException.accessDenied();
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(TaskServiceException::taskNotFound);

        if (!task.getWorkspaceId().equals(workspaceId)) {
            throw TaskServiceException.accessDenied();
        }

        TaskList targetList = taskListRepository.findById(request.getTargetListId())
                .orElseThrow(TaskServiceException::listNotFound);

        if (!targetList.getBoard().getWorkspaceId().equals(workspaceId)) {
            throw TaskServiceException.accessDenied();
        }

        // Shift existing tasks in the target list to make room
        List<Task> targetTasks = taskRepository.findByListIdOrderByPositionAsc(targetList.getId());
        for (Task t : targetTasks) {
            if (t.getPosition() >= request.getPosition() && !t.getId().equals(taskId)) {
                t.setPosition(t.getPosition() + 1);
            }
        }
        taskRepository.saveAll(targetTasks);

        task.setList(targetList);
        task.setPosition(request.getPosition());
        taskRepository.save(task);

        return enrichSingle(task);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public void deleteTask(UUID workspaceId, UUID taskId, UUID authId) {
        if (!workspaceClient.isOwnerOrAdmin(workspaceId, authId)) {
            throw TaskServiceException.accessDenied();
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(TaskServiceException::taskNotFound);

        if (!task.getWorkspaceId().equals(workspaceId)) {
            throw TaskServiceException.accessDenied();
        }

        taskRepository.delete(task);
    }

    // -----------------------------------------------------------------------
    // Profile enrichment helpers
    // -----------------------------------------------------------------------

    private TaskResponseDTO enrichSingle(Task task) {
        if (task.getAssigneeAuthId() == null) return TaskResponseDTO.fromEntity(task);

        Map<UUID, UserServiceClient.UserProfileResponse> profileMap =
                resolveProfiles(List.of(task));

        UserServiceClient.UserProfileResponse profile = profileMap.get(task.getAssigneeAuthId());
        return TaskResponseDTO.fromEntity(task,
                profile != null ? profile.displayName() : null,
                profile != null ? profile.avatarUrl() : null);
    }

    private List<TaskResponseDTO> enrichWithProfiles(List<Task> tasks) {
        Map<UUID, UserServiceClient.UserProfileResponse> profileMap = resolveProfiles(tasks);
        return tasks.stream().map(task -> {
            if (task.getAssigneeAuthId() == null) return TaskResponseDTO.fromEntity(task);
            UserServiceClient.UserProfileResponse p = profileMap.get(task.getAssigneeAuthId());
            return TaskResponseDTO.fromEntity(task,
                    p != null ? p.displayName() : null,
                    p != null ? p.avatarUrl() : null);
        }).toList();
    }

    private Map<UUID, UserServiceClient.UserProfileResponse> resolveProfiles(List<Task> tasks) {
        List<UUID> ids = tasks.stream()
                .map(Task::getAssigneeAuthId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (ids.isEmpty()) return Map.of();

        return userServiceClient.fetchProfiles(ids).stream()
                .collect(Collectors.toMap(UserServiceClient.UserProfileResponse::authId, p -> p));
    }
}
