package com.edupulse.controller;

import com.edupulse.seeder.DataSeeder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Administrative demo-showcase provisioning.
 *
 * <p>Security model (see docs/security.md):
 * <ul>
 *   <li>Requires an authenticated ADMIN principal (never reachable anonymously).</li>
 *   <li>Only works when {@code app.seeder.enabled=true}; otherwise the DataSeeder bean does not exist
 *       and the endpoint answers 409 without touching any data.</li>
 *   <li>The seeder itself only creates or refreshes accounts whose auth provider is {@code DEMO};
 *       real (LOCAL / SSO) accounts are never modified.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/demo")
public class AdminDemoController {

    private final ObjectProvider<DataSeeder> dataSeeder;

    public AdminDemoController(ObjectProvider<DataSeeder> dataSeeder) {
        this.dataSeeder = dataSeeder;
    }

    @PostMapping("/provision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> provision() {
        DataSeeder seeder = dataSeeder.getIfAvailable();
        if (seeder == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "message", "Demo seeding is disabled (app.seeder.enabled=false). No data was changed."
            ));
        }
        boolean success = seeder.seedDemoData();
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", "Demo showcase accounts provisioned or refreshed (idempotent; non-demo accounts untouched)."
        ));
    }
}
