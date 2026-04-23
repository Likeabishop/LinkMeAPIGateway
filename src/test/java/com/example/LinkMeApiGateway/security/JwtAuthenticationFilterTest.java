package com.example.LinkMeApiGateway.security;

import com.example.LinkMeApiGateway.exception.custom.UnauthorizedException;
import com.example.LinkMeApiGateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private WebFilterChain filterChain;

    @Captor
    private ArgumentCaptor<ServerWebExchange> exchangeCaptor;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtil);
        lenient().when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/auth/login",
        "/auth/register",
        "/auth/register-admin",
        "/auth/logout",
        "/auth/refresh-token",
        "/auth/verify-email",
        "/auth/request-password-reset",
        "/auth/reset-password"
    })
    void filter_ShouldAllowRequestToPublicEndpoint(@NonNull String path) {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get(path)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(filterChain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void filter_ShouldAllowHealthEndpoint() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/actuator/health")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(filterChain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void filter_ShouldRejectRequestWithoutAuthorizationHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, "test-route");
        
        // When/Then
        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectError(UnauthorizedException.class)
                .verify();
                
        verify(filterChain, never()).filter(any());
    }

    @Test
    void filter_ShouldRejectRequestWithInvalidAuthorizationHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz") // Basic auth, not Bearer
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, "test-route");

        // When/Then
        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectError(UnauthorizedException.class)
                .verify();
                
        verify(filterChain, never()).filter(any());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void filter_ShouldValidateTokenAndProceed_WhenTokenIsValid() {
        // Given
        String validToken = "valid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected") // Make sure this matches your protected path pattern
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock the route attribute to simulate a matched route
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, "test-route");

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.extractRoles(validToken)).thenReturn("USER");
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(jwtUtil).validateToken(validToken);
        verify(jwtUtil).extractRoles(validToken);
        verify(filterChain).filter(any());
    }

    @Test
    void filter_ShouldRejectRequest_WhenTokenIsInvalid() {
        // Given
        String invalidToken = "invalid.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, "test-route");

        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        // When/Then
        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectError(UnauthorizedException.class)
                .verify();
                
        verify(filterChain, never()).filter(any());
        verify(jwtUtil).validateToken(invalidToken);
    }

    @Test
    void filter_ShouldHandleNullUserInfoGracefully() {
        // Given
        String token = "valid.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        lenient().when(jwtUtil.validateToken(token)).thenReturn(true);
        lenient().when(jwtUtil.extractUsername(token)).thenReturn(null);
        lenient().when(jwtUtil.extractUserId(token)).thenReturn(null);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(any());
    }

    @Test
    void filter_ShouldForwardUserInfoAsHeaders_WhenTokenIsValid() {
        // Given
        String validToken = "valid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/protected")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, "test-route");

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.extractUsername(validToken)).thenReturn("john@example.com");
        when(jwtUtil.extractUserId(validToken)).thenReturn("user-123");
        when(jwtUtil.extractRoles(validToken)).thenReturn("USER");
        when(filterChain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        // Then — verify downstream receives user info as headers
        ServerWebExchange captured = exchangeCaptor.getValue();
        HttpHeaders headers = captured.getRequest().getHeaders();
        assertEquals("john@example.com", headers.getFirst("X-User-Name"));
        assertEquals("user-123", headers.getFirst("X-User-Id"));
        assertEquals("USER", headers.getFirst("X-User-Roles"));
     }
}