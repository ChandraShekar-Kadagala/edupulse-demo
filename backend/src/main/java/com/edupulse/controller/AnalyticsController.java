package com.edupulse.controller;

import com.edupulse.service.InterventionEffectivenessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final InterventionEffectivenessService effectivenessService;

    /** Observational intervention outcomes (faculty and admin only; never public). */
    @GetMapping("/interventions/effectiveness")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<InterventionEffectivenessService.EffectivenessReport> interventionEffectiveness() {
        return ResponseEntity.ok(effectivenessService.report());
    }
}
