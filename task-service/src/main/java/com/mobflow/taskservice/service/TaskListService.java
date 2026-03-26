package com.mobflow.taskservice.service;

import com.mobflow.taskservice.client.WorkspaceClient;
import com.mobflow.taskservice.exception.TaskServiceException;
import com.mobflow.taskservice.model.dto.request.CreateTaskListRequest;
import com.mobflow.taskservice.model.dto.request.ReorderListsRequest;
import com.mobflow.taskservice.model.dto.request.UpdateTaskListRequest;
import com.mobflow.taskservice.model.dto.response.TaskListResponseDTO;
import com.mobflow.taskservice.model.entities.Board;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.repository.BoardRepository;
import com.mobflow.taskservice.repository.TaskListRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TaskListService {

    private final TaskListRepository taskListRepository;
    private final BoardRepository boardRepository;
    private final WorkspaceClient workspaceClient;

    public TaskListService(
            TaskListRepository taskListRepository,
            BoardRepository boardRepository,
            WorkspaceClient workspaceClient
    ) {
        this.taskListRepository = taskListRepository;
        this.boardRepository = boardRepository;
        this.workspaceClient = workspaceClient;
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public TaskListResponseDTO createList(UUID workspaceId, UUID boardId, UUID authId, CreateTaskListRequest request) {
        requireOwnerOrAdmin(workspaceId, authId);

        Board board = boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
                .orElseThrow(TaskServiceException::boardNotFound);

        int position = taskListRepository.countByBoardId(boardId);
        TaskList list = TaskList.create(board, request.getName(), position);
        taskListRepository.save(list);

        return TaskListResponseDTO.fromEntityWithoutTasks(list);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public TaskListResponseDTO updateList(UUID workspaceId, UUID boardId, UUID listId, UUID authId, UpdateTaskListRequest request) {
        requireOwnerOrAdmin(workspaceId, authId);

        boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
                .orElseThrow(TaskServiceException::boardNotFound);

        TaskList list = taskListRepository.findByIdAndBoardId(listId, boardId)
                .orElseThrow(TaskServiceException::listNotFound);

        if (request.getName() != null) list.setName(request.getName());

        taskListRepository.save(list);
        return TaskListResponseDTO.fromEntityWithoutTasks(list);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public void deleteList(UUID workspaceId, UUID boardId, UUID listId, UUID authId) {
        requireOwnerOrAdmin(workspaceId, authId);

        boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
                .orElseThrow(TaskServiceException::boardNotFound);

        TaskList list = taskListRepository.findByIdAndBoardId(listId, boardId)
                .orElseThrow(TaskServiceException::listNotFound);

        taskListRepository.delete(list);
    }

    @Transactional
    @CacheEvict(value = "boards", key = "#workspaceId")
    public void reorderLists(UUID workspaceId, UUID boardId, UUID authId, ReorderListsRequest request) {
        requireOwnerOrAdmin(workspaceId, authId);

        boardRepository.findByIdAndWorkspaceId(boardId, workspaceId)
                .orElseThrow(TaskServiceException::boardNotFound);

        List<TaskList> lists = taskListRepository.findByBoardIdOrderByPositionAsc(boardId);

        for (int i = 0; i < request.getOrderedIds().size(); i++) {
            UUID id = request.getOrderedIds().get(i);
            final int newPosition = i;
            lists.stream()
                    .filter(l -> l.getId().equals(id))
                    .findFirst()
                    .ifPresent(l -> l.setPosition(newPosition));
        }

        taskListRepository.saveAll(lists);
    }

    private void requireOwnerOrAdmin(UUID workspaceId, UUID authId) {
        if (!workspaceClient.isOwnerOrAdmin(workspaceId, authId)) {
            throw TaskServiceException.accessDenied();
        }
    }
}
