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
import static com.mobflow.notificationservice.testsupport.NotificationTestFixtures.taskEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskEventConsumerTest {

    @Mock
    private NotificationFactory notificationFactory;

    @Mock
    private NotificationService notificationService;

    private TaskEventConsumer taskEventConsumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        taskEventConsumer = new TaskEventConsumer(objectMapper, notificationFactory, notificationService);
    }

    @Test
    void consume_supportedTaskPayload_persistsNotification() throws Exception {
        Notification notification = notification("user-1");
        when(notificationFactory.createTaskNotification(any())).thenReturn(notification);

        taskEventConsumer.consume(objectMapper.writeValueAsString(taskEvent("TASK_ASSIGNED")));

        verify(notificationService).save(notification);
    }

    @Test
    void consume_unknownTaskPayload_skipsPersistence() throws Exception {
        when(notificationFactory.createTaskNotification(any())).thenReturn(null);

        taskEventConsumer.consume(objectMapper.writeValueAsString(taskEvent("UNKNOWN")));

        verify(notificationService, never()).save(any());
    }
}
