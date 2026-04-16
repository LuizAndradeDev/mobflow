package com.mobflow.taskservice.service;

import com.mobflow.taskservice.client.WorkspaceClient;
import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.model.dto.request.CreateBoardRequest;
import com.mobflow.taskservice.model.dto.request.UpdateBoardRequest;
import com.mobflow.taskservice.model.dto.response.BoardResponseDTO;
import com.mobflow.taskservice.model.dto.response.TaskListResponseDTO;
import com.mobflow.taskservice.model.entities.Board;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.repository.BoardRepository;
import com.mobflow.taskservice.repository.TaskListRepository;
import com.mobflow.taskservice.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final TaskListRepository taskListRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceClient workspaceClient;
    private final TaskProfileService taskProfileService;

    @Autowired
    public BoardService(
            BoardRepository boardRepository,
            TaskListRepository taskListRepository,
            TaskRepository taskRepository,
            WorkspaceClient workspaceClient,
            TaskProfileService taskProfileService
    ) {
        this.boardRepository = boardRepository;
        this.taskListRepository = taskListRepository;
        this.taskRepository = taskRepository;
        this.workspaceClient = workspaceClient;
        this.taskProfileService = taskProfileService;
    }

    public BoardService(
            BoardRepository boardRepository,
            TaskListRepository taskListRepository,
            TaskRepository taskRepository,
            WorkspaceClient workspaceClient,
            com.mobflow.taskservice.client.UserServiceClient userServiceClient
    ) {
        this(
                boardRepository,
                taskListRepository,
                taskRepository,
                workspaceClient,
                new TaskProfileService(userServiceClient)
        );
    }

    @Cacheable(value = "boards", key = "#workspaceId")
    public List<BoardResponseDTO> listBoards(UUID workspaceId) {
        List<Board> boards = boardRepository.findByWorkspaceIdOrderByPositionAsc(workspaceId);
        return boards.stream()
                .map(board -> buildBoardResponse(board, true))
                .toList();
    }

    public BoardResponseDTO getBoard(UUID workspaceId, UUID boardId) {
        Board board = boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
                .orElseThrow(TaskServiceException::boardNotFound);
        return buildBoardResponse(board, true);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public BoardResponseDTO createBoard(UUID workspaceId, UUID authId, CreateBoardRequest request) {
        requireOwnerOrAdmin(workspaceId, authId);

        int position = boardRepository.countByWorkspaceId(workspaceId);
        String color = request.getColor() != null ? request.getColor() : "#6366f1";

        Board board = Board.create(workspaceId, request.getName(), color, position);
        boardRepository.save(board);

        return BoardResponseDTO.fromEntityWithoutLists(board);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public BoardResponseDTO updateBoard(UUID workspaceId, UUID boardId, UUID authId, UpdateBoardRequest request) {
        requireOwnerOrAdmin(workspaceId, authId);

        Board board = boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
                .orElseThrow(TaskServiceException::boardNotFound);

        if (request.getName() != null) board.setName(request.getName());
        if (request.getColor() != null) board.setColor(request.getColor());

        boardRepository.save(board);
        return BoardResponseDTO.fromEntityWithoutLists(board);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public void deleteBoard(UUID workspaceId, UUID boardId, UUID authId) {
        requireOwnerOrAdmin(workspaceId, authId);

        Board board = boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
                .orElseThrow(TaskServiceException::boardNotFound);

        boardRepository.delete(board);
    }

    private BoardResponseDTO buildBoardResponse(Board board, boolean includeTasks) {
        List<TaskListResponseDTO> listResponses = taskListRepository.findByBoardIdOrderByPositionAsc(board.getId()).stream()
                .map(taskList -> buildTaskListResponse(taskList, includeTasks))
                .toList();

        return BoardResponseDTO.fromEntity(board, listResponses);
    }

    private TaskListResponseDTO buildTaskListResponse(TaskList taskList, boolean includeTasks) {
        if (!includeTasks) {
            return TaskListResponseDTO.fromEntityWithoutTasks(taskList);
        }

        List<Task> tasks = taskRepository.findByListIdOrderByPositionAsc(taskList.getId());
        return TaskListResponseDTO.fromEntity(taskList, taskProfileService.toTaskResponses(tasks));
    }

    private void requireOwnerOrAdmin(UUID workspaceId, UUID authId) {
        if (!workspaceClient.isOwnerOrAdmin(workspaceId, authId)) {
            throw TaskServiceException.accessDenied();
        }
    }
}
