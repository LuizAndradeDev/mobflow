package com.mobflow.socialservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobflow.socialservice.client.AuthServiceClient;
import com.mobflow.socialservice.client.TaskServiceClient;
import com.mobflow.socialservice.client.WorkspaceServiceClient;
import com.mobflow.socialservice.model.entities.Comment;
import com.mobflow.socialservice.model.enums.WorkspaceRole;
import com.mobflow.socialservice.repository.CommentRepository;
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

import static com.mobflow.socialservice.testsupport.CommentTestFixtures.TASK_ASSIGNEE_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.TASK_CREATOR_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.TASK_ID;
import static com.mobflow.socialservice.testsupport.CommentTestFixtures.WORKSPACE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentFlowIntegrationTest extends AbstractSocialIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentRepository commentRepository;

    @MockBean
    private TaskServiceClient taskServiceClient;

    @MockBean
    private WorkspaceServiceClient workspaceServiceClient;

    @MockBean
    private AuthServiceClient authServiceClient;

    private Consumer<String, String> commentConsumer;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        commentConsumer = consumer("social-comment-events", "comment-flow-" + UUID.randomUUID());
        when(taskServiceClient.getTaskContext(TASK_ID))
                .thenReturn(new TaskServiceClient.TaskCommentContextResponse(
                        TASK_ID,
                        WORKSPACE_ID,
                        TASK_CREATOR_ID,
                        TASK_ASSIGNEE_ID,
                        "Prepare roadmap"
                ));
        when(workspaceServiceClient.requireMembership(eq(WORKSPACE_ID), org.mockito.ArgumentMatchers.any(UUID.class)))
                .thenReturn(WorkspaceRole.MEMBER);
        when(authServiceClient.resolveByUsernames(anyList()))
                .thenReturn(java.util.Map.of("mary_dev", new AuthServiceClient.AuthUserSummaryResponse(UUID.randomUUID(), "mary_dev")));
    }

    @AfterEach
    void tearDown() {
        commentConsumer.close();
    }

    @Test
    void commentFlow_createListEditDeleteComment_persistsChangesAndPublishesEvents() throws Exception {
        UUID actorId = UUID.randomUUID();
        String token = bearerToken(actorId, "john_dev");

        String createResponse = mockMvc.perform(withSocialContextPath(
                        post("/social/api/tasks/{taskId}/comments", TASK_ID))
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "Hello @mary_dev team"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(TASK_ID.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID commentId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        mockMvc.perform(withSocialContextPath(
                        get("/social/api/tasks/{taskId}/comments", TASK_ID))
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(commentId.toString()));

        mockMvc.perform(withSocialContextPath(
                        put("/social/api/comments/{commentId}", commentId))
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("content", "Updated comment"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated comment"));

        mockMvc.perform(withSocialContextPath(
                        delete("/social/api/comments/{commentId}", commentId))
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        Comment storedComment = commentRepository.findById(commentId).orElseThrow();
        assertThat(storedComment.isDeleted()).isTrue();
        assertThat(storedComment.getContent()).isEmpty();

        List<ConsumerRecord<String, String>> records = new CopyOnWriteArrayList<>();
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    KafkaTestUtils.getRecords(commentConsumer, Duration.ofMillis(250))
                            .forEach(records::add);
                    assertThat(records).hasSizeGreaterThanOrEqualTo(3);
                });

        assertThat(records)
                .extracting(ConsumerRecord::value)
                .anySatisfy(payload -> assertThat(payload).contains("\"eventType\":\"COMMENT_CREATED\""))
                .anySatisfy(payload -> assertThat(payload).contains("\"eventType\":\"USER_MENTIONED\""));
    }
}
