package com.mobflow.socialservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.socialservice.client.AuthServiceClient;
import com.mobflow.socialservice.model.entities.FriendRequest;
import com.mobflow.socialservice.model.entities.Friendship;
import com.mobflow.socialservice.model.enums.FriendRequestStatus;
import com.mobflow.socialservice.repository.FriendRequestRepository;
import com.mobflow.socialservice.repository.FriendshipRepository;
import com.mobflow.socialservice.testsupport.AbstractSocialIntegrationTest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.REQUESTER_ID;
import static com.mobflow.socialservice.testsupport.FriendRequestTestFixtures.TARGET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FriendRequestFlowIntegrationTest extends AbstractSocialIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @MockBean
    private AuthServiceClient authServiceClient;

    private Consumer<String, String> friendshipConsumer;

    @BeforeEach
    void setUp() {
        friendRequestRepository.deleteAll();
        friendshipRepository.deleteAll();
        friendshipConsumer = consumer("social-friendship-events", "friend-flow-" + UUID.randomUUID());
        when(authServiceClient.resolveRequiredByUsername("mary_dev"))
                .thenReturn(new AuthServiceClient.AuthUserSummaryResponse(TARGET_ID, "mary_dev"));
    }

    @AfterEach
    void tearDown() {
        friendshipConsumer.close();
    }

    @Test
    void friendRequestFlow_sendAcceptAndListFriends_persistsStateAndPublishesEvents() throws Exception {
        String requesterToken = bearerToken(REQUESTER_ID, "john_dev");
        String targetToken = bearerToken(TARGET_ID, "mary_dev");

        String requestBody = mockMvc.perform(withSocialContextPath(post("/social/api/friends/request"))
                        .header("Authorization", requesterToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("username", "mary_dev"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode requestJson = objectMapper.readTree(requestBody);
        UUID requestId = UUID.fromString(requestJson.get("id").asText());

        mockMvc.perform(withSocialContextPath(post("/social/api/friends/{requestId}/accept", requestId))
                        .header("Authorization", targetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(withSocialContextPath(get("/social/api/friends"))
                        .header("Authorization", requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authId").value(TARGET_ID.toString()));

        FriendRequest storedRequest = friendRequestRepository.findById(requestId).orElseThrow();
        assertThat(storedRequest.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);

        List<Friendship> friendships = friendshipRepository.findByUserAOrUserB(REQUESTER_ID, REQUESTER_ID);
        assertThat(friendships).hasSize(1);

        List<ConsumerRecord<String, String>> records = new CopyOnWriteArrayList<>();
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    KafkaTestUtils.getRecords(friendshipConsumer, Duration.ofMillis(250))
                            .forEach(records::add);
                    assertThat(records).hasSizeGreaterThanOrEqualTo(2);
                });

        assertThat(records)
                .extracting(ConsumerRecord::value)
                .anySatisfy(payload -> assertThat(payload).contains("\"eventType\":\"FRIEND_REQUEST_SENT\""))
                .anySatisfy(payload -> assertThat(payload).contains("\"eventType\":\"FRIEND_REQUEST_ACCEPTED\""));
    }

    @Test
    void friendRequestFlow_duplicatePendingRequest_returnsConflict() throws Exception {
        String requesterToken = bearerToken(REQUESTER_ID, "john_dev");

        mockMvc.perform(withSocialContextPath(post("/social/api/friends/request"))
                        .header("Authorization", requesterToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("username", "mary_dev"))))
                .andExpect(status().isCreated());

        mockMvc.perform(withSocialContextPath(post("/social/api/friends/request"))
                        .header("Authorization", requesterToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("username", "mary_dev"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("FRIEND_REQUEST_ALREADY_EXISTS"));
    }
}
