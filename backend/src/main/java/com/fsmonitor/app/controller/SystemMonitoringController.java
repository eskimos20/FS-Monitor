package com.fsmonitor.app.controller;

import com.fsmonitor.app.dto.SystemStats;
import com.fsmonitor.app.service.SystemMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemMonitoringController {

    private final SystemMonitoringService systemMonitoringService;

    public SystemMonitoringController(SystemMonitoringService systemMonitoringService) {
        this.systemMonitoringService = systemMonitoringService;
    }

    @GetMapping("/stats")
    public ResponseEntity<SystemStats> getSystemStats() {
        SystemStats stats = systemMonitoringService.getSystemStats();
        return ResponseEntity.ok(stats);
    }
}
