package com.mobflow.socialservice.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void handleCommentNotFoundException_returnsNotFoundProblemDetail() throws Exception {
        mockMvc.perform(get("/comment-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("COMMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Comment not found"));
    }

    @Test
    void handleDuplicateFriendRequest_returnsConflictProblemDetail() throws Exception {
        mockMvc.perform(get("/duplicate-friend-request"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("FRIEND_REQUEST_ALREADY_EXISTS"));
    }

    @Test
    void handleUnauthorizedAction_returnsForbiddenProblemDetail() throws Exception {
        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("ACCESS_DENIED"));
    }

    @Test
    void handleValidationError_invalidPayload_returnsBadRequestProblemDetail() throws Exception {
        mockMvc.perform(post("/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors[0]").exists());
    }

    @Test
    void handleFallbackException_unexpectedError_returnsInternalServerErrorProblemDetail() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"));
    }

    @RestController
    static class TestController {

        @GetMapping("/comment-not-found")
        void commentNotFound() {
            throw SocialServiceException.commentNotFound();
        }

        @GetMapping("/duplicate-friend-request")
        void duplicateFriendRequest() {
            throw SocialServiceException.friendRequestAlreadyExists();
        }

        @GetMapping("/access-denied")
        void accessDenied() {
            throw SocialServiceException.accessDenied();
        }

        @PostMapping("/validation")
        void validate(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("boom");
        }
    }

    record ValidationRequest(@NotBlank String content) {
    }
}
