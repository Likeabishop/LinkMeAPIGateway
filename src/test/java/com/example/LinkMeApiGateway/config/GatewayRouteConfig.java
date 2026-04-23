package com.example.LinkMeApiGateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    @Value("${AUTH_SERVICE_ROUTES}")
    private String authServiceUrl;

    @Value("${PROTECTED_SERVICE_ROUTES}")
    private String protectedServiceUrl;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth", r -> r.path("/auth/**").uri(authServiceUrl))
            .route("api", r -> r.path("/api/**").uri(protectedServiceUrl))
            .build();
    }
}