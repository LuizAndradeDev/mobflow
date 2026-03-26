package com.mobflow.taskservice.model.dto.response;

import com.mobflow.taskservice.model.entities.Board;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BoardResponseDTO {

    private UUID id;
    private UUID workspaceId;
    private String name;
    private String color;
    private int position;
    private List<TaskListResponseDTO> lists;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BoardResponseDTO fromEntity(Board board, List<TaskListResponseDTO> lists) {
        return BoardResponseDTO.builder()
                .id(board.getId())
                .workspaceId(board.getWorkspaceId())
                .name(board.getName())
                .color(board.getColor())
                .position(board.getPosition())
                .lists(lists)
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }

    public static BoardResponseDTO fromEntityWithoutLists(Board board) {
        return BoardResponseDTO.builder()
                .id(board.getId())
                .workspaceId(board.getWorkspaceId())
                .name(board.getName())
                .color(board.getColor())
                .position(board.getPosition())
                .lists(List.of())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }
}
