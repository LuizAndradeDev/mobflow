package com.mobflow.notificationservice.controller;

import com.mobflow.notificationservice.model.dto.response.NotificationResponseDTO;
import com.mobflow.notificationservice.model.dto.response.UnreadCountResponseDTO;
import com.mobflow.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> list(Authentication authentication) {
        return ResponseEntity.ok(notificationService.listForUser(extractRecipientId(authentication)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponseDTO> unreadCount(Authentication authentication) {
        return ResponseEntity.ok(new UnreadCountResponseDTO(notificationService.countUnread(extractRecipientId(authentication))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAsRead(id, extractRecipientId(authentication)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<UnreadCountResponseDTO> markAllAsRead(Authentication authentication) {
        long updatedCount = notificationService.markAllAsRead(extractRecipientId(authentication));
        return ResponseEntity.ok(new UnreadCountResponseDTO(updatedCount));
    }

    private String extractRecipientId(Authentication authentication) {
        Object credentials = authentication.getCredentials();
        if (credentials instanceof UUID authId) {
            return authId.toString();
        }
        return String.valueOf(credentials);
    }
}
