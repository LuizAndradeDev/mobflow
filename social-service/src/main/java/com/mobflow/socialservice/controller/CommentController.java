package com.mobflow.socialservice.controller;

import com.mobflow.socialservice.model.dto.request.CreateCommentRequest;
import com.mobflow.socialservice.model.dto.request.UpdateCommentRequest;
import com.mobflow.socialservice.model.dto.response.CommentResponse;
import com.mobflow.socialservice.security.AuthenticatedUser;
import com.mobflow.socialservice.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(taskId, AuthenticatedUser.from(authentication), request));
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<Page<CommentResponse>> listComments(
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(commentService.listComments(
                taskId,
                AuthenticatedUser.from(authentication),
                page,
                size
        ));
    }

    @PutMapping("/api/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(commentService.updateComment(
                commentId,
                AuthenticatedUser.from(authentication),
                request
        ));
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            Authentication authentication
    ) {
        commentService.deleteComment(commentId, AuthenticatedUser.from(authentication));
        return ResponseEntity.noContent().build();
    }
}
