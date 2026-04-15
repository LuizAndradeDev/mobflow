package com.mobflow.notificationservice.kafka.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mobflow.notificationservice.model.entities.Notification;
import com.mobflow.notificationservice.service.NotificationFactory;
import com.mobflow.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mobflow.notificationservice.testsupport.NotificationTestFixtures.notification;
import static com.mobflow.notificationservice.testsupport.NotificationTestFixtures.workspaceEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceEventConsumerTest {

    @Mock
    private NotificationFactory notificationFactory;

    @Mock
    private NotificationService notificationService;

    private WorkspaceEventConsumer workspaceEventConsumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        workspaceEventConsumer = new WorkspaceEventConsumer(objectMapper, notificationFactory, notificationService);
    }

    @Test
    void consume_supportedWorkspacePayload_persistsNotification() throws Exception {
        Notification notification = notification("user-1");
        when(notificationFactory.createWorkspaceNotification(any())).thenReturn(notification);

        workspaceEventConsumer.consume(objectMapper.writeValueAsString(workspaceEvent("WORKSPACE_INVITE")));

        verify(notificationService).save(notification);
    }
}
