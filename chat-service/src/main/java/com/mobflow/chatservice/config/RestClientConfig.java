package com.mobflow.chatservice.config;

import com.mobflow.chatservice.observability.CorrelationIdClientHttpRequestInterceptor;
import com.mobflow.chatservice.resilience.InternalCallPolicy;
import com.mobflow.chatservice.resilience.InternalHttpClientSupport;
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

    @Bean("socialRestClient")
    public RestClient socialRestClient(
            @Value("${social.service.url}") String baseUrl,
            ClientHttpRequestFactory internalClientHttpRequestFactory,
            CorrelationIdClientHttpRequestInterceptor correlationIdInterceptor
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(internalClientHttpRequestFactory)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }

    @Bean("socialFriendshipPolicy")
    public InternalCallPolicy socialFriendshipPolicy() {
        return InternalCallPolicy.critical("chat-social-friendship");
    }
}
