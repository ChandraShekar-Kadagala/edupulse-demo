package com.edupulse.controller;

import com.edupulse.dto.TimetableSlotDto;
import com.edupulse.service.TimetableService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
@RestController
@RequestMapping("/api/timetable")
@lombok.RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<List<TimetableSlotDto>> getTimetable() {
        return ResponseEntity.ok(timetableService.getCommonTimetable());
    }
}
