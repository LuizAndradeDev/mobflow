package com.mobflow.taskservice.service;

import com.mobflow.taskservice.client.WorkspaceClient;
import com.mobflow.taskservice.model.dto.response.WorkspaceSummaryDTO;
import com.mobflow.taskservice.model.entities.Board;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.repository.BoardRepository;
import com.mobflow.taskservice.repository.TaskListRepository;
import com.mobflow.taskservice.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkspaceSummaryService {

    private static final int MAX_PREVIEW_TASKS = 3;

    private final BoardRepository boardRepository;
    private final TaskListRepository taskListRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceClient workspaceClient;
    private final TaskProfileService taskProfileService;

    @Autowired
    public WorkspaceSummaryService(
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

    public WorkspaceSummaryService(
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

    public WorkspaceSummaryDTO getSummary(UUID workspaceId, UUID authId) {
        workspaceClient.getMemberRole(workspaceId, authId);
        return buildSummary(workspaceId);
    }

    public WorkspaceSummaryDTO getSummary(UUID workspaceId) {
        return buildSummary(workspaceId);
    }

    public List<WorkspaceSummaryDTO> getSummaries(List<UUID> workspaceIds, UUID authId) {
        return workspaceIds.stream()
                .map(workspaceId -> getSummary(workspaceId, authId))
                .toList();
    }

    public List<WorkspaceSummaryDTO> getSummaries(List<UUID> workspaceIds) {
        return workspaceIds.stream()
                .map(this::getSummary)
                .toList();
    }

    private WorkspaceSummaryDTO buildSummary(UUID workspaceId) {
        List<Board> boards = boardRepository.findByWorkspaceIdOrderByPositionAsc(workspaceId);
        List<Task> allTasks = taskRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        Map<UUID, List<Task>> tasksByListId = allTasks.stream()
                .collect(Collectors.groupingBy(task -> task.getList().getId()));
        Map<UUID, String> avatarUrlsByAuthId = taskProfileService.avatarUrlsByAuthId(allTasks);

        List<WorkspaceSummaryDTO.BoardSummaryDTO> boardSummaries = boards.stream()
                .map(board -> buildBoardSummary(board, tasksByListId, avatarUrlsByAuthId))
                .toList();

        return WorkspaceSummaryDTO.builder()
                .workspaceId(workspaceId)
                .boards(boardSummaries)
                .build();
    }

    private WorkspaceSummaryDTO.BoardSummaryDTO buildBoardSummary(
            Board board,
            Map<UUID, List<Task>> tasksByListId,
            Map<UUID, String> avatarUrlsByAuthId
    ) {
        List<WorkspaceSummaryDTO.ListSummaryDTO> listSummaries = taskListRepository.findByBoardIdOrderByPositionAsc(board.getId()).stream()
                .map(taskList -> buildListSummary(taskList, tasksByListId, avatarUrlsByAuthId))
                .toList();

        return WorkspaceSummaryDTO.BoardSummaryDTO.builder()
                .id(board.getId())
                .name(board.getName())
                .color(board.getColor())
                .position(board.getPosition())
                .lists(listSummaries)
                .build();
    }

    private WorkspaceSummaryDTO.ListSummaryDTO buildListSummary(
            TaskList taskList,
            Map<UUID, List<Task>> tasksByListId,
            Map<UUID, String> avatarUrlsByAuthId
    ) {
        List<Task> listTasks = tasksByListId.getOrDefault(taskList.getId(), List.of());

        return WorkspaceSummaryDTO.ListSummaryDTO.builder()
                .id(taskList.getId())
                .name(taskList.getName())
                .position(taskList.getPosition())
                .taskCount(listTasks.size())
                .previewTasks(buildPreviewTasks(listTasks, avatarUrlsByAuthId))
                .build();
    }

    private List<WorkspaceSummaryDTO.TaskCardDTO> buildPreviewTasks(
            List<Task> tasks,
            Map<UUID, String> avatarUrlsByAuthId
    ) {
        return tasks.stream()
                .limit(MAX_PREVIEW_TASKS)
                .map(task -> WorkspaceSummaryDTO.TaskCardDTO.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .priority(task.getPriority().name())
                        .dueDate(task.getDueDate() == null ? null : task.getDueDate().toString())
                        .assigneeAvatarUrl(resolveAvatarUrl(task.getAssigneeAuthId(), avatarUrlsByAuthId))
                        .build())
                .toList();
    }

    private String resolveAvatarUrl(UUID assigneeAuthId, Map<UUID, String> avatarUrlsByAuthId) {
        if (assigneeAuthId == null) {
            return null;
        }
        return avatarUrlsByAuthId.get(assigneeAuthId);
    }
}
