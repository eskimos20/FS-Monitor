package com.fsmonitor.app.controller;

import com.fsmonitor.app.cache.IntegrationCacheService;
import com.fsmonitor.app.cache.IntegrationCacheService.IntegrationCache;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.service.FileMonitoringService;
import com.fsmonitor.app.service.IntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final FileMonitoringService fileMonitoringService;
    private final IntegrationService integrationService;
    private final IntegrationCacheService integrationCacheService;

    public MonitoringController(FileMonitoringService fileMonitoringService,
                                IntegrationService integrationService,
                                IntegrationCacheService integrationCacheService) {
        this.fileMonitoringService = fileMonitoringService;
        this.integrationService = integrationService;
        this.integrationCacheService = integrationCacheService;
    }

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
                Long intervalMin = integration.getCheckIntervalMinutes();
                long intervalSeconds = (intervalMin != null ? intervalMin : 5L) * 60;
                
                long secondsUntilIntegrationRun;
                if (lastChecked == null) {
                    // Never been checked - show the configured interval time
                    secondsUntilIntegrationRun = intervalSeconds;
                } else {
                    LocalDateTime nextIntegrationRun = lastChecked.plusMinutes(intervalMin != null ? intervalMin : 5L);
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
        LocalDateTime lastCheck = integration.getLastCheck();
        
        if (lastCheck == null) {
            // Never been checked, should run immediately
            return ResponseEntity.ok(Map.of(
                "integrationId", id,
                "status", "pending",
                "secondsUntilNextRun", 0,
                "lastCheck", "",
                "nextRun", now.toString()
            ));
        }
        
        Long intervalMin = integration.getCheckIntervalMinutes();
        LocalDateTime nextRun = lastCheck.plusMinutes(intervalMin != null ? intervalMin : 5L);
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
            "intervalMinutes", intervalMin != null ? intervalMin : 5L
        ));
    }

    @PostMapping("/integrations/{id}/check-now")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerIntegrationCheck(@PathVariable Long id) {
        return integrationService.getIntegrationById(id)
                .map(integration -> {
                    fileMonitoringService.checkIntegrationNow(integration);
                    return ResponseEntity.ok("Integration check triggered successfully");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
