package com.mobflow.socialservice.repository;

import com.mobflow.socialservice.model.entities.FriendRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository extends MongoRepository<FriendRequest, UUID> {

    @Query("{ '$or': [ {'requesterId': ?0, 'targetId': ?1}, {'requesterId': ?1, 'targetId': ?0} ], 'status': 'PENDING' }")
    Optional<FriendRequest> findPendingBetweenUsers(UUID firstUserId, UUID secondUserId);

    @Query("{ '$or': [ {'requesterId': ?0}, {'targetId': ?0} ] }")
    List<FriendRequest> findAllByParticipant(UUID authId, Sort sort);
}
