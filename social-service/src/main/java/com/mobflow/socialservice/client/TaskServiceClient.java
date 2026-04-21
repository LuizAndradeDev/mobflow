package com.mobflow.socialservice.client;

import com.mobflow.socialservice.exception.SocialServiceException;
import com.mobflow.socialservice.resilience.InternalCallPolicy;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final InternalCallPolicy taskContextPolicy;

    @Autowired
    public TaskServiceClient(
            @Qualifier("taskRestClient") RestClient restClient,
            @Value("${internal.secret}") String internalSecret,
            @Qualifier("taskContextPolicy") InternalCallPolicy taskContextPolicy
    ) {
        this.restClient = restClient;
        this.internalSecret = internalSecret;
        this.taskContextPolicy = taskContextPolicy;
    }

    public TaskServiceClient(RestClient restClient, String internalSecret) {
        this(restClient, internalSecret, InternalCallPolicy.noOp());
    }

    public TaskCommentContextResponse getTaskContext(UUID taskId) {
        try {
            TaskCommentContextResponse response = taskContextPolicy.execute(() -> restClient.get()
                    .uri("/tasks/internal/tasks/{taskId}/context", taskId)
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .retrieve()
                    .body(TaskCommentContextResponse.class));

            if (response == null) {
                throw SocialServiceException.taskNotFound();
            }
            return response;
        } catch (HttpClientErrorException.NotFound exception) {
            throw SocialServiceException.taskNotFound();
        } catch (CallNotPermittedException exception) {
            throw SocialServiceException.upstreamServiceError();
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
