package com.mobflow.userservice.observability;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class CorrelationIdClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (StringUtils.hasText(correlationId) && !request.getHeaders().containsKey(CorrelationIdFilter.HEADER_NAME)) {
            request.getHeaders().set(CorrelationIdFilter.HEADER_NAME, correlationId);
        }
        return execution.execute(request, body);
    }
}
