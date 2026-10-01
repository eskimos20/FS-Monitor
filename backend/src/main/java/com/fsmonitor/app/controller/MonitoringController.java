package com.fsmonitor.app.controller;

import com.fsmonitor.app.cache.IntegrationCacheService;
import com.fsmonitor.app.cache.IntegrationCacheService.IntegrationCache;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.service.FileMonitoringService;
import com.fsmonitor.app.service.IntegrationService;
import com.fsmonitor.app.util.ScheduleUtil;
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

                LocalDateTime eligiblePass = nextEligiblePass(integration, lastChecked, nextRun, now);
                long secondsUntilIntegrationRun = eligiblePass != null
                        ? Duration.between(now, eligiblePass).getSeconds()
                        : secondsUntilNextRun;
                boolean outsideSchedule = Boolean.TRUE.equals(integration.getScheduleEnabled())
                        && !ScheduleUtil.isWithinSchedule(integration.getActiveDays(),
                                integration.getActiveStartHour(),
                                integration.getActiveEndHour(), now);

                integrationTimers.put(String.valueOf(integration.getId()), Map.of(
                    "secondsUntilNextRun", secondsUntilIntegrationRun,
                    "intervalSeconds", intervalSeconds,
                    "lastCheckedAt", lastChecked != null ? lastChecked.toString() : "",
                    "outsideSchedule", outsideSchedule
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
        Optional<Integration> integrationOpt = integrationService.getIntegrationById(id);
        if (integrationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Integration integration = integrationOpt.get();
        LocalDateTime now = LocalDateTime.now();
        // Same source the scheduler uses - DB lastCheck is stale after a restart
        LocalDateTime lastCheck = integrationCacheService.getCache(id).getLastCheckedAt();
        LocalDateTime nextPass = fileMonitoringService.getNextSchedulerRun();
        Long intervalMin = integration.getCheckIntervalMinutes();

        LocalDateTime eligiblePass = nextEligiblePass(integration, lastCheck, nextPass, now);
        if (eligiblePass == null) {
            eligiblePass = nextPass != null ? nextPass : now;
        }

        return ResponseEntity.ok(Map.of(
            "integrationId", id,
            "status", lastCheck == null ? "pending" : "scheduled",
            "secondsUntilNextRun", Math.max(0, Duration.between(now, eligiblePass).getSeconds()),
            "lastCheck", lastCheck != null ? lastCheck.toString() : "",
            "nextRun", eligiblePass.toString(),
            "intervalMinutes", intervalMin != null ? intervalMin : 5L
        ));
    }

    /**
     * Find the first scheduler pass at which a check will actually execute:
     * the first pass at-or-after {@code lastCheck + interval}. Checks run
     * regardless of schedule - the schedule only gates outbound mail, so the
     * countdown always advances on the normal pass grid.
     */
    private LocalDateTime nextEligiblePass(Integration integration, LocalDateTime lastChecked,
                                           LocalDateTime firstPass, LocalDateTime now) {
        long intervalMin = integration.getCheckIntervalMinutes() != null
                ? integration.getCheckIntervalMinutes() : 5L;
        LocalDateTime due = lastChecked != null ? lastChecked.plusMinutes(intervalMin) : now;

        LocalDateTime pass = firstPass != null ? firstPass : now.plusSeconds(60);
        while (pass.isBefore(due)) {
            pass = pass.plusMinutes(1);
        }
        return pass;
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
