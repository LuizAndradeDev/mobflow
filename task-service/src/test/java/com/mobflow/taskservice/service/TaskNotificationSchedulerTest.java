package com.mobflow.taskservice.service;

import com.mobflow.taskservice.events.TaskEventPublisher;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.enums.TaskStatus;
import com.mobflow.taskservice.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.board;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.task;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.taskList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskNotificationSchedulerTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskEventPublisher taskEventPublisher;

    private TaskNotificationScheduler taskNotificationScheduler;

    @BeforeEach
    void setUp() {
        taskNotificationScheduler = new TaskNotificationScheduler(taskRepository, taskEventPublisher);
    }

    @Test
    void publishDueSoonEvents_tasksExistForTomorrow_publishesDueSoonEventForEachTask() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Task task = task(taskList(board(workspaceId)), workspaceId, authId, UUID.randomUUID());
        task.setDueDate(LocalDate.now().plusDays(1));

        when(taskRepository.findByDueDateAndStatusNot(LocalDate.now().plusDays(1), TaskStatus.COMPLETED)).thenReturn(List.of(task));

        taskNotificationScheduler.publishDueSoonEvents();

        verify(taskEventPublisher).publish("TASK_DUE_SOON", task, authId, task.getList().getBoard().getName());
    }
}
