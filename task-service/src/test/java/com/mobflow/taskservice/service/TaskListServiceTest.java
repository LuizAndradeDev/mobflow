package com.mobflow.taskservice.service;

import com.mobflow.taskservice.client.WorkspaceClient;
import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.model.dto.response.TaskListResponseDTO;
import com.mobflow.taskservice.model.entities.Board;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.repository.BoardRepository;
import com.mobflow.taskservice.repository.TaskListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.board;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.createTaskListRequest;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.reorderListsRequest;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.taskList;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.updateTaskListRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskListServiceTest {

    @Mock
    private TaskListRepository taskListRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private WorkspaceClient workspaceClient;

    private TaskListService taskListService;

    @BeforeEach
    void setUp() {
        taskListService = new TaskListService(taskListRepository, boardRepository, workspaceClient);
    }

    @Test
    void createList_adminRequest_persistsListAtNextPosition() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board board = board(workspaceId);

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(true);
        when(boardRepository.findByIdAndWorkspaceId(board.getId(), workspaceId)).thenReturn(Optional.of(board));
        when(taskListRepository.countByBoardId(board.getId())).thenReturn(2);
        when(taskListRepository.save(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskListResponseDTO response = taskListService.createList(workspaceId, board.getId(), authId, createTaskListRequest());

        assertThat(response.getPosition()).isEqualTo(2);
        assertThat(response.getName()).isEqualTo("Doing");
    }

    @Test
    void updateList_existingList_updatesName() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board board = board(workspaceId);
        TaskList list = taskList(board);

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(true);
        when(boardRepository.findByIdAndWorkspaceId(board.getId(), workspaceId)).thenReturn(Optional.of(board));
        when(taskListRepository.findByIdAndBoardId(list.getId(), board.getId())).thenReturn(Optional.of(list));
        when(taskListRepository.save(list)).thenReturn(list);

        TaskListResponseDTO response = taskListService.updateList(workspaceId, board.getId(), list.getId(), authId, updateTaskListRequest());

        assertThat(response.getName()).isEqualTo("Review");
    }

    @Test
    void reorderLists_ownerRequest_updatesPositionsInRequestedOrder() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board board = board(workspaceId);
        TaskList first = taskList(board);
        TaskList second = taskList(board);

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(true);
        when(boardRepository.findByIdAndWorkspaceId(board.getId(), workspaceId)).thenReturn(Optional.of(board));
        when(taskListRepository.findByBoardIdOrderByPositionAsc(board.getId())).thenReturn(List.of(first, second));

        taskListService.reorderLists(workspaceId, board.getId(), authId, reorderListsRequest(List.of(second.getId(), first.getId())));

        assertThat(first.getPosition()).isEqualTo(1);
        assertThat(second.getPosition()).isEqualTo(0);
        verify(taskListRepository).saveAll(List.of(first, second));
    }

    @Test
    void deleteList_nonAdminRequest_throwsAccessDenied() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(false);

        assertThatThrownBy(() -> taskListService.deleteList(workspaceId, UUID.randomUUID(), UUID.randomUUID(), authId))
                .isInstanceOf(TaskServiceException.class);
    }
}
