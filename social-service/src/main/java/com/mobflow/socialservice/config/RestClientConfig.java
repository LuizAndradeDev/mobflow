package com.mobflow.socialservice.config;

import com.mobflow.socialservice.observability.CorrelationIdClientHttpRequestInterceptor;
import com.mobflow.socialservice.resilience.InternalCallPolicy;
import com.mobflow.socialservice.resilience.InternalHttpClientSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public ClientHttpRequestFactory internalClientHttpRequestFactory(
            @Value("${internal.http.connect-timeout:500ms}") Duration connectTimeout,
            @Value("${internal.http.read-timeout:2s}") Duration readTimeout
    ) {
        return InternalHttpClientSupport.requestFactory(connectTimeout, readTimeout);
    }

    @Bean("authRestClient")
    public RestClient authRestClient(
            @Value("${auth.service.url}") String baseUrl,
            ClientHttpRequestFactory internalClientHttpRequestFactory,
            CorrelationIdClientHttpRequestInterceptor correlationIdInterceptor
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(internalClientHttpRequestFactory)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @Bean("workspaceRestClient")
    public RestClient workspaceRestClient(
            @Value("${workspace.service.url}") String baseUrl,
            ClientHttpRequestFactory internalClientHttpRequestFactory,
            CorrelationIdClientHttpRequestInterceptor correlationIdInterceptor
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(internalClientHttpRequestFactory)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @Bean("taskRestClient")
    public RestClient taskRestClient(
            @Value("${task.service.url}") String baseUrl,
            ClientHttpRequestFactory internalClientHttpRequestFactory,
            CorrelationIdClientHttpRequestInterceptor correlationIdInterceptor
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(internalClientHttpRequestFactory)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @Bean("userRestClient")
    public RestClient userRestClient(
            @Value("${user.service.url}") String baseUrl,
            ClientHttpRequestFactory internalClientHttpRequestFactory,
            CorrelationIdClientHttpRequestInterceptor correlationIdInterceptor
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(internalClientHttpRequestFactory)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @Bean("authUserLookupPolicy")
    public InternalCallPolicy authUserLookupPolicy() {
        return InternalCallPolicy.critical("social-auth-user-lookup");
    }

    @Bean("authMentionLookupPolicy")
    public InternalCallPolicy authMentionLookupPolicy() {
        return InternalCallPolicy.retryOnly("social-auth-mention-lookup");
    }

    @Bean("taskContextPolicy")
    public InternalCallPolicy taskContextPolicy() {
        return InternalCallPolicy.critical("social-task-context");
    }

    @Bean("workspaceMembershipPolicy")
    public InternalCallPolicy workspaceMembershipPolicy() {
        return InternalCallPolicy.critical("social-workspace-membership");
    }

    @Bean("userProfileLookupPolicy")
    public InternalCallPolicy userProfileLookupPolicy() {
        return InternalCallPolicy.retryOnly("social-user-profile-lookup");
    }
}
