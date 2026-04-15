package com.mobflow.taskservice.repository;

import com.mobflow.taskservice.model.entities.Board;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.model.enums.TaskStatus;
import com.mobflow.taskservice.testsupport.AbstractPostgresTaskServiceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.board;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.task;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.taskList;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryTest extends AbstractPostgresTaskServiceTest {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TaskListRepository taskListRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void findByListIdOrderByPositionAsc_existingTasks_returnsOrderedTasks() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board board = board(workspaceId);
        board.setId(null);
        board = boardRepository.save(board);
        TaskList list = taskList(board);
        list.setId(null);
        list = taskListRepository.save(list);
        Task first = task(list, workspaceId, authId, null);
        first.setId(null);
        first.setPosition(1);
        Task second = task(list, workspaceId, authId, null);
        second.setId(null);
        second.setPosition(0);
        taskRepository.saveAll(List.of(first, second));

        assertThat(taskRepository.findByListIdOrderByPositionAsc(list.getId()))
                .extracting(Task::getPosition)
                .containsExactly(0, 1);
    }

    @Test
    void findByWorkspaceIdAndDueDateBetween_matchingTasks_returnsSortedByDueDate() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board board = board(workspaceId);
        board.setId(null);
        board = boardRepository.save(board);
        TaskList list = taskList(board);
        list.setId(null);
        list = taskListRepository.save(list);
        Task today = task(list, workspaceId, authId, authId);
        today.setId(null);
        today.setDueDate(LocalDate.now().plusDays(1));
        Task later = task(list, workspaceId, authId, authId);
        later.setId(null);
        later.setDueDate(LocalDate.now().plusDays(2));
        taskRepository.saveAll(List.of(later, today));

        assertThat(taskRepository.findByWorkspaceIdAndDueDateBetween(workspaceId, LocalDate.now(), LocalDate.now().plusDays(3)))
                .extracting(Task::getDueDate)
                .containsExactly(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
    }

    @Test
    void findByDueDateAndStatusNot_dueSoonTasksExist_excludesCompletedTasks() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board board = board(workspaceId);
        board.setId(null);
        board = boardRepository.save(board);
        TaskList list = taskList(board);
        list.setId(null);
        list = taskListRepository.save(list);
        Task openTask = task(list, workspaceId, authId, authId);
        openTask.setId(null);
        openTask.setDueDate(LocalDate.now().plusDays(1));
        Task completedTask = task(list, workspaceId, authId, authId);
        completedTask.setId(null);
        completedTask.setDueDate(LocalDate.now().plusDays(1));
        completedTask.setStatus(TaskStatus.COMPLETED);
        taskRepository.saveAll(List.of(openTask, completedTask));

        assertThat(taskRepository.findByDueDateAndStatusNot(LocalDate.now().plusDays(1), TaskStatus.COMPLETED))
                .singleElement()
                .extracting(Task::getId)
                .isEqualTo(openTask.getId());
    }
}
