package com.mobflow.chatservice.service;

import com.mobflow.chatservice.client.SocialServiceClient;
import com.mobflow.chatservice.exception.ChatServiceException;
import com.mobflow.chatservice.model.dto.response.ConversationUpsertResponseDTO;
import com.mobflow.chatservice.model.entities.Conversation;
import com.mobflow.chatservice.repository.ConversationRepository;
import com.mobflow.chatservice.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.mobflow.chatservice.testsupport.ChatTestFixtures.FRIEND_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.USER_ID;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.conversation;
import static com.mobflow.chatservice.testsupport.ChatTestFixtures.createConversationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationGuardService conversationGuardService;

    @Mock
    private ConversationReadService conversationReadService;

    @Mock
    private SocialServiceClient socialServiceClient;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void createOrGetPrivateConversation_existingConversation_returnsExistingWithoutCallingSocialService() {
        Conversation existingConversation = conversation();
        ConversationGuardService.NormalizedParticipantPair normalizedPair =
                new ConversationGuardService.NormalizedParticipantPair(
                        existingConversation.getParticipantIds(),
                        existingConversation.getParticipantPairKey()
                );

        when(conversationGuardService.normalizePrivatePair(USER_ID, FRIEND_ID)).thenReturn(normalizedPair);
        when(conversationRepository.findByParticipantPairKey(existingConversation.getParticipantPairKey()))
                .thenReturn(Optional.of(existingConversation));

        ConversationUpsertResponseDTO response = conversationService.createOrGetPrivateConversation(
                AuthenticatedUser.of(USER_ID, "john_dev"),
                createConversationRequest()
        );

        assertThat(response.isCreated()).isFalse();
        assertThat(response.getConversation().getId()).isEqualTo(existingConversation.getId());
        verifyNoInteractions(socialServiceClient);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createOrGetPrivateConversation_selfConversation_throwsBusinessException() {
        assertThatThrownBy(() -> conversationService.createOrGetPrivateConversation(
                AuthenticatedUser.of(USER_ID, "john_dev"),
                com.mobflow.chatservice.model.dto.request.CreateConversationRequestDTO.builder()
                        .targetAuthId(USER_ID)
                        .build()
        )).isInstanceOf(ChatServiceException.class)
                .hasMessage("You cannot create a conversation with yourself");
    }

    @Test
    void createOrGetPrivateConversation_newConversation_savesAndReturnsCreatedResponse() {
        Conversation savedConversation = conversation();
        ConversationGuardService.NormalizedParticipantPair normalizedPair =
                new ConversationGuardService.NormalizedParticipantPair(
                        savedConversation.getParticipantIds(),
                        savedConversation.getParticipantPairKey()
                );

        when(conversationGuardService.normalizePrivatePair(USER_ID, FRIEND_ID)).thenReturn(normalizedPair);
        when(conversationRepository.findByParticipantPairKey(savedConversation.getParticipantPairKey()))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);

        ConversationUpsertResponseDTO response = conversationService.createOrGetPrivateConversation(
                AuthenticatedUser.of(USER_ID, "john_dev"),
                createConversationRequest()
        );

        assertThat(response.isCreated()).isTrue();
        assertThat(response.getConversation().getCounterpartAuthId()).isEqualTo(FRIEND_ID);
        verify(socialServiceClient).validateFriendshipRequired(USER_ID, FRIEND_ID);
        verify(conversationRepository).save(any(Conversation.class));
    }
}
