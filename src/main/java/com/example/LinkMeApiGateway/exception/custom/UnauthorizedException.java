package com.example.LinkMeApiGateway.exception.custom;

public class UnauthorizedException extends GatewayException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
