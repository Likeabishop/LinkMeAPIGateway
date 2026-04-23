package com.example.LinkMeApiGateway.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayIT {

    static WireMockServer wireMockServer;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        WireMock.configureFor("localhost", wireMockServer.port());

        stubFor(post(urlEqualTo("/auth/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                    "token": "mock-jwt-token",
                    "user": "test"
                    }
                """)));

        stubFor(post(urlEqualTo("/auth/register"))
        .willReturn(aResponse()
            .withStatus(201)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {
                "name": "John",
                "surname": "Doe",
                "email": "john@example.com",
                "role": "USER",
                "status": "ACTIVE"
                }
            """)));

        stubFor(post(urlEqualTo("/auth/register-admin"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                    "name": "Admin",
                    "surname": "User",
                    "email": "admin@example.com",
                    "role": "ADMIN",
                    "status": "ACTIVE"
                    }
                """)));

        stubFor(post(urlEqualTo("/auth/logout"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("Logged out successfully")));

        stubFor(post(urlEqualTo("/auth/refresh-token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                    "accessToken": "new-access-token",
                    "refreshToken": "new-refresh-token"
                    }
                """)));

        stubFor(get(urlPathEqualTo("/auth/verify-email"))
            .willReturn(aResponse()
                .withStatus(302)
                .withHeader("Location", "/")));

        stubFor(post(urlEqualTo("/auth/request-password-reset"))
            .willReturn(aResponse()
                .withStatus(200)));

        stubFor(post(urlPathEqualTo("/auth/reset-password"))
            .willReturn(aResponse()
                .withStatus(200)));
        }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_ROUTES",
            () -> "http://localhost:" + wireMockServer.port());

        registry.add("PROTECTED_SERVICE_ROUTES",
            () -> "http://localhost:" + wireMockServer.port());
    }

    @AfterAll
    static void stopMockServer() {
        wireMockServer.stop();
    }

    @Test
    void publicAuthEndpoint_ShouldBeAccessibleWithoutToken() {
        webTestClient.post()
                .uri("/auth/login")
                .header("Content-Type", "application/json") 
                .bodyValue("{\"username\":\"test\",\"password\":\"test\"}")
                .exchange()
                .expectStatus().isOk(); // Assuming auth service returns 200
    }

    @Test
    void registerEndpoint_ShouldBeAccessibleWithoutToken() {
        webTestClient.post()
                .uri("/auth/register")
                .header("Content-Type", "application/json")
                .bodyValue("""
                    {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john@example.com",
                    "password": "password123"
                    }
                """)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void registerAdminEndpoint_ShouldBeAccessibleWithoutToken() {
        webTestClient.post()
                .uri("/auth/register-admin")
                .header("Content-Type", "application/json")
                .bodyValue("""
                    {
                    "firstName": "Admin",
                    "lastName": "User",
                    "email": "admin@example.com",
                    "password": "password123"
                    }
                """)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void logoutEndpoint_ShouldBeAccessibleWithoutToken() {
        webTestClient.post()
                .uri("/auth/logout")
                .header("Content-Type", "application/json")
                .bodyValue("""
                    {
                    "accessToken": "mock-access-token",
                    "refreshToken": "mock-refresh-token"
                    }
                """)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void refreshTokenEndpoint_ShouldBeAccessibleWithoutToken() {
        webTestClient.post()
                .uri("/auth/refresh-token")
                .header("Content-Type", "application/json")
                .bodyValue("""
                    {
                    "refreshToken": "mock-refresh-token"
                    }
                """)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void verifyEmailEndpoint_ShouldBeAccessibleWithoutToken() {
        webTestClient.get()
                .uri("/auth/verify-email?verificationToken=mock-token")
                .exchange()
                .expectStatus().isFound();
    }

    @Test
    void requestPasswordResetEndpoint_ShouldBeAccessibleWithoutToken() {
        webTestClient.post()
                .uri("/auth/request-password-reset")
                .header("Content-Type", "application/json")
                .bodyValue("""
                    {
                    "email": "john@example.com"
                    }
                """)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void resetPasswordEndpoint_ShouldBeAccessibleWithoutToken() {
        webTestClient.post()
                .uri("/auth/reset-password?resetPasswordToken=mock-token")
                .header("Content-Type", "application/json")
                .bodyValue("""
                    {
                    "password": "newPassword123"
                    }
                """)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void protectedEndpoint_ShouldReturnUnauthorized_WithoutToken() {
        webTestClient.get()
                .uri("/api/protected-resource")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpoint_ShouldReturnUnauthorized_WithInvalidToken() {
        webTestClient.get()
                .uri("/api/protected-resource")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void healthEndpoint_ShouldBeAccessible() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void nonExistentRoute_ShouldReturn404() {
        webTestClient.get()
                .uri("/non/existent/route")
                .exchange()
                .expectStatus().isNotFound();
    }

}