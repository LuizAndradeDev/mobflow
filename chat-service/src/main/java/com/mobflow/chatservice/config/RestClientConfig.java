package com.mobflow.chatservice.config;

import com.mobflow.chatservice.observability.CorrelationIdClientHttpRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("socialRestClient")
    public RestClient socialRestClient(
            @Value("${social.service.url}") String baseUrl,
            CorrelationIdClientHttpRequestInterceptor correlationIdInterceptor
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }
}
