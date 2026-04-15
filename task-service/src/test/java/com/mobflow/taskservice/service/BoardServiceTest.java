package com.mobflow.taskservice.service;

import com.mobflow.taskservice.client.UserServiceClient;
import com.mobflow.taskservice.client.WorkspaceClient;
import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.model.dto.response.BoardResponseDTO;
import com.mobflow.taskservice.model.entities.Board;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.repository.BoardRepository;
import com.mobflow.taskservice.repository.TaskListRepository;
import com.mobflow.taskservice.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.board;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.createBoardRequest;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.profile;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.task;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.taskList;
import static com.mobflow.taskservice.testsupport.TaskServiceTestFixtures.updateBoardRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private TaskListRepository taskListRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private UserServiceClient userServiceClient;

    private BoardService boardService;

    @BeforeEach
    void setUp() {
        boardService = new BoardService(boardRepository, taskListRepository, taskRepository, workspaceClient, userServiceClient);
    }

    @Test
    void createBoard_ownerRequest_persistsBoardWithNextPosition() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(true);
        when(boardRepository.countByWorkspaceId(workspaceId)).thenReturn(3);
        when(boardRepository.save(any(Board.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoardResponseDTO response = boardService.createBoard(workspaceId, authId, createBoardRequest());

        assertThat(response.getPosition()).isEqualTo(3);
        assertThat(response.getColor()).isEqualTo("#111827");
    }

    @Test
    void createBoard_nonAdminRequest_throwsAccessDenied() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(false);

        assertThatThrownBy(() -> boardService.createBoard(workspaceId, authId, createBoardRequest()))
                .isInstanceOf(TaskServiceException.class);
    }

    @Test
    void listBoards_listsAndTasksExist_buildsNestedResponseWithProfiles() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        UUID assigneeAuthId = UUID.randomUUID();
        Board board = board(workspaceId);
        TaskList list = taskList(board);
        Task task = task(list, workspaceId, authId, assigneeAuthId);

        when(boardRepository.findByWorkspaceIdOrderByPositionAsc(workspaceId)).thenReturn(List.of(board));
        when(taskListRepository.findByBoardIdOrderByPositionAsc(board.getId())).thenReturn(List.of(list));
        when(taskRepository.findByListIdOrderByPositionAsc(list.getId())).thenReturn(List.of(task));
        when(userServiceClient.fetchProfiles(List.of(assigneeAuthId))).thenReturn(List.of(profile(assigneeAuthId, "mary")));

        List<BoardResponseDTO> response = boardService.listBoards(workspaceId);

        assertThat(response).singleElement().satisfies(boardResponse -> {
            assertThat(boardResponse.getLists()).singleElement().satisfies(listResponse ->
                    assertThat(listResponse.getTasks()).singleElement().satisfies(taskResponse ->
                            assertThat(taskResponse.getAssigneeDisplayName()).isEqualTo("mary")));
        });
    }

    @Test
    void updateBoard_existingBoard_updatesMutableFields() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board board = board(workspaceId);

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(true);
        when(boardRepository.findByIdAndWorkspaceId(board.getId(), workspaceId)).thenReturn(Optional.of(board));
        when(boardRepository.save(board)).thenReturn(board);

        BoardResponseDTO response = boardService.updateBoard(workspaceId, board.getId(), authId, updateBoardRequest());

        assertThat(response.getName()).isEqualTo("Platform Ops");
        assertThat(response.getColor()).isEqualTo("#0f766e");
    }

    @Test
    void deleteBoard_existingBoard_deletesEntity() {
        UUID workspaceId = UUID.randomUUID();
        UUID authId = UUID.randomUUID();
        Board board = board(workspaceId);

        when(workspaceClient.isOwnerOrAdmin(workspaceId, authId)).thenReturn(true);
        when(boardRepository.findByIdAndWorkspaceId(board.getId(), workspaceId)).thenReturn(Optional.of(board));

        boardService.deleteBoard(workspaceId, board.getId(), authId);

        verify(boardRepository).delete(board);
    }
}
