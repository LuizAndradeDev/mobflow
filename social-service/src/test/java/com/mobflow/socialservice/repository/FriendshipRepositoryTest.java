package com.mobflow.socialservice.repository;

import com.mobflow.socialservice.config.MongoConfig;
import com.mobflow.socialservice.model.entities.Friendship;
import com.mobflow.socialservice.testsupport.AbstractMongoSocialTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.REQUESTER_ID;
import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.TARGET_ID;
import static com.mobflow.socialservice.testsupport.FriendshipTestFixtures.friendship;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@Import(MongoConfig.class)
class FriendshipRepositoryTest extends AbstractMongoSocialTest {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @BeforeEach
    void cleanUp() {
        friendshipRepository.deleteAll();
    }

    @Test
    void save_validFriendship_persistsDocument() {
        Friendship savedFriendship = friendshipRepository.save(friendship());

        assertThat(savedFriendship.getId()).isNotNull();
        assertThat(friendshipRepository.findById(savedFriendship.getId())).isPresent();
    }

    @Test
    void findByUserAAndUserB_existingFriendship_returnsMatch() {
        Friendship savedFriendship = friendshipRepository.save(friendship());

        Optional<Friendship> found = friendshipRepository.findByUserAAndUserB(savedFriendship.getUserA(), savedFriendship.getUserB());

        assertThat(found).contains(savedFriendship);
    }

    @Test
    void findByUserAOrUserB_existingFriendship_returnsFriendshipsForParticipant() {
        Friendship savedFriendship = friendshipRepository.save(friendship());

        List<Friendship> friendships = friendshipRepository.findByUserAOrUserB(REQUESTER_ID, REQUESTER_ID);

        assertThat(friendships).contains(savedFriendship);
    }

    @Test
    void save_duplicatePair_throwsDuplicateKeyException() {
        friendshipRepository.save(friendship(UUID.randomUUID(), REQUESTER_ID, "john_dev", TARGET_ID, "mary_dev"));

        assertThatThrownBy(() -> friendshipRepository.save(friendship(UUID.randomUUID(), REQUESTER_ID, "john_dev", TARGET_ID, "mary_dev")))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
