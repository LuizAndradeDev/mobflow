package com.mobflow.chatservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationUpsertResponseDTO {

    private boolean created;
    private ConversationResponseDTO conversation;
}
