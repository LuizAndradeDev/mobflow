package com.mobflow.taskservice.service;

import com.mobflow.taskservice.client.UserServiceClient;
import com.mobflow.taskservice.model.dto.response.WorkspaceSummaryDTO;
import com.mobflow.taskservice.model.entities.Board;
import com.mobflow.taskservice.model.entities.Task;
import com.mobflow.taskservice.model.entities.TaskList;
import com.mobflow.taskservice.repository.BoardRepository;
import com.mobflow.taskservice.repository.TaskListRepository;
import com.mobflow.taskservice.repository.TaskRepository;
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
    private final UserServiceClient userServiceClient;

    public WorkspaceSummaryService(
            BoardRepository boardRepository,
            TaskListRepository taskListRepository,
            TaskRepository taskRepository,
            UserServiceClient userServiceClient
    ) {
        this.boardRepository = boardRepository;
        this.taskListRepository = taskListRepository;
        this.taskRepository = taskRepository;
        this.userServiceClient = userServiceClient;
    }

    /**
     * Returns a compact summary of all boards in a single workspace.
     * Each list includes a preview of up to 3 tasks (no full task payload).
     */
    public WorkspaceSummaryDTO getSummary(UUID workspaceId) {
        List<Board> boards = boardRepository.findByWorkspaceIdOrderByPositionAsc(workspaceId);

        // Collect all tasks for this workspace in one query to avoid N+1
        List<Task> allTasks = taskRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        Map<UUID, List<Task>> tasksByListId = allTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getList().getId()));

        // Resolve assignee avatars for preview tasks
        List<UUID> assigneeIds = allTasks.stream()
                .map(Task::getAssigneeAuthId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<UUID, String> avatarByAuthId = userServiceClient.fetchProfiles(assigneeIds).stream()
                .collect(Collectors.toMap(
                        UserServiceClient.UserProfileResponse::authId,
                        p -> p.avatarUrl() != null ? p.avatarUrl() : ""
                ));

        List<WorkspaceSummaryDTO.BoardSummaryDTO> boardSummaries = boards.stream().map(board -> {
            List<TaskList> lists = taskListRepository.findByBoardIdOrderByPositionAsc(board.getId());

            List<WorkspaceSummaryDTO.ListSummaryDTO> listSummaries = lists.stream().map(list -> {
                List<Task> listTasks = tasksByListId.getOrDefault(list.getId(), List.of());

                List<WorkspaceSummaryDTO.TaskCardDTO> preview = listTasks.stream()
                        .limit(MAX_PREVIEW_TASKS)
                        .map(t -> WorkspaceSummaryDTO.TaskCardDTO.builder()
                                .id(t.getId())
                                .title(t.getTitle())
                                .priority(t.getPriority().name())
                                .dueDate(t.getDueDate() != null ? t.getDueDate().toString() : null)
                                .assigneeAvatarUrl(
                                        t.getAssigneeAuthId() != null
                                                ? avatarByAuthId.get(t.getAssigneeAuthId())
                                                : null
                                )
                                .build())
                        .toList();

                return WorkspaceSummaryDTO.ListSummaryDTO.builder()
                        .id(list.getId())
                        .name(list.getName())
                        .position(list.getPosition())
                        .taskCount(listTasks.size())
                        .previewTasks(preview)
                        .build();
            }).toList();

            return WorkspaceSummaryDTO.BoardSummaryDTO.builder()
                    .id(board.getId())
                    .name(board.getName())
                    .color(board.getColor())
                    .position(board.getPosition())
                    .lists(listSummaries)
                    .build();
        }).toList();

        return WorkspaceSummaryDTO.builder()
                .workspaceId(workspaceId)
                .boards(boardSummaries)
                .build();
    }

    /**
     * Returns summaries for multiple workspaces at once (used by the overview page).
     */
    public List<WorkspaceSummaryDTO> getSummaries(List<UUID> workspaceIds) {
        return workspaceIds.stream()
                .map(this::getSummary)
                .toList();
    }
}
