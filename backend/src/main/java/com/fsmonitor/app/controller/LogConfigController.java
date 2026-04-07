package com.fsmonitor.app.controller;

import com.fsmonitor.app.cache.MonitoringCacheManager;
import com.fsmonitor.app.dto.LogMatch;
import com.fsmonitor.app.entity.LogConfig;
import com.fsmonitor.app.repository.LogConfigRepository;
import com.fsmonitor.app.service.LogMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/log-configs")
@CrossOrigin(origins = "*")
public class LogConfigController {

    @Autowired
    private LogConfigRepository logConfigRepository;

    @Autowired
    private LogMonitoringService logMonitoringService;

    @GetMapping
    public List<LogConfig> getAllLogConfigs() {
        return logConfigRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogConfig> getLogConfig(@PathVariable Long id) {
        return logConfigRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public LogConfig createLogConfig(@RequestBody LogConfig logConfig) {
        return logConfigRepository.save(logConfig);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LogConfig> updateLogConfig(@PathVariable Long id, @RequestBody LogConfig logConfig) {
        return logConfigRepository.findById(id)
                .map(existing -> {
                    // Check if keywords changed - if so, clear old matches
                    boolean keywordsChanged = !existing.getKeywords().equals(logConfig.getKeywords());
                    
                    existing.setName(logConfig.getName());
                    existing.setPath(logConfig.getPath());
                    existing.setFileTypes(logConfig.getFileTypes());
                    existing.setKeywords(logConfig.getKeywords());
                    existing.setCheckIntervalMinutes(logConfig.getCheckIntervalMinutes());
                    existing.setRecursive(logConfig.isRecursive());
                    existing.setActive(logConfig.isActive());
                    
                    // Clear matches if keywords changed
                    if (keywordsChanged) {
                        logMonitoringService.clearMatchesForConfig(id);
                    }
                    
                    return ResponseEntity.ok(logConfigRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLogConfig(@PathVariable Long id) {
        return logConfigRepository.findById(id)
                .map(config -> {
                    logMonitoringService.clearMatchesForConfig(id);
                    logConfigRepository.delete(config);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<LogConfig> toggleLogConfig(@PathVariable Long id) {
        return logConfigRepository.findById(id)
                .map(config -> {
                    config.setActive(!config.isActive());
                    return ResponseEntity.ok(logConfigRepository.save(config));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/search")
    public List<LogMatch> searchLogs(@PathVariable Long id) {
        return logMonitoringService.searchLogs(id);
    }

    @GetMapping("/stats")
    public Map<String, Object> getLogStats() {
        return logMonitoringService.getLogStats();
    }

    @GetMapping("/matches/recent")
    public List<MonitoringCacheManager.LogMatchResultData> getRecentMatches(@RequestParam(defaultValue = "24") int hours) {
        return logMonitoringService.getRecentMatches(hours);
    }

    @GetMapping("/{id}/matches")
    public List<MonitoringCacheManager.LogMatchResultData> getMatchesForConfig(@PathVariable Long id) {
        return logMonitoringService.getMatchesForConfig(id);
    }
}
