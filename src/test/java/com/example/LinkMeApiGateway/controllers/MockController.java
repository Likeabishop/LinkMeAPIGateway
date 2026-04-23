package com.example.LinkMeApiGateway.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
class MockController {

    @RequestMapping("/mock/**")
    public Mono<ResponseEntity<String>> mock() {
        return Mono.just(ResponseEntity.ok("mock response"));
    }
}
