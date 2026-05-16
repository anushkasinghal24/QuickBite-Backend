package com.quickbite.auth_service.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> home() {
        return Map.of(
                "service", "auth-service",
                "status", "running",
                "endpoints", "/api/v1/auth/*"
        );
    }
}
