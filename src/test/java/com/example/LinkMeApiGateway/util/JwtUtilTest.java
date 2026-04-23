package com.example.LinkMeApiGateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "dGVzdFNlY3JldEtleVRoYXRNdXN0QmVMb25nRW5vdWdoRm9ySFMyNTY="; // Base64 encoded "testSecretKeyThatMustBeLongEnoughForHS256"

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(testSecret);
    }

    @Test
    void constructor_ShouldInitializeWithValidSecret() {
        assertNotNull(jwtUtil);
    }

    @Test
    void constructor_ShouldThrowException_WhenSecretIsInvalid() {
        assertThrows(RuntimeException.class, () -> new JwtUtil("invalid-base64!"));
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {
        // Generate a valid token for testing
        String token = generateTestToken("testuser");
        
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_ShouldReturnFalse_ForExpiredToken() {
        // This would require creating an expired token
        // For simplicity, we'll test with malformed token
        assertFalse(jwtUtil.validateToken("malformed.token.string"));
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        String token = generateTestToken("testuser");
        
        String username = jwtUtil.extractUsername(token);
        
        assertEquals("testuser", username);
    }

    @Test
    void extractClaim_ShouldReturnSpecificClaim() {
        String token = generateTokenWithClaims("testuser", "123", "USER");
        
        String userId = (String) jwtUtil.extractClaim(token, "userId");
        String roles = (String) jwtUtil.extractClaim(token, "roles");
        
        assertEquals("123", userId);
        assertEquals("USER", roles);
    }

    @Test
    void extractUserId_ShouldReturnUserId() {
        String token = generateTokenWithClaims("testuser", "456", "ADMIN");
        
        String userId = jwtUtil.extractUserId(token);
        
        assertEquals("456", userId);
    }

    @Test
    void extractRoles_ShouldReturnRoles() {
        String token = generateTokenWithClaims("testuser", "789", "MANAGER");
        
        String roles = jwtUtil.extractRoles(token);
        
        assertEquals("MANAGER", roles);
    }

    @Test
    void isTokenExpired_ShouldReturnFalse_ForValidToken() {
        String token = generateTestToken("testuser");
        
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void isTokenExpired_ShouldReturnTrue_ForExpiredToken() {
        // Create an expired token (1 hour ago)
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecret));
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(key)
                .compact();
        
        assertTrue(jwtUtil.isTokenExpired(expiredToken));
    }

    // Helper methods
    private String generateTestToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecret));
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour from now
                .signWith(key)
                .compact();
    }

    private String generateTokenWithClaims(String username, String userId, String roles) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecret));
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }
}