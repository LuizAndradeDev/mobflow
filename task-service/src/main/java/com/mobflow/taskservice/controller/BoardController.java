package com.mobflow.taskservice.controller;

import com.mobflow.taskservice.model.dto.request.CreateBoardRequest;
import com.mobflow.taskservice.model.dto.request.UpdateBoardRequest;
import com.mobflow.taskservice.model.dto.response.BoardResponseDTO;
import com.mobflow.taskservice.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public ResponseEntity<List<BoardResponseDTO>> listBoards(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(boardService.listBoards(workspaceId));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponseDTO> getBoard(
            @PathVariable UUID workspaceId,
            @PathVariable UUID boardId
    ) {
        return ResponseEntity.ok(boardService.getBoard(workspaceId, boardId));
    }

    @PostMapping
    public ResponseEntity<BoardResponseDTO> createBoard(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateBoardRequest request,
            Authentication authentication
    ) {
        UUID authId = (UUID) authentication.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.createBoard(workspaceId, authId, request));
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<BoardResponseDTO> updateBoard(
            @PathVariable UUID workspaceId,
            @PathVariable UUID boardId,
            @Valid @RequestBody UpdateBoardRequest request,
            Authentication authentication
    ) {
        UUID authId = (UUID) authentication.getCredentials();
        return ResponseEntity.ok(boardService.updateBoard(workspaceId, boardId, authId, request));
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable UUID workspaceId,
            @PathVariable UUID boardId,
            Authentication authentication
    ) {
        UUID authId = (UUID) authentication.getCredentials();
        boardService.deleteBoard(workspaceId, boardId, authId);
        return ResponseEntity.noContent().build();
    }
}
