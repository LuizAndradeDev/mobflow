package com.mobflow.taskservice.service;

import com.mobflow.taskservice.client.WorkspaceClient;
import com.mobflow.taskservice.events.TaskEventPublisher;
import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.model.dto.request.CreateTaskRequest;
import com.mobflow.taskservice.model.dto.request.MoveTaskRequest;
import com.mobflow.taskservice.model.dto.request.UpdateTaskRequest;
import com.mobflow.taskservice.model.dto.response.TaskResponseDTO;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.repository.TaskListRepository;
import com.mobflow.taskservice.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskListRepository taskListRepository;
    private final WorkspaceClient workspaceClient;
    private final TaskEventPublisher taskEventPublisher;
    private final TaskProfileService taskProfileService;

    @Autowired
    public TaskService(
            TaskRepository taskRepository,
            TaskListRepository taskListRepository,
            WorkspaceClient workspaceClient,
            TaskEventPublisher taskEventPublisher,
            TaskProfileService taskProfileService
    ) {
        this.taskRepository = taskRepository;
        this.taskListRepository = taskListRepository;
        this.workspaceClient = workspaceClient;
        this.taskEventPublisher = taskEventPublisher;
        this.taskProfileService = taskProfileService;
    }

    public TaskService(
            TaskRepository taskRepository,
            TaskListRepository taskListRepository,
            WorkspaceClient workspaceClient,
            com.mobflow.taskservice.client.UserServiceClient userServiceClient,
            TaskEventPublisher taskEventPublisher
    ) {
        this(
                taskRepository,
                taskListRepository,
                workspaceClient,
                taskEventPublisher,
                new TaskProfileService(userServiceClient)
        );
    }

    public List<TaskResponseDTO> listTasksByList(UUID listId) {
        return taskProfileService.toTaskResponses(taskRepository.findByListIdOrderByPositionAsc(listId));
    }

    public TaskResponseDTO getTask(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(TaskServiceException::taskNotFound);
        return taskProfileService.toTaskResponse(task);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public TaskResponseDTO createTask(UUID workspaceId, UUID listId, UUID authId, CreateTaskRequest request) {
        workspaceClient.getMemberRole(workspaceId, authId);

        TaskList list = taskListRepository.findById(listId)
                .orElseThrow(TaskServiceException::listNotFound);

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
        if (task.getAssigneeAuthId() != null) {
            taskEventPublisher.publish("TASK_CREATED", task, authId, list.getBoard().getName());
            if (!task.getAssigneeAuthId().equals(authId)) {
                taskEventPublisher.publish("TASK_ASSIGNED", task, authId, list.getBoard().getName());
            }
        }
        return taskProfileService.toTaskResponse(task);
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
        UUID previousAssigneeAuthId = task.getAssigneeAuthId();
        if (request.getAssigneeAuthId() != null) task.setAssigneeAuthId(request.getAssigneeAuthId());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
            if (request.getStatus() == com.mobflow.taskservice.model.enums.TaskStatus.COMPLETED && task.getCompletedAt() == null) {
                task.setCompletedAt(java.time.LocalDateTime.now());
            } else if (request.getStatus() != com.mobflow.taskservice.model.enums.TaskStatus.COMPLETED) {
                task.setCompletedAt(null);
                task.setCompletedByAuthId(null);
            }
        }
        if (request.getCompletedByAuthId() != null) task.setCompletedByAuthId(request.getCompletedByAuthId());

        taskRepository.save(task);
        if (task.getStatus() == com.mobflow.taskservice.model.enums.TaskStatus.COMPLETED && task.getAssigneeAuthId() != null && !task.getAssigneeAuthId().equals(authId)) {
            taskEventPublisher.publish("TASK_COMPLETED", task, authId, task.getList().getBoard().getName());
        } else if (task.getAssigneeAuthId() != null && !task.getAssigneeAuthId().equals(previousAssigneeAuthId)) {
            if (!task.getAssigneeAuthId().equals(authId)) {
                taskEventPublisher.publish("TASK_ASSIGNED", task, authId, task.getList().getBoard().getName());
            }
        } else if (task.getAssigneeAuthId() != null && !task.getAssigneeAuthId().equals(authId)) {
            taskEventPublisher.publish("TASK_UPDATED", task, authId, task.getList().getBoard().getName());
        }
        return taskProfileService.toTaskResponse(task);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public TaskResponseDTO moveTask(UUID workspaceId, UUID taskId, UUID authId, MoveTaskRequest request) {
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

        List<Task> targetTasks = taskRepository.findByListIdOrderByPositionAsc(targetList.getId());
        for (Task targetTask : targetTasks) {
            if (targetTask.getPosition() >= request.getPosition() && !targetTask.getId().equals(taskId)) {
                targetTask.setPosition(targetTask.getPosition() + 1);
            }
        }
        taskRepository.saveAll(targetTasks);

        task.setList(targetList);
        task.setPosition(request.getPosition());
        taskRepository.save(task);

        return taskProfileService.toTaskResponse(task);
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

        if (task.getAssigneeAuthId() != null) {
            taskEventPublisher.publish("TASK_DELETED", task, authId, task.getList().getBoard().getName());
        }
        taskRepository.delete(task);
    }

}
