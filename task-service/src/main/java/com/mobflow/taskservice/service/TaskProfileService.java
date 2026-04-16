package com.mobflow.taskservice.service;

import com.mobflow.taskservice.client.UserServiceClient;
import com.mobflow.taskservice.model.dto.response.TaskResponseDTO;
import com.mobflow.taskservice.model.entities.Task;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskProfileService {

    private final UserServiceClient userServiceClient;

    public TaskProfileService(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    public List<TaskResponseDTO> toTaskResponses(List<Task> tasks) {
        Map<UUID, UserServiceClient.UserProfileResponse> profilesByAuthId = fetchProfilesByAuthId(tasks);
        return tasks.stream()
                .map(task -> toTaskResponse(task, profilesByAuthId))
                .toList();
    }

    public TaskResponseDTO toTaskResponse(Task task) {
        return toTaskResponse(task, fetchProfilesByAuthId(List.of(task)));
    }

    public Map<UUID, String> avatarUrlsByAuthId(List<Task> tasks) {
        return fetchProfilesByAuthId(tasks).values().stream()
                .collect(Collectors.toMap(
                        UserServiceClient.UserProfileResponse::authId,
                        profile -> profile.avatarUrl() == null ? "" : profile.avatarUrl()
                ));
    }

    private TaskResponseDTO toTaskResponse(
            Task task,
            Map<UUID, UserServiceClient.UserProfileResponse> profilesByAuthId
    ) {
        UUID assigneeAuthId = task.getAssigneeAuthId();
        if (assigneeAuthId == null) {
            return TaskResponseDTO.fromEntity(task);
        }

        UserServiceClient.UserProfileResponse profile = profilesByAuthId.get(assigneeAuthId);
        if (profile == null) {
            return TaskResponseDTO.fromEntity(task);
        }

        return TaskResponseDTO.fromEntity(task, profile.displayName(), profile.avatarUrl());
    }

    private Map<UUID, UserServiceClient.UserProfileResponse> fetchProfilesByAuthId(List<Task> tasks) {
        List<UUID> assigneeIds = tasks.stream()
                .map(Task::getAssigneeAuthId)
                .filter(assigneeId -> assigneeId != null)
                .distinct()
                .toList();

        if (assigneeIds.isEmpty()) {
            return Map.of();
        }

        return userServiceClient.fetchProfiles(assigneeIds).stream()
                .collect(Collectors.toMap(UserServiceClient.UserProfileResponse::authId, Function.identity()));
    }
}
