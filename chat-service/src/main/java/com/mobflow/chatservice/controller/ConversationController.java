package com.mobflow.chatservice.controller;

import com.mobflow.chatservice.model.dto.request.CreateConversationRequestDTO;
import com.mobflow.chatservice.model.dto.response.ConversationResponseDTO;
import com.mobflow.chatservice.model.dto.response.ConversationUpsertResponseDTO;
import com.mobflow.chatservice.model.dto.response.MarkConversationReadResponseDTO;
import com.mobflow.chatservice.model.dto.response.MessageResponseDTO;
import com.mobflow.chatservice.security.AuthenticatedUser;
import com.mobflow.chatservice.service.ConversationService;
import com.mobflow.chatservice.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    @PostMapping("/private")
    public ResponseEntity<ConversationUpsertResponseDTO> createOrGetPrivateConversation(
            @Valid @RequestBody CreateConversationRequestDTO request,
            Authentication authentication
    ) {
        ConversationUpsertResponseDTO response =
                conversationService.createOrGetPrivateConversation(AuthenticatedUser.from(authentication), request);

        return ResponseEntity.status(response.isCreated() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponseDTO>> listConversations(Authentication authentication) {
        return ResponseEntity.ok(conversationService.listConversations(AuthenticatedUser.from(authentication)));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponseDTO> getConversation(
            @PathVariable UUID conversationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(conversationService.getConversation(conversationId, AuthenticatedUser.from(authentication)));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<Page<MessageResponseDTO>> listMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(messageService.listMessages(
                conversationId,
                AuthenticatedUser.from(authentication),
                page,
                size
        ));
    }

    @PostMapping("/{conversationId}/read")
    public ResponseEntity<MarkConversationReadResponseDTO> markConversationAsRead(
            @PathVariable UUID conversationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(conversationService.markConversationAsRead(
                conversationId,
                AuthenticatedUser.from(authentication)
        ));
    }
}
