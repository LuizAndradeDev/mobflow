package com.mobflow.userservice.repository;

import com.mobflow.userservice.model.entities.UserProfile;
import com.mobflow.userservice.testsupport.AbstractPostgresUserServiceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static com.mobflow.userservice.testsupport.UserServiceTestFixtures.userProfile;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserProfileRepositoryTest extends AbstractPostgresUserServiceTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void findByAuthId_existingProfile_returnsProfile() {
        UserProfile profile = userProfile();
        profile.setId(null);
        UserProfile saved = userProfileRepository.save(profile);

        assertThat(userProfileRepository.findByAuthId(saved.getAuthId()))
                .isPresent()
                .get()
                .extracting(UserProfile::getId)
                .isEqualTo(saved.getId());
    }

    @Test
    void findByDisplayName_existingProfile_returnsProfile() {
        UserProfile profile = userProfile(UUID.randomUUID(), "johnny");
        profile.setId(null);
        UserProfile saved = userProfileRepository.save(profile);

        assertThat(userProfileRepository.findByDisplayName("johnny"))
                .isPresent()
                .get()
                .extracting(UserProfile::getId)
                .isEqualTo(saved.getId());
    }

    @Test
    void findAllByAuthIdIn_profilesExist_returnsMatchingProfiles() {
        UserProfile firstProfile = userProfile(UUID.randomUUID(), "john");
        firstProfile.setId(null);
        UserProfile secondProfile = userProfile(UUID.randomUUID(), "mary");
        secondProfile.setId(null);
        UserProfile first = userProfileRepository.save(firstProfile);
        UserProfile second = userProfileRepository.save(secondProfile);

        List<UserProfile> profiles = userProfileRepository.findAllByAuthIdIn(List.of(first.getAuthId(), second.getAuthId()));

        assertThat(profiles).extracting(UserProfile::getAuthId)
                .containsExactlyInAnyOrder(first.getAuthId(), second.getAuthId());
    }

    @Test
    void existsByAuthId_profileExists_returnsTrue() {
        UserProfile profile = userProfile();
        profile.setId(null);
        UserProfile saved = userProfileRepository.save(profile);

        assertThat(userProfileRepository.existsByAuthId(saved.getAuthId())).isTrue();
        assertThat(userProfileRepository.existsByAuthId(UUID.randomUUID())).isFalse();
    }
}
