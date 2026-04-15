package com.mobflow.workspaceservice.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.workspaceservice.config.UserServiceClient;
import com.mobflow.workspaceservice.model.entities.Workspace;
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
public class WorkspaceEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final UserServiceClient userServiceClient;
    private final String topicName;

    public WorkspaceEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            UserServiceClient userServiceClient,
            @Value("${app.kafka.topics.workspace}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.userServiceClient = userServiceClient;
        this.topicName = topicName;
    }

    public void publish(String eventType, UUID recipientId, UUID actorAuthId, Workspace workspace, String inviteId, String role) {
        if (recipientId == null) {
            return;
        }

        try {
            Map<UUID, UserServiceClient.UserProfileResponse> profiles = userServiceClient
                    .fetchProfilesBatch(List.of(recipientId, actorAuthId).stream().distinct().toList());

            WorkspaceNotificationEvent event = new WorkspaceNotificationEvent(
                    eventType,
                    recipientId.toString(),
                    null,
                    profiles.containsKey(recipientId) ? profiles.get(recipientId).displayName() : null,
                    actorAuthId != null ? actorAuthId.toString() : null,
                    actorAuthId != null && profiles.containsKey(actorAuthId) ? profiles.get(actorAuthId).displayName() : null,
                    workspace.getId().toString(),
                    workspace.getName(),
                    inviteId,
                    role,
                    Instant.now()
            );

            kafkaTemplate.send(topicName, workspace.getId().toString(), objectMapper.writeValueAsString(event));
        } catch (Exception ignored) {
        }
    }
}
