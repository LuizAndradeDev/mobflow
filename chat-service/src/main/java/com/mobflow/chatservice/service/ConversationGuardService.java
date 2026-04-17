package com.mobflow.chatservice.service;

import com.mobflow.chatservice.exception.ChatServiceException;
import com.mobflow.chatservice.model.entities.Conversation;
import com.mobflow.chatservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationGuardService {

    private final ConversationRepository conversationRepository;

    public Conversation requireParticipant(UUID conversationId, UUID authId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(ChatServiceException::conversationNotFound);

        if (!conversation.getParticipantIds().contains(authId)) {
            throw ChatServiceException.accessDenied();
        }

        return conversation;
    }

    public UUID resolveCounterpartAuthId(Conversation conversation, UUID authId) {
        return conversation.getParticipantIds().stream()
                .filter(participantId -> !participantId.equals(authId))
                .findFirst()
                .orElseThrow(ChatServiceException::conversationNotFound);
    }

    public NormalizedParticipantPair normalizePrivatePair(UUID firstParticipantId, UUID secondParticipantId) {
        List<UUID> normalizedParticipantIds = new ArrayList<>(List.of(firstParticipantId, secondParticipantId));
        normalizedParticipantIds.sort(Comparator.comparing(UUID::toString));

        return new NormalizedParticipantPair(
                normalizedParticipantIds,
                normalizedParticipantIds.get(0) + ":" + normalizedParticipantIds.get(1)
        );
    }

    public long unreadCountFor(Conversation conversation, UUID authId) {
        return conversation.getUnreadCountByUser().getOrDefault(authId.toString(), 0L);
    }

    public record NormalizedParticipantPair(
            List<UUID> participantIds,
            String participantPairKey
    ) {
    }
}
