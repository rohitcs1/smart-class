package com.elneb.health;

import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "elneb-backend", "timestamp", Instant.now());
    }
}
