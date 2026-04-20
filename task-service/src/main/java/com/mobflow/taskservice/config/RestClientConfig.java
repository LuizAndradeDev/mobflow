package com.mobflow.taskservice.config;

import com.mobflow.taskservice.observability.CorrelationIdClientHttpRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("workspaceRestClient")
    public RestClient workspaceRestClient(
            @Value("${workspace.service.url}") String baseUrl,
            CorrelationIdClientHttpRequestInterceptor correlationIdInterceptor
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @Bean("userRestClient")
    public RestClient userRestClient(
            @Value("${user.service.url}") String baseUrl,
            CorrelationIdClientHttpRequestInterceptor correlationIdInterceptor
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }
}
