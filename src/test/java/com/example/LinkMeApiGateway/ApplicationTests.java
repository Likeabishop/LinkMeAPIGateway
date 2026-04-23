package com.example.LinkMeApiGateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;


@SpringBootTest(
    classes = Application.class,
    properties = {
        "spring.cloud.gateway.enabled=false",
        "spring.main.lazy-initialization=true"
    }
)
@ActiveProfiles("test")
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
