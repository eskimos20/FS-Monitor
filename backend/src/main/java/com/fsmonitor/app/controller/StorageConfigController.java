package com.fsmonitor.app.controller;

import com.fsmonitor.app.entity.StorageConfig;
import com.fsmonitor.app.repository.StorageConfigRepository;
import com.fsmonitor.app.service.StorageMonitoringService;
import com.fsmonitor.app.service.StorageCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storage-configs")
public class StorageConfigController {
    
    @Autowired
    private StorageConfigRepository storageConfigRepository;
    
    @Autowired
    private StorageCacheService storageCacheService;
    
    @Autowired
    private StorageMonitoringService storageMonitoringService;
    
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
                // Clear cache data for this config
                storageCacheService.clearStorageData(id);
                // Delete the config
                storageConfigRepository.delete(config);
                return ResponseEntity.ok().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/info")
    public List<?> getStorageInfo(@PathVariable Long id) {
        return storageCacheService.getStorageInfo(id);
    }
    
    @GetMapping("/{id}/largest-files")
    public List<?> getLargestFiles(@PathVariable Long id) {
        return storageCacheService.getLargestFiles(id);
    }
    
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        List<StorageConfig> configs = storageConfigRepository.findByActiveTrue();
        Map<String, Object> result = new HashMap<>();
        
        // Get all cached data at once for efficiency
        Map<String, Object> allCachedData = storageCacheService.getAllStorageData();
        
        for (StorageConfig config : configs) {
            Map<String, Object> configData = new HashMap<>();
            configData.put("config", config);
            
            String configId = config.getId().toString();
            if (allCachedData.containsKey(configId)) {
                Map<String, Object> cachedData = (Map<String, Object>) allCachedData.get(configId);
                configData.put("info", cachedData.get("info"));
                configData.put("largestFiles", cachedData.get("largestFiles"));
                configData.put("diskSpace", cachedData.get("diskSpace"));
            } else {
                // No cached data yet
                configData.put("info", Collections.emptyList());
                configData.put("largestFiles", Collections.emptyList());
                configData.put("diskSpace", null);
            }
            
            result.put(configId, configData);
        }
        
        return result;
    }
}
