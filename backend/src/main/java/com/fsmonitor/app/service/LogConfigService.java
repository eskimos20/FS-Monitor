package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.LogConfig;
import com.fsmonitor.app.repository.LogConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for log monitoring configurations. Controllers stay thin
 * and never touch repositories directly.
 */
@Service
public class LogConfigService {

    private final LogConfigRepository logConfigRepository;
    private final LogMonitoringService logMonitoringService;

    public LogConfigService(LogConfigRepository logConfigRepository,
                            LogMonitoringService logMonitoringService) {
        this.logConfigRepository = logConfigRepository;
        this.logMonitoringService = logMonitoringService;
    }

    public List<LogConfig> findAll() {
        return logConfigRepository.findAll();
    }

    public Optional<LogConfig> findById(Long id) {
        return logConfigRepository.findById(id);
    }

    @Transactional
    public LogConfig create(LogConfig config) {
        return logConfigRepository.save(config);
    }

    @Transactional
    public Optional<LogConfig> update(Long id, LogConfig incoming) {
        return logConfigRepository.findById(id).map(existing -> {
            boolean keywordsChanged = !java.util.Objects.equals(
                    existing.getKeywords(), incoming.getKeywords());

            existing.setName(incoming.getName());
            existing.setPath(incoming.getPath());
            existing.setFileTypes(incoming.getFileTypes());
            existing.setKeywords(incoming.getKeywords());
            existing.setCheckIntervalMinutes(incoming.getCheckIntervalMinutes());
            existing.setRecursive(incoming.isRecursive());
            existing.setActive(incoming.isActive());

            if (keywordsChanged) {
                logMonitoringService.clearMatchesForConfig(id);
            }
            return logConfigRepository.save(existing);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        return logConfigRepository.findById(id).map(config -> {
            logMonitoringService.clearMatchesForConfig(id);
            logConfigRepository.delete(config);
            return true;
        }).orElse(false);
    }

    @Transactional
    public Optional<LogConfig> toggle(Long id) {
        return logConfigRepository.findById(id).map(config -> {
            config.setActive(!config.isActive());
            return logConfigRepository.save(config);
        });
    }
}
