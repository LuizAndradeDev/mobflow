package com.mobflow.authservice.middlewares;

import com.mobflow.authservice.services.JWTService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

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
    private JWTService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userDetailsService, handlerExceptionResolver);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueFilterChainWhenAuthorizationHeaderIsMissing() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldContinueFilterChainWhenAuthorizationHeaderIsNotBearer() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldAuthenticateUserWhenTokenIsValid() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserDetails userDetails = new User("john", "encoded-password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(jwtService.extractUsername("valid-token")).thenReturn("john");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("john");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldDelegateToExceptionResolverWhenTokenProcessingThrows() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("invalid-token")).thenThrow(new IllegalArgumentException("Malformed token"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(handlerExceptionResolver).resolveException(
                eq(request),
                eq(response),
                isNull(),
                any(IllegalArgumentException.class)
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldNotReauthenticateWhenSecurityContextAlreadyContainsAuthentication() throws Exception {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("already-authenticated", null, List.of())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("valid-token")).thenReturn("john");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("already-authenticated");
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
        verify(filterChain).doFilter(request, response);
    }
}
