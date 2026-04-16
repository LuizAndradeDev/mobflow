package com.mobflow.taskservice.service;

import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.model.dto.response.TaskCommentContextResponseDTO;
import com.mobflow.taskservice.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TaskContextService {

    private final TaskRepository taskRepository;

    public TaskContextService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskCommentContextResponseDTO getTaskCommentContext(UUID taskId) {
        return taskRepository.findTaskContextById(taskId)
                .map(TaskCommentContextResponseDTO::fromEntity)
                .orElseThrow(TaskServiceException::taskNotFound);
    }
}
