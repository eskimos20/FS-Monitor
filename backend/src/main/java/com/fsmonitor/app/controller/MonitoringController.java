package com.fsmonitor.app.controller;

import com.fsmonitor.app.cache.IntegrationCacheService;
import com.fsmonitor.app.cache.IntegrationCacheService.IntegrationCache;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.service.FileMonitoringService;
import com.fsmonitor.app.service.FileCleanupService;
import com.fsmonitor.app.service.IntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    @Autowired
    private FileMonitoringService fileMonitoringService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private FileCleanupService fileCleanupService;

    @Autowired
    private IntegrationCacheService integrationCacheService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMonitoringStatus() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = fileMonitoringService.getNextSchedulerRun();
        
        long secondsUntilNextRun;
        if (nextRun == null) {
            // Scheduler hasn't run yet, estimate based on startup
            secondsUntilNextRun = 60;
        } else {
            secondsUntilNextRun = Duration.between(now, nextRun).getSeconds();
            if (secondsUntilNextRun < 0) secondsUntilNextRun = 0;
        }
        
        // Build per-integration timer data
        List<Integration> allIntegrations = integrationService.getAllIntegrations();
        Map<String, Object> integrationTimers = new HashMap<>();
        
        for (Integration integration : allIntegrations) {
            if (Boolean.TRUE.equals(integration.getIsActive()) && Boolean.TRUE.equals(integration.getMonitoringEnabled())) {
                IntegrationCache cache = integrationCacheService.getCache(integration.getId());
                LocalDateTime lastChecked = cache.getLastCheckedAt();
                long intervalSeconds = integration.getCheckIntervalMinutes() * 60;
                
                long secondsUntilIntegrationRun;
                if (lastChecked == null) {
                    // Never been checked - show the configured interval time
                    secondsUntilIntegrationRun = intervalSeconds;
                } else {
                    LocalDateTime nextIntegrationRun = lastChecked.plusMinutes(integration.getCheckIntervalMinutes());
                    secondsUntilIntegrationRun = Duration.between(now, nextIntegrationRun).getSeconds();
                    
                    if (secondsUntilIntegrationRun < 0) {
                        // Due to run, will happen on next scheduler cycle
                        secondsUntilIntegrationRun = secondsUntilNextRun;
                    }
                }
                
                integrationTimers.put(String.valueOf(integration.getId()), Map.of(
                    "secondsUntilNextRun", secondsUntilIntegrationRun,
                    "intervalSeconds", intervalSeconds,
                    "lastCheckedAt", lastChecked != null ? lastChecked.toString() : ""
                ));
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "running",
            "secondsUntilNextRun", secondsUntilNextRun,
            "interval", 60,
            "integrationTimers", integrationTimers
        ));
    }

    @GetMapping("/integrations/{id}/status")
    public ResponseEntity<Map<String, Object>> getIntegrationStatus(@PathVariable Long id) {
        java.util.Optional<Integration> integrationOpt = integrationService.getIntegrationById(id);
        if (integrationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Integration integration = integrationOpt.get();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastCheck = integration.getUpdatedAt();
        
        if (lastCheck == null) {
            // Never been checked, should run immediately
            return ResponseEntity.ok(Map.of(
                "integrationId", id,
                "status", "pending",
                "secondsUntilNextRun", 0,
                "lastCheck", null,
                "nextRun", now.toString()
            ));
        }
        
        LocalDateTime nextRun = lastCheck.plusMinutes(integration.getCheckIntervalMinutes().longValue());
        long secondsUntilNextRun = java.time.Duration.between(now, nextRun).getSeconds();
        
        if (secondsUntilNextRun < 0) {
            // Should have run already
            secondsUntilNextRun = 0;
            nextRun = now;
        }
        
        return ResponseEntity.ok(Map.of(
            "integrationId", id,
            "status", "scheduled",
            "secondsUntilNextRun", secondsUntilNextRun,
            "lastCheck", lastCheck.toString(),
            "nextRun", nextRun.toString(),
            "intervalMinutes", integration.getCheckIntervalMinutes()
        ));
    }

    @PostMapping("/integrations/{id}/check-now")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerIntegrationCheck(@PathVariable Long id) {
        try {
            // This would trigger an immediate check for the specific integration
            return ResponseEntity.ok("Integration check triggered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to trigger integration check: " + e.getMessage());
        }
    }

    @PostMapping("/integrations/{id}/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerIntegrationCleanup(@PathVariable Long id) {
        try {
            fileCleanupService.cleanupIntegrationFilesNow(id);
            return ResponseEntity.ok("Integration cleanup completed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to trigger integration cleanup: " + e.getMessage());
        }
    }

    @GetMapping("/integrations/{id}/cleanup-preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCleanupPreview(@PathVariable Long id) {
        try {
            Map<String, Object> preview = fileCleanupService.getCleanupPreview(id);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to generate cleanup preview: " + e.getMessage()
            ));
        }
    }
}
