package com.example.LinkMeApiGateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey key;
    private final String secretString;

    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        this.secretString = secret;
        try {
            // Decode the Base64 encoded secret (same as auth service)
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
            logger.info("JwtUtil initialized successfully with provided secret");
        } catch (IllegalArgumentException e) {
            logger.error("Failed to decode JWT secret. Ensure it's a valid Base64 string: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT secret key format", e);
        }
    }

    /**
     * Parse and validate JWT token
     * @param token The JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            logger.debug("Token validated successfully");
            return true;
        } catch (ExpiredJwtException e) {
            logger.warn("Token has expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract username/email from token
     * @param token The JWT token
     * @return username/subject from token
     */
    public String extractUsername(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String username = claims.getSubject();
            logger.debug("Extracted username: {}", username);
            return username;
        } catch (Exception e) {
            logger.error("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract specific claim by key
     * @param token The JWT token
     * @param claimKey The claim key to extract
     * @return The claim value as Object
     */
    public Object extractClaim(String token, String claimKey) {
        try {
            Claims claims = extractAllClaims(token);
            Object claim = claims.get(claimKey);
            logger.debug("Extracted claim '{}': {}", claimKey, claim);
            return claim;
        } catch (Exception e) {
            logger.error("Failed to extract claim '{}': {}", claimKey, e.getMessage());
            return null;
        }
    }

    /**
     * Extract user ID claim
     * @param token The JWT token
     * @return user ID as String
     */
    public String extractUserId(String token) {
        Object userId = extractClaim(token, "userId");
        return userId != null ? userId.toString() : null;
    }

    /**
     * Extract roles from token
     * @param token The JWT token
     * @return roles as String
     */
    public String extractRoles(String token) {
        Object roles = extractClaim(token, "roles");
        return roles != null ? roles.toString() : null;
    }

    /**
     * Check if token is expired
     * @param token The JWT token
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            boolean expired = expiration.before(new Date());
            if (expired) {
                logger.debug("Token expired at: {}", expiration);
            }
            return expired;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Extract all claims from token
     * @param token The JWT token
     * @return Claims object
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get the secret key string (for debugging, use carefully)
     */
    public String getSecretString() {
        return secretString;
    }
}