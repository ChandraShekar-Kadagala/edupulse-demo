package com.edupulse.controller;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Public liveness check. Exposes only the applied schema version, never data or counts. */
@RestController
public class HealthController {

    private final ObjectProvider<Flyway> flyway;

    public HealthController(ObjectProvider<Flyway> flyway) {
        this.flyway = flyway;
    }

    @GetMapping({"/health", "/api/health"})
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "edupulse-backend");
        body.put("timestamp", LocalDateTime.now());
        Flyway f = flyway.getIfAvailable();
        if (f != null) {
            var current = f.info().current();
            body.put("schemaVersion", current != null && current.getVersion() != null ? current.getVersion().getVersion() : null);
        }
        return ResponseEntity.ok(body);
    }
}
