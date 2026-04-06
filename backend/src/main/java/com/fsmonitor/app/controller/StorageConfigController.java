package com.fsmonitor.app.controller;

import com.fsmonitor.app.entity.DiskSpace;
import com.fsmonitor.app.entity.LargestFile;
import com.fsmonitor.app.entity.StorageConfig;
import com.fsmonitor.app.entity.StorageInfo;
import com.fsmonitor.app.repository.DiskSpaceRepository;
import com.fsmonitor.app.repository.LargestFileRepository;
import com.fsmonitor.app.repository.StorageConfigRepository;
import com.fsmonitor.app.repository.StorageInfoRepository;
import com.fsmonitor.app.service.StorageMonitoringService;
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
    private StorageInfoRepository storageInfoRepository;
    
    @Autowired
    private LargestFileRepository largestFileRepository;
    
    @Autowired
    private DiskSpaceRepository diskSpaceRepository;
    
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
                // Delete all related data first
                storageInfoRepository.deleteByStorageConfigId(id);
                largestFileRepository.deleteByStorageConfigId(id);
                diskSpaceRepository.deleteByStorageConfigId(id);
                // Now delete the config
                storageConfigRepository.delete(config);
                return ResponseEntity.ok().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/info")
    public List<StorageInfo> getStorageInfo(@PathVariable Long id) {
        return storageInfoRepository.findByStorageConfigId(id);
    }
    
    @GetMapping("/{id}/largest-files")
    public List<LargestFile> getLargestFiles(@PathVariable Long id) {
        return largestFileRepository.findByStorageConfigIdOrderBySizeBytesDesc(id);
    }
    
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        List<StorageConfig> configs = storageConfigRepository.findByActiveTrue();
        Map<String, Object> result = new HashMap<>();
        
        for (StorageConfig config : configs) {
            Map<String, Object> configData = new HashMap<>();
            configData.put("config", config);
            configData.put("info", storageInfoRepository.findByStorageConfigId(config.getId()));
            configData.put("largestFiles", largestFileRepository.findByStorageConfigIdOrderBySizeBytesDesc(config.getId()));
            configData.put("diskSpace", diskSpaceRepository.findByStorageConfigId(config.getId()).orElse(null));
            result.put(config.getId().toString(), configData);
        }
        
        return result;
    }
}
