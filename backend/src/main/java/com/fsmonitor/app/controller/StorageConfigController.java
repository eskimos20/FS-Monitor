package com.fsmonitor.app.controller;

import com.fsmonitor.app.entity.StorageConfig;
import com.fsmonitor.app.repository.StorageConfigRepository;
import com.fsmonitor.app.service.StorageMonitoringService;
import com.fsmonitor.app.cache.StorageCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storage-configs")
public class StorageConfigController {
    
    @Autowired
    private StorageConfigRepository storageConfigRepository;
        
    @Autowired
    private StorageMonitoringService storageMonitoringService;
    
    @Autowired
    private StorageCacheService storageCacheService;
    
    @GetMapping
    public List<StorageConfig> getAllStorageConfigs() {
        return storageConfigRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<StorageConfig> getStorageConfig(@PathVariable Long id) {
        return storageConfigRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StorageConfig createStorageConfig(@RequestBody StorageConfig storageConfig) {
        StorageConfig saved = storageConfigRepository.save(storageConfig);
        // Trigger immediate scan
        storageMonitoringService.scanStorage(saved);
        return saved;
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StorageConfig> updateStorageConfig(@PathVariable Long id, @RequestBody StorageConfig storageConfig) {
        return storageConfigRepository.findById(id)
            .map(existing -> {
                existing.setName(storageConfig.getName());
                existing.setPath(storageConfig.getPath());
                existing.setRecursive(storageConfig.getRecursive());
                existing.setCheckIntervalMinutes(storageConfig.getCheckIntervalMinutes());
                existing.setIntervalUnit(storageConfig.getIntervalUnit());
                existing.setActive(storageConfig.getActive());
                StorageConfig updated = storageConfigRepository.save(existing);
                // Trigger immediate scan
                storageMonitoringService.scanStorage(updated);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStorageConfig(@PathVariable Long id) {
        return storageConfigRepository.findById(id)
            .map(config -> {
                // Delete the config
                storageConfigRepository.delete(config);
                return ResponseEntity.ok().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        List<StorageConfig> configs = storageConfigRepository.findByActiveTrue();
        Map<String, Object> result = new HashMap<>();
        
        // Add system-wide storage data first (ID: -1)
        Map<String, Object> systemStorageData = new HashMap<>();
        StorageCacheService.StorageCache systemCache = storageCacheService.getCache(-1L);
        if (systemCache != null && !systemCache.getStorageInfoList().isEmpty()) {
            systemStorageData.put("config", createSystemConfig());
            systemStorageData.put("info", systemCache.getStorageInfoList());
            systemStorageData.put("largestFiles", systemCache.getLargestFiles());
            systemStorageData.put("diskSpace", systemCache.getDiskSpace());
            result.put("-1", systemStorageData);
        }
        
        // Get actual data from cache for each config
        for (StorageConfig config : configs) {
            Map<String, Object> configData = new HashMap<>();
            configData.put("config", config);
            
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
