package com.mobflow.userservice.services;

import com.mobflow.userservice.exceptions.UserProfileNotFoundException;
import com.mobflow.userservice.model.dto.request.UpdateUserProfileDTO;
import com.mobflow.userservice.model.entities.UserProfile;
import com.mobflow.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.mobflow.userservice.testsupport.UserServiceTestFixtures.avatarFile;
import static com.mobflow.userservice.testsupport.UserServiceTestFixtures.updateUserProfileDTO;
import static com.mobflow.userservice.testsupport.UserServiceTestFixtures.userProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private StorageService storageService;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userProfileRepository, storageService);
    }

    @Test
    void findOrCreateProfile_profileExists_returnsExistingProfile() {
        UUID authId = UUID.randomUUID();
        UserProfile existingProfile = userProfile(authId, "john");
        when(userProfileRepository.findByAuthId(authId)).thenReturn(Optional.of(existingProfile));

        UserProfile result = userProfileService.findOrCreateProfile(authId, "john");

        assertThat(result).isSameAs(existingProfile);
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void findOrCreateProfile_profileMissing_createsFallbackProfile() {
        UUID authId = UUID.randomUUID();
        when(userProfileRepository.findByAuthId(authId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = userProfileService.findOrCreateProfile(authId, "john");

        assertThat(result.getAuthId()).isEqualTo(authId);
        assertThat(result.getDisplayName()).isEqualTo("john");
    }

    @Test
    void updateProfile_partialUpdate_updatesOnlyProvidedFields() {
        UUID authId = UUID.randomUUID();
        UserProfile profile = userProfile(authId, "john");
        UpdateUserProfileDTO dto = updateUserProfileDTO("johnny", null, "99999");

        when(userProfileRepository.findByAuthId(authId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = userProfileService.updateProfile(authId, dto);

        assertThat(updated.getDisplayName()).isEqualTo("johnny");
        assertThat(updated.getBio()).isEqualTo("Bio");
        assertThat(updated.getPhone()).isEqualTo("99999");
    }

    @Test
    void updateProfile_profileMissing_throwsNotFound() {
        UUID authId = UUID.randomUUID();
        when(userProfileRepository.findByAuthId(authId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.updateProfile(authId, new UpdateUserProfileDTO()))
                .isInstanceOf(UserProfileNotFoundException.class);
    }

    @Test
    void updateAvatar_profileHasPreviousAvatar_deletesOldAvatarBeforeSavingNewOne() {
        UUID authId = UUID.randomUUID();
        UserProfile profile = userProfile(authId, "john");
        when(userProfileRepository.findByAuthId(authId)).thenReturn(Optional.of(profile));
        when(storageService.uploadAvatar(any())).thenReturn("http://cdn.mobflow.dev/new-avatar.png");
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = userProfileService.updateAvatar(authId, avatarFile());

        verify(storageService).deleteAvatar("http://cdn.mobflow.dev/avatar.png");
        assertThat(updated.getAvatarUrl()).isEqualTo("http://cdn.mobflow.dev/new-avatar.png");
    }

    @Test
    void getProfileByEmailLookup_profileMissing_throwsNotFound() {
        when(userProfileRepository.findByDisplayName("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfileByUsername("missing"))
                .isInstanceOf(UserProfileNotFoundException.class);
    }
}
