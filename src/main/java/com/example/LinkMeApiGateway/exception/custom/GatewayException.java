package com.example.LinkMeApiGateway.exception.custom;

public class GatewayException extends RuntimeException {
    public GatewayException(String message) {
        super(message);
    }
}
