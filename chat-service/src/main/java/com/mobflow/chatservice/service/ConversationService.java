package com.mobflow.chatservice.service;

import com.mobflow.chatservice.client.SocialServiceClient;
import com.mobflow.chatservice.exception.ChatServiceException;
import com.mobflow.chatservice.model.dto.request.CreateConversationRequestDTO;
import com.mobflow.chatservice.model.dto.response.ConversationResponseDTO;
import com.mobflow.chatservice.model.dto.response.ConversationUpsertResponseDTO;
import com.mobflow.chatservice.model.dto.response.MarkConversationReadResponseDTO;
import com.mobflow.chatservice.model.entities.Conversation;
import com.mobflow.chatservice.repository.ConversationRepository;
import com.mobflow.chatservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationGuardService conversationGuardService;
    private final ConversationReadService conversationReadService;
    private final SocialServiceClient socialServiceClient;

    public ConversationUpsertResponseDTO createOrGetPrivateConversation(
            AuthenticatedUser authenticatedUser,
            CreateConversationRequestDTO request
    ) {
        UUID currentAuthId = authenticatedUser.authId();
        UUID targetAuthId = request.getTargetAuthId();

        if (currentAuthId.equals(targetAuthId)) {
            throw ChatServiceException.selfConversationNotAllowed();
        }

        ConversationGuardService.NormalizedParticipantPair normalizedPair =
                conversationGuardService.normalizePrivatePair(currentAuthId, targetAuthId);

        Conversation existingConversation = conversationRepository.findByParticipantPairKey(normalizedPair.participantPairKey())
                .orElse(null);

        if (existingConversation != null) {
            return ConversationUpsertResponseDTO.builder()
                    .created(false)
                    .conversation(ConversationResponseDTO.fromEntity(existingConversation, currentAuthId))
                    .build();
        }

        socialServiceClient.validateFriendshipRequired(currentAuthId, targetAuthId);

        Instant now = Instant.now();
        Map<String, Long> unreadCountByUser = new LinkedHashMap<>();
        unreadCountByUser.put(currentAuthId.toString(), 0L);
        unreadCountByUser.put(targetAuthId.toString(), 0L);

        Conversation conversation = Conversation.create(
                normalizedPair.participantIds(),
                normalizedPair.participantPairKey(),
                unreadCountByUser,
                now
        );

        try {
            Conversation savedConversation = conversationRepository.save(conversation);
            return ConversationUpsertResponseDTO.builder()
                    .created(true)
                    .conversation(ConversationResponseDTO.fromEntity(savedConversation, currentAuthId))
                    .build();
        } catch (DuplicateKeyException exception) {
            Conversation savedConversation = conversationRepository.findByParticipantPairKey(normalizedPair.participantPairKey())
                    .orElseThrow(ChatServiceException::conversationNotFound);
            return ConversationUpsertResponseDTO.builder()
                    .created(false)
                    .conversation(ConversationResponseDTO.fromEntity(savedConversation, currentAuthId))
                    .build();
        }
    }

    public List<ConversationResponseDTO> listConversations(AuthenticatedUser authenticatedUser) {
        return conversationRepository.findByParticipantIdsContainingOrderByLastActivityAtDesc(authenticatedUser.authId()).stream()
                .map(conversation -> ConversationResponseDTO.fromEntity(conversation, authenticatedUser.authId()))
                .toList();
    }

    public ConversationResponseDTO getConversation(UUID conversationId, AuthenticatedUser authenticatedUser) {
        Conversation conversation = conversationGuardService.requireParticipant(conversationId, authenticatedUser.authId());
        return ConversationResponseDTO.fromEntity(conversation, authenticatedUser.authId());
    }

    public MarkConversationReadResponseDTO markConversationAsRead(UUID conversationId, AuthenticatedUser authenticatedUser) {
        Conversation conversation = conversationGuardService.requireParticipant(conversationId, authenticatedUser.authId());
        return conversationReadService.markConversationAsRead(conversation, authenticatedUser.authId());
    }
}
