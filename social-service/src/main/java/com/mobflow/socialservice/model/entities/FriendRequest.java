package com.mobflow.socialservice.model.entities;

import com.mobflow.socialservice.model.enums.FriendRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "friend_requests")
@CompoundIndexes({
        @CompoundIndex(name = "idx_friend_request_target_status", def = "{'targetId': 1, 'status': 1}"),
        @CompoundIndex(name = "idx_friend_request_participants_status", def = "{'requesterId': 1, 'targetId': 1, 'status': 1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class FriendRequest {

    @Id
    private UUID id;
    private UUID requesterId;
    private String requesterUsername;
    private UUID targetId;
    private String targetUsername;
    private FriendRequestStatus status;
    @CreatedDate
    private Instant createdAt;
    private Instant respondedAt;

    public static FriendRequest create(
            UUID requesterId,
            String requesterUsername,
            UUID targetId,
            String targetUsername
    ) {
        return FriendRequest.builder()
                .id(UUID.randomUUID())
                .requesterId(requesterId)
                .requesterUsername(requesterUsername)
                .targetId(targetId)
                .targetUsername(targetUsername)
                .status(FriendRequestStatus.PENDING)
                .build();
    }

    public void accept() {
        this.status = FriendRequestStatus.ACCEPTED;
        this.respondedAt = Instant.now();
    }

    public void decline() {
        this.status = FriendRequestStatus.DECLINED;
        this.respondedAt = Instant.now();
    }
}
