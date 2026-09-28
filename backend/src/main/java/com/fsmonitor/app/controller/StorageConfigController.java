package com.fsmonitor.app.controller;

import com.fsmonitor.app.dto.StorageConfigResponse;
import com.fsmonitor.app.entity.StorageConfig;
import com.fsmonitor.app.service.StorageMonitoringService;
import com.fsmonitor.app.cache.StorageCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storage-configs")
public class StorageConfigController {

    private final StorageMonitoringService storageMonitoringService;
    private final StorageCacheService storageCacheService;

    public StorageConfigController(StorageMonitoringService storageMonitoringService,
                                   StorageCacheService storageCacheService) {
        this.storageMonitoringService = storageMonitoringService;
        this.storageCacheService = storageCacheService;
    }

    @GetMapping
    public List<StorageConfigResponse> getAllStorageConfigs() {
        return storageMonitoringService.findAll().stream()
            .map(StorageConfigResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StorageConfigResponse> getStorageConfig(@PathVariable Long id) {
        return storageMonitoringService.findById(id)
            .map(config -> ResponseEntity.ok(StorageConfigResponse.from(config)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StorageConfigResponse createStorageConfig(@RequestBody StorageConfig storageConfig) {
        return StorageConfigResponse.from(storageMonitoringService.create(storageConfig));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StorageConfigResponse> updateStorageConfig(@PathVariable Long id, @RequestBody StorageConfig storageConfig) {
        return storageMonitoringService.update(id, storageConfig)
            .map(config -> ResponseEntity.ok(StorageConfigResponse.from(config)))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStorageConfig(@PathVariable Long id) {
        return storageMonitoringService.delete(id)
            ? ResponseEntity.ok().<Void>build()
            : ResponseEntity.notFound().build();
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        List<StorageConfig> configs = storageMonitoringService.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .toList();
        Map<String, Object> result = new HashMap<>();

        // Add system-wide storage data first (ID: -1)
        Map<String, Object> systemStorageData = new HashMap<>();
        StorageCacheService.StorageCache systemCache = storageCacheService.getCache(-1L);
        if (systemCache != null && !systemCache.getStorageInfoList().isEmpty()) {
            systemStorageData.put("config", StorageConfigResponse.from(createSystemConfig()));
            systemStorageData.put("info", systemCache.getStorageInfoList());
            systemStorageData.put("largestFiles", systemCache.getLargestFiles());
            systemStorageData.put("diskSpace", systemCache.getDiskSpace());
            result.put("-1", systemStorageData);
        }

        // Get actual data from cache for each config
        for (StorageConfig config : configs) {
            Map<String, Object> configData = new HashMap<>();
            configData.put("config", StorageConfigResponse.from(config));

            // Get data from storage cache service
            StorageCacheService.StorageCache cache = storageCacheService.getCache(config.getId());
            configData.put("info", cache.getStorageInfoList());
            configData.put("largestFiles", cache.getLargestFiles());
            configData.put("diskSpace", cache.getDiskSpace());

            result.put(config.getId().toString(), configData);
        }

        return result;
    }

    private StorageConfig createSystemConfig() {
        StorageConfig systemConfig = new StorageConfig();
        systemConfig.setId(-1L);
        systemConfig.setName("System Storage");
        systemConfig.setPath("/");
        systemConfig.setActive(true);
        return systemConfig;
    }
}
