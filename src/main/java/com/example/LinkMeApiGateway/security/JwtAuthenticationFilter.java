package com.example.LinkMeApiGateway.security;

import com.example.LinkMeApiGateway.exception.custom.UnauthorizedException;
import com.example.LinkMeApiGateway.exception.custom.ForbiddenException;
import com.example.LinkMeApiGateway.util.JwtUtil;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getPath().value();

        // Skip non-API routes
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new UnauthorizedException("Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            return Mono.error(new UnauthorizedException("Invalid or expired JWT token"));
        }

        String roles = jwtUtil.extractRoles(token);

        if (path.startsWith("/api/admin/") && (roles == null || !roles.contains("ADMIN"))) {
            return Mono.error(new ForbiddenException("Admin access required"));
        }

        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(r -> r
                .header("X-User-Name", jwtUtil.extractUsername(token))
                .header("X-User-Id", jwtUtil.extractUserId(token))
                .header("X-User-Roles", roles)
            )
            .build();

        return chain.filter(mutatedExchange);
    }

}