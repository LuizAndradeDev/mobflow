package com.mobflow.socialservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, handlerExceptionResolver);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_missingAuthorizationHeader_continuesWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    void doFilterInternal_invalidAuthorizationHeaderPrefix_continuesWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Token abc");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    void doFilterInternal_validToken_setsSecurityContextAuthentication() throws Exception {
        UUID authId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn("Bearer token-123");
        when(jwtService.extractUsername("token-123")).thenReturn("john_dev");
        when(jwtService.extractAuthId("token-123")).thenReturn(authId);
        when(jwtService.isTokenValid("token-123")).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("john_dev");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials()).isEqualTo(authId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_invalidToken_resolvesExceptionAndStopsChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-123");
        when(jwtService.extractUsername("token-123")).thenThrow(new IllegalArgumentException("invalid token"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(handlerExceptionResolver).resolveException(eq(request), eq(response), isNull(), any(IllegalArgumentException.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_existingAuthentication_keepsCurrentContext() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing", UUID.randomUUID(), List.of())
        );
        when(request.getHeader("Authorization")).thenReturn("Bearer token-123");
        when(jwtService.extractUsername("token-123")).thenReturn("john_dev");
        when(jwtService.extractAuthId("token-123")).thenReturn(UUID.randomUUID());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).isTokenValid("token-123");
        verify(filterChain).doFilter(request, response);
    }
}
