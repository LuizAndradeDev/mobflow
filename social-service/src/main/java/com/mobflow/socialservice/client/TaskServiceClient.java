package com.mobflow.socialservice.client;

import com.mobflow.socialservice.exception.SocialServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class TaskServiceClient {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient restClient;
    private final String internalSecret;

    public TaskServiceClient(
            @Qualifier("taskRestClient") RestClient restClient,
            @Value("${internal.secret}") String internalSecret
    ) {
        this.restClient = restClient;
        this.internalSecret = internalSecret;
    }

    public TaskCommentContextResponse getTaskContext(UUID taskId) {
        try {
            TaskCommentContextResponse response = restClient.get()
                    .uri("/tasks/internal/tasks/{taskId}/context", taskId)
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .retrieve()
                    .body(TaskCommentContextResponse.class);

            if (response == null) {
                throw SocialServiceException.taskNotFound();
            }
            return response;
        } catch (HttpClientErrorException.NotFound exception) {
            throw SocialServiceException.taskNotFound();
        } catch (RestClientException exception) {
            throw SocialServiceException.upstreamServiceError();
        }
    }

    public record TaskCommentContextResponse(
            UUID taskId,
            UUID workspaceId,
            UUID boardId,
            UUID createdByAuthId,
            UUID assigneeAuthId,
            String taskTitle
    ) {
    }
}
