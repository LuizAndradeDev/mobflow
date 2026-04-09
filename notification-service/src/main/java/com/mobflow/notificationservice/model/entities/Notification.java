package com.mobflow.notificationservice.model.entities;

import com.mobflow.notificationservice.model.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = "id")
@Builder
public class Notification {
    private String id;
    private UUID recipientAuthId;
    private NotificationType type;
    private Boolean read = false;
    private LocalDateTime createdAt;
    private Map<String,String> payload;

}
