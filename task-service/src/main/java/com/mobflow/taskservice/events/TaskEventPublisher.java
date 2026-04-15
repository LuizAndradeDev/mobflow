package com.mobflow.taskservice.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.taskservice.client.UserServiceClient;
import com.mobflow.taskservice.model.entities.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TaskEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final UserServiceClient userServiceClient;
    private final String topicName;

    public TaskEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            UserServiceClient userServiceClient,
            @Value("${app.kafka.topics.task}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.userServiceClient = userServiceClient;
        this.topicName = topicName;
    }

    public void publish(String eventType, Task task, UUID actorAuthId, String workspaceName) {
        if (task.getAssigneeAuthId() == null) {
            return;
        }

        try {
            Map<UUID, UserServiceClient.UserProfileResponse> profiles = userServiceClient
                    .fetchProfiles(List.of(task.getAssigneeAuthId(), actorAuthId).stream().distinct().toList())
                    .stream()
                    .collect(Collectors.toMap(UserServiceClient.UserProfileResponse::authId, Function.identity()));

            UserServiceClient.UserProfileResponse assignee = profiles.get(task.getAssigneeAuthId());
            UserServiceClient.UserProfileResponse actor = profiles.get(actorAuthId);

            TaskNotificationEvent event = new TaskNotificationEvent(
                    eventType,
                    task.getAssigneeAuthId().toString(),
                    null,
                    assignee != null ? assignee.displayName() : null,
                    actorAuthId != null ? actorAuthId.toString() : null,
                    actor != null ? actor.displayName() : null,
                    task.getWorkspaceId().toString(),
                    workspaceName,
                    task.getId().toString(),
                    task.getTitle(),
                    task.getStatus().name(),
                    task.getDueDate(),
                    Instant.now()
            );

            kafkaTemplate.send(topicName, task.getId().toString(), objectMapper.writeValueAsString(event));
        } catch (Exception ignored) {
        }
    }
}
