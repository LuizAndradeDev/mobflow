package com.mobflow.workspaceservice.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.workspaceservice.config.UserServiceClient;
import com.mobflow.workspaceservice.model.entities.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.UUID;

import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.workspace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private UserServiceClient userServiceClient;

    private WorkspaceEventPublisher workspaceEventPublisher;

    @BeforeEach
    void setUp() {
        workspaceEventPublisher = new WorkspaceEventPublisher(
                kafkaTemplate,
                new ObjectMapper().findAndRegisterModules(),
                userServiceClient,
                "workspace-events"
        );
    }

    @Test
    void publish_validPayload_sendsSerializedEventToKafka() {
        UUID recipientId = UUID.randomUUID();
        UUID actorAuthId = UUID.randomUUID();
        Workspace workspace = workspace(actorAuthId);
        when(userServiceClient.fetchProfilesBatch(any()))
                .thenReturn(Map.of(
                        recipientId, new UserServiceClient.UserProfileResponse(recipientId, "mary", null),
                        actorAuthId, new UserServiceClient.UserProfileResponse(actorAuthId, "john", null)
                ));

        workspaceEventPublisher.publish("WORKSPACE_INVITE", recipientId, actorAuthId, recipientId, workspace, "invite-1", "MEMBER");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("workspace-events"), eq(workspace.getId().toString()), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("WORKSPACE_INVITE");
        assertThat(payloadCaptor.getValue()).contains("invite-1");
    }

    @Test
    void publish_profileLookupFails_doesNotThrowOrSendPartialFailure() {
        UUID recipientId = UUID.randomUUID();
        Workspace workspace = workspace(UUID.randomUUID());
        when(userServiceClient.fetchProfilesBatch(any())).thenThrow(new RuntimeException("boom"));

        workspaceEventPublisher.publish("WORKSPACE_INVITE", recipientId, UUID.randomUUID(), recipientId, workspace, "invite-1", "MEMBER");

        verify(kafkaTemplate, never()).send(eq("workspace-events"), any(), any());
    }
}
