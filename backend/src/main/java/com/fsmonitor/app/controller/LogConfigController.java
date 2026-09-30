package com.fsmonitor.app.controller;

import com.fsmonitor.app.cache.model.LogMatchResultData;
import com.fsmonitor.app.dto.LogConfigResponse;
import com.fsmonitor.app.dto.LogMatch;
import com.fsmonitor.app.entity.LogConfig;
import com.fsmonitor.app.service.LogConfigService;
import com.fsmonitor.app.service.LogMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/log-configs")
public class LogConfigController {

    private final LogConfigService logConfigService;
    private final LogMonitoringService logMonitoringService;

    public LogConfigController(LogConfigService logConfigService,
                               LogMonitoringService logMonitoringService) {
        this.logConfigService = logConfigService;
        this.logMonitoringService = logMonitoringService;
    }

    @GetMapping
    public List<LogConfigResponse> getAllLogConfigs() {
        return logConfigService.findAll().stream()
                .map(LogConfigResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogConfigResponse> getLogConfig(@PathVariable Long id) {
        return logConfigService.findById(id)
                .map(config -> ResponseEntity.ok(LogConfigResponse.from(config)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public LogConfigResponse createLogConfig(@RequestBody LogConfig logConfig) {
        return LogConfigResponse.from(logConfigService.create(logConfig));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LogConfigResponse> updateLogConfig(@PathVariable Long id, @RequestBody LogConfig logConfig) {
        return logConfigService.update(id, logConfig)
                .map(config -> ResponseEntity.ok(LogConfigResponse.from(config)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLogConfig(@PathVariable Long id) {
        return logConfigService.delete(id)
                ? ResponseEntity.ok().<Void>build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LogConfigResponse> toggleLogConfig(@PathVariable Long id) {
        return logConfigService.toggle(id)
                .map(config -> ResponseEntity.ok(LogConfigResponse.from(config)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Synchronous grep scan that can hold a request thread for up to
    // fsmonitor.command-timeout-seconds - restricted to admins so regular
    // users cannot trigger heavy on-demand filesystem scans
    @GetMapping("/{id}/search")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LogMatch> searchLogs(@PathVariable Long id) {
        return logMonitoringService.searchLogs(id);
    }

    @GetMapping("/stats")
    public Map<String, Object> getLogStats() {
        return logMonitoringService.getLogStats();
    }

    @GetMapping("/matches/recent")
    public List<LogMatchResultData> getRecentMatches(@RequestParam(defaultValue = "24") int hours) {
        return logMonitoringService.getRecentMatches(hours);
    }

    @GetMapping("/{id}/matches")
    public List<LogMatchResultData> getMatchesForConfig(@PathVariable Long id) {
        return logMonitoringService.getMatchesForConfig(id);
    }
}
