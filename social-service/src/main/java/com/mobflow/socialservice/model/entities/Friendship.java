package com.mobflow.socialservice.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "friendships")
@CompoundIndex(name = "idx_friendship_pair", def = "{'userA': 1, 'userB': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Friendship {

    @Id
    private UUID id;
    @Indexed
    private UUID userA;
    private String userAUsername;
    @Indexed
    private UUID userB;
    private String userBUsername;
    @CreatedDate
    private Instant createdAt;

    public static Friendship create(
            UUID userA,
            String userAUsername,
            UUID userB,
            String userBUsername
    ) {
        return Friendship.builder()
                .id(UUID.randomUUID())
                .userA(userA)
                .userAUsername(userAUsername)
                .userB(userB)
                .userBUsername(userBUsername)
                .build();
    }
}
