package com.mobflow.workspaceservice.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String MDC_HTTP_METHOD_KEY = "httpMethod";
    private static final String MDC_HTTP_PATH_KEY = "httpPath";
    private static final String MDC_HTTP_STATUS_KEY = "httpStatus";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
        long startedAt = System.nanoTime();

        MDC.put(MDC_KEY, correlationId);
        MDC.put(MDC_HTTP_METHOD_KEY, request.getMethod());
        MDC.put(MDC_HTTP_PATH_KEY, request.getRequestURI());
        response.setHeader(HEADER_NAME, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.put(MDC_HTTP_STATUS_KEY, String.valueOf(response.getStatus()));
            if (shouldLogRequest(request)) {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
                log.info("http_request_completed durationMs={}", durationMs);
            }
            MDC.remove(MDC_HTTP_STATUS_KEY);
            MDC.remove(MDC_HTTP_PATH_KEY);
            MDC.remove(MDC_HTTP_METHOD_KEY);
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return UUID.randomUUID().toString();
        }

        String correlationId = candidate.strip();
        return isValidCorrelationId(correlationId) ? correlationId : UUID.randomUUID().toString();
    }

    private boolean isValidCorrelationId(String correlationId) {
        if (correlationId.length() < 8 || correlationId.length() > 128) {
            return false;
        }

        for (int i = 0; i < correlationId.length(); i++) {
            char character = correlationId.charAt(i);
            boolean valid = (character >= 'a' && character <= 'z')
                    || (character >= 'A' && character <= 'Z')
                    || (character >= '0' && character <= '9')
                    || character == '-'
                    || character == '_'
                    || character == '.'
                    || character == ':';
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldLogRequest(HttpServletRequest request) {
        return !request.getRequestURI().contains("/actuator");
    }
}
