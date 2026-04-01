package com.mobflow.taskservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WorkspaceSummaryDTO {

    private UUID workspaceId;
    private List<BoardSummaryDTO> boards;

    @Data
    @Builder
    public static class BoardSummaryDTO {
        private UUID id;
        private String name;
        private String color;
        private int position;
        private List<ListSummaryDTO> lists;
    }

    @Data
    @Builder
    public static class ListSummaryDTO {
        private UUID id;
        private String name;
        private int position;
        private int taskCount;
        private List<TaskCardDTO> previewTasks;
    }

    @Data
    @Builder
    public static class TaskCardDTO {
        private UUID id;
        private String title;
        private String priority;
        private String dueDate;
        private String assigneeAvatarUrl;
    }
}
