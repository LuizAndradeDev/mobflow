package com.mobflow.taskservice.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;
import java.util.function.Supplier;

public final class InternalCallPolicy {

    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    private InternalCallPolicy(Retry retry, CircuitBreaker circuitBreaker) {
        this.retry = retry;
        this.circuitBreaker = circuitBreaker;
    }

    public static InternalCallPolicy critical(String name) {
        return critical(name, 3, Duration.ofMillis(150), 2.0, 10, 5, 50.0f, Duration.ofSeconds(20));
    }

    public static InternalCallPolicy critical(
            String name,
            int maxAttempts,
            Duration initialBackoff,
            double backoffMultiplier,
            int slidingWindowSize,
            int minimumNumberOfCalls,
            float failureRateThreshold,
            Duration waitDurationInOpenState
    ) {
        return new InternalCallPolicy(
                retry(name, maxAttempts, initialBackoff, backoffMultiplier),
                circuitBreaker(name, slidingWindowSize, minimumNumberOfCalls, failureRateThreshold, waitDurationInOpenState)
        );
    }

    public static InternalCallPolicy retryOnly(String name) {
        return new InternalCallPolicy(retry(name, 2, Duration.ofMillis(100), 2.0), null);
    }

    public static InternalCallPolicy noOp() {
        return new InternalCallPolicy(null, null);
    }

    public <T> T execute(Supplier<T> supplier) {
        Supplier<T> decorated = supplier;
        if (retry != null) {
            decorated = Retry.decorateSupplier(retry, decorated);
        }
        if (circuitBreaker != null) {
            decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated);
        }
        return decorated.get();
    }

    private static Retry retry(String name, int maxAttempts, Duration initialBackoff, double backoffMultiplier) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(initialBackoff.toMillis(), backoffMultiplier))
                .retryOnException(InternalHttpClientSupport::isTransientFailure)
                .build();
        return Retry.of(name + "-retry", config);
    }

    private static CircuitBreaker circuitBreaker(
            String name,
            int slidingWindowSize,
            int minimumNumberOfCalls,
            float failureRateThreshold,
            Duration waitDurationInOpenState
    ) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(waitDurationInOpenState)
                .permittedNumberOfCallsInHalfOpenState(1)
                .recordException(InternalHttpClientSupport::isTransientFailure)
                .build();
        return CircuitBreaker.of(name + "-circuit-breaker", config);
    }
}
