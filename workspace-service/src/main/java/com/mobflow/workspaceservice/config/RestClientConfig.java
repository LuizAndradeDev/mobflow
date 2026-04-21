package com.mobflow.workspaceservice.config;

import com.mobflow.workspaceservice.observability.CorrelationIdClientHttpRequestInterceptor;
import com.mobflow.workspaceservice.resilience.InternalCallPolicy;
import com.mobflow.workspaceservice.resilience.InternalHttpClientSupport;
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

    @Bean("userLookupPolicy")
    public InternalCallPolicy userLookupPolicy() {
        return InternalCallPolicy.critical("workspace-user-lookup");
    }

    @Bean("userProfileLookupPolicy")
    public InternalCallPolicy userProfileLookupPolicy() {
        return InternalCallPolicy.retryOnly("workspace-user-profile-lookup");
    }
}
