package com.edupulse.controller;

import com.edupulse.security.UserDetailsImpl;
import com.edupulse.service.CsvImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping({"/api/data-import", "/api/v1/data-import"})
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
public class DataImportController {

    @Autowired
    private CsvImportService csvImportService;

    @PostMapping("/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<?> importCsvData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "atomic", defaultValue = "false") boolean atomic,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty or missing"));
        }

        try {
            Long userId = userDetails != null ? userDetails.getId() : null;
            Map<String, Object> result = csvImportService.processCsv(file, userId, atomic);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
