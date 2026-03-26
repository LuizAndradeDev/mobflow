package com.mobflow.taskservice.service;

import com.mobflow.taskservice.client.UserServiceClient;
import com.mobflow.taskservice.client.WorkspaceClient;
import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.model.dto.request.CreateBoardRequest;
import com.mobflow.taskservice.model.dto.request.UpdateBoardRequest;
import com.mobflow.taskservice.model.dto.response.BoardResponseDTO;
import com.mobflow.taskservice.model.dto.response.TaskListResponseDTO;
import com.mobflow.taskservice.model.dto.response.TaskResponseDTO;
import com.mobflow.taskservice.model.entities.Board;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.repository.BoardRepository;
import com.mobflow.taskservice.repository.TaskListRepository;
import com.mobflow.taskservice.repository.TaskRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final TaskListRepository taskListRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceClient workspaceClient;
    private final UserServiceClient userServiceClient;

    public BoardService(
            BoardRepository boardRepository,
            TaskListRepository taskListRepository,
            TaskRepository taskRepository,
            WorkspaceClient workspaceClient,
            UserServiceClient userServiceClient
    ) {
        this.boardRepository = boardRepository;
        this.taskListRepository = taskListRepository;
        this.taskRepository = taskRepository;
        this.workspaceClient = workspaceClient;
        this.userServiceClient = userServiceClient;
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

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private BoardResponseDTO buildBoardResponse(Board board, boolean includeTasks) {
        List<TaskList> lists = taskListRepository.findByBoardIdOrderByPositionAsc(board.getId());

        List<TaskListResponseDTO> listDTOs = lists.stream().map(list -> {
            if (!includeTasks) {
                return TaskListResponseDTO.fromEntityWithoutTasks(list);
            }

            List<Task> tasks = taskRepository.findByListIdOrderByPositionAsc(list.getId());
            Map<UUID, UserServiceClient.UserProfileResponse> profileMap = resolveProfiles(tasks);

            List<TaskResponseDTO> taskDTOs = tasks.stream()
                    .map(task -> {
                        if (task.getAssigneeAuthId() != null) {
                            UserServiceClient.UserProfileResponse profile = profileMap.get(task.getAssigneeAuthId());
                            String displayName = profile != null ? profile.displayName() : null;
                            String avatarUrl = profile != null ? profile.avatarUrl() : null;
                            return TaskResponseDTO.fromEntity(task, displayName, avatarUrl);
                        }
                        return TaskResponseDTO.fromEntity(task);
                    })
                    .toList();

            return TaskListResponseDTO.fromEntity(list, taskDTOs);
        }).toList();

        return BoardResponseDTO.fromEntity(board, listDTOs);
    }

    private Map<UUID, UserServiceClient.UserProfileResponse> resolveProfiles(List<Task> tasks) {
        List<UUID> assigneeIds = tasks.stream()
                .map(Task::getAssigneeAuthId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (assigneeIds.isEmpty()) return Map.of();

        return userServiceClient.fetchProfiles(assigneeIds).stream()
                .collect(Collectors.toMap(UserServiceClient.UserProfileResponse::authId, p -> p));
    }

    private void requireOwnerOrAdmin(UUID workspaceId, UUID authId) {
        if (!workspaceClient.isOwnerOrAdmin(workspaceId, authId)) {
            throw TaskServiceException.accessDenied();
        }
    }
}
