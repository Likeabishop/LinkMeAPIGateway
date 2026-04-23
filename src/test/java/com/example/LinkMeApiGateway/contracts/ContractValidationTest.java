package com.example.LinkMeApiGateway.contracts;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;

class ContractValidationTest {

    private final OpenApiInteractionValidator validator =
        OpenApiInteractionValidator
            .createForSpecificationUrl("/openapi/auth-api.yml")
            .build();

    @Test
    void loginRequest_ShouldConformToContract() {
        SimpleRequest request = SimpleRequest.Builder
            .post("/auth/login")
            .withContentType("application/json")
            .withBody("""
                {"email": "test@example.com", "password": "test"}
            """)
            .build();

        SimpleResponse response = SimpleResponse.Builder
            .status(200)
            .withContentType("application/json")
            .withBody("""
                {
                  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.abc",
                  "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.xyz"
                }
            """)
            .build();

        ValidationReport report = validator.validate(request, response);

        // This is the stable assertion method
        assertFalse(
            report.hasErrors(),
            "Contract violations: " + report.getMessages()
        );
    }
}