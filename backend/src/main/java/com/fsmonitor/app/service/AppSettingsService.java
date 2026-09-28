package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.AppSettings;
import com.fsmonitor.app.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AppSettingsService {
    
    private final AppSettingsRepository appSettingsRepository;

    public AppSettingsService(AppSettingsRepository appSettingsRepository) {
        this.appSettingsRepository = appSettingsRepository;
    }
    
    @Transactional
    public AppSettings saveAppSettings(AppSettings appSettings) {
        Optional<AppSettings> existing = appSettingsRepository.findFirstByOrderByIdDesc();
        if (existing.isPresent()) {
            AppSettings current = existing.get();
            current.setRefreshIntervalSeconds(appSettings.getRefreshIntervalSeconds());
            return appSettingsRepository.save(current);
        }
        return appSettingsRepository.save(appSettings);
    }
    
    @Transactional(readOnly = true)
    public AppSettings getCurrentAppSettings() {
        return appSettingsRepository.findFirstByOrderByIdDesc()
                .orElseGet(() -> {
                    AppSettings defaultSettings = new AppSettings();
                    defaultSettings.setRefreshIntervalSeconds(5);
                    return defaultSettings;
                });
    }
}
