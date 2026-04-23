package com.example.LinkMeApiGateway.exception;

import com.example.LinkMeApiGateway.exception.custom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GatewayExceptionHandler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // Determine error details based on exception type
        ErrorDetails errorDetails = determineErrorDetails(ex);
        
        response.setStatusCode(errorDetails.getStatus());
        
        // Log with appropriate level based on severity
        if (errorDetails.getStatus().is5xxServerError()) {
            logger.error("Gateway server error [{}]: {}", errorDetails.getStatus(), errorDetails.getMessage(), ex);
        } else if (errorDetails.getStatus().is4xxClientError()) {
            logger.warn("Gateway client error [{}]: {}", errorDetails.getStatus(), errorDetails.getMessage());
        } else {
            logger.info("Gateway handled error: {}", errorDetails.getMessage());
        }

        // Build enhanced error response
        String errorResponse = buildEnhancedErrorResponse(exchange, errorDetails);

        DataBufferFactory bufferFactory = response.bufferFactory();
        DataBuffer buffer = bufferFactory.wrap(errorResponse.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }

    private ErrorDetails determineErrorDetails(Throwable ex) {
        // Handle your custom exceptions
        if (ex instanceof UnauthorizedException) {
            return new ErrorDetails(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                ex.getMessage() != null ? ex.getMessage() : "Authentication required"
            );
        }
        else if (ex instanceof ServiceUnavailableException) {
            ServiceUnavailableException sue = (ServiceUnavailableException) ex;
            return new ErrorDetails(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                sue.getMessage() != null ? sue.getMessage() : "Service temporarily unavailable"
            );
        }
        else if (ex instanceof RateLimitExceededException) {
            return new ErrorDetails(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate Limit Exceeded",
                ex.getMessage() != null ? ex.getMessage() : "Too many requests. Please try again later."
            );
        }
        else if (ex instanceof BadRequestException) {
            return new ErrorDetails(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage() != null ? ex.getMessage() : "Invalid request parameters"
            );
        }
        else if (ex instanceof ForbiddenException) {
            return new ErrorDetails(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                ex.getMessage() != null ? ex.getMessage() : "Access denied"
            );
        }
        // Handle existing exceptions
        else if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            HttpStatusCode statusCode = rse.getStatusCode();
            // Convert HttpStatusCode to HttpStatus if possible
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            return new ErrorDetails(
                status,
                status.getReasonPhrase(),
                rse.getReason() != null ? rse.getReason() : "Request failed"
            );
        }
        else if (ex instanceof NotFoundException) {
            return new ErrorDetails(
                HttpStatus.NOT_FOUND,
                "Not Found",
                "The requested service is unavailable"
            );
        }
        else if (ex instanceof IllegalArgumentException) {
            return new ErrorDetails(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage() != null ? ex.getMessage() : "Invalid argument provided"
            );
        }
        // Default case
        else {
            return new ErrorDetails(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred"
            );
        }
    }

    private String buildEnhancedErrorResponse(ServerWebExchange exchange, ErrorDetails errorDetails) {
        return String.format(
            "{" +
            "\"timestamp\":\"%s\"," +
            "\"status\":%d," +
            "\"error\":\"%s\"," +
            "\"message\":\"%s\"," +
            "\"path\":\"%s\"," +
            "\"method\":\"%s\"," +
            "\"traceId\":\"%s\"" +
            "}",
            LocalDateTime.now().format(formatter),
            errorDetails.getStatus().value(),
            errorDetails.getError(),
            escapeJson(errorDetails.getMessage()),
            exchange.getRequest().getPath().toString(),
            exchange.getRequest().getMethod().toString(),
            generateTraceId() // Optional: generate unique ID for tracking
        );
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String generateTraceId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    // Inner class for error details
    private static class ErrorDetails {
        private final HttpStatus status;
        private final String error;
        private final String message;

        public ErrorDetails(HttpStatus status, String error, String message) {
            this.status = status;
            this.error = error;
            this.message = message;
        }

        public HttpStatus getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
    }
}