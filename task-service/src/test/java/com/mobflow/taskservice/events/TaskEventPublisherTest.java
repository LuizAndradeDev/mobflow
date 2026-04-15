package com.mobflow.taskservice.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.taskservice.client.UserServiceClient;
import com.mobflow.taskservice.model.entities.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;

import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.profile;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.task;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.taskList;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.board;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private UserServiceClient userServiceClient;

    private TaskEventPublisher taskEventPublisher;

    @BeforeEach
    void setUp() {
        taskEventPublisher = new TaskEventPublisher(kafkaTemplate, new ObjectMapper().findAndRegisterModules(), userServiceClient, "task-events");
    }

    @Test
    void publish_validAssignedTask_serializesAndSendsEvent() {
        UUID workspaceId = UUID.randomUUID();
        UUID actorAuthId = UUID.randomUUID();
        UUID assigneeAuthId = UUID.randomUUID();
        Task task = task(taskList(board(workspaceId)), workspaceId, actorAuthId, assigneeAuthId);

        when(userServiceClient.fetchProfiles(anyList())).thenReturn(List.of(
                profile(assigneeAuthId, "mary"),
                profile(actorAuthId, "john")
        ));

        taskEventPublisher.publish("TASK_ASSIGNED", task, actorAuthId, "Product");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("task-events"), eq(task.getId().toString()), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("TASK_ASSIGNED");
        assertThat(payloadCaptor.getValue()).contains(task.getTitle());
    }

    @Test
    void publish_profileLookupFails_doesNotPropagateOrSend() {
        UUID workspaceId = UUID.randomUUID();
        UUID actorAuthId = UUID.randomUUID();
        Task task = task(taskList(board(workspaceId)), workspaceId, actorAuthId, UUID.randomUUID());

        when(userServiceClient.fetchProfiles(anyList())).thenThrow(new RuntimeException("boom"));

        taskEventPublisher.publish("TASK_ASSIGNED", task, actorAuthId, "Product");

        verify(kafkaTemplate, never()).send(eq("task-events"), eq(task.getId().toString()), org.mockito.ArgumentMatchers.anyString());
    }
}
