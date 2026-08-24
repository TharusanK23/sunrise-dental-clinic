package com.sunrise.dentalclinic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/** Unauthenticated liveness endpoint used by SETUP.md's "verify the backend is running" step and by automated tests. */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "dental-clinic-backend",
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
