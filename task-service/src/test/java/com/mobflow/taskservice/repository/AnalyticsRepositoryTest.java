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
class AnalyticsRepositoryTest extends AbstractPostgresTaskServiceTest {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TaskListRepository taskListRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Test
    void countOverdueTasksByWorkspaceAndAssignee_overdueTasksExist_returnsMatchingCount() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board board = board(workspaceId);
        board.setId(null);
        board = boardRepository.save(board);
        TaskList list = taskList(board);
        list.setId(null);
        list = taskListRepository.save(list);
        Task overdue = task(list, workspaceId, authId, authId);
        overdue.setId(null);
        overdue.setDueDate(LocalDate.now().minusDays(1));
        Task completed = task(list, workspaceId, authId, authId);
        completed.setId(null);
        completed.setDueDate(LocalDate.now().minusDays(1));
        completed.setStatus(TaskStatus.COMPLETED);
        taskRepository.saveAll(List.of(overdue, completed));

        long count = analyticsRepository.countOverdueTasksByWorkspaceAndAssignee(
                workspaceId,
                authId,
                LocalDate.now(),
                TaskStatus.COMPLETED
        );

        assertThat(count).isEqualTo(1L);
    }
}
