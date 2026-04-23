package com.example.LinkMeApiGateway.exception.custom;

public class ServiceUnavailableException extends GatewayException {
    private final String serviceName;
    
    public ServiceUnavailableException(String serviceName) {
        super("Service " + serviceName + " is currently unavailable");
        this.serviceName = serviceName;
    }

    public ServiceUnavailableException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
}
