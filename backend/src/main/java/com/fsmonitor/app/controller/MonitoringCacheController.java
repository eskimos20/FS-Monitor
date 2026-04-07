package com.fsmonitor.app.controller;

import com.fsmonitor.app.cache.MonitoringCacheManager;
import com.fsmonitor.app.dto.IntegrationStatusDTO;
import com.fsmonitor.app.dto.ServiceStatusDTO;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.repository.IntegrationRepository;
import com.fsmonitor.app.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring-cache")
@CrossOrigin(origins = "*")
public class MonitoringCacheController {
    
    @Autowired
    private MonitoringCacheManager cacheManager;
    
    @Autowired
    private IntegrationRepository integrationRepository;
    
    @Autowired
    private ServiceRepository serviceRepository;
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(cacheManager.getCacheStats());
    }
    
    @GetMapping("/integrations/status")
    public ResponseEntity<List<IntegrationStatusDTO>> getAllIntegrationStatuses() {
        List<Integration> integrations = integrationRepository.findAll();
        List<IntegrationStatusDTO> statuses = new ArrayList<>();
        
        for (Integration integration : integrations) {
            MonitoringCacheManager.IntegrationCache cache = cacheManager.getIntegrationCache(integration.getId());
            
            IntegrationStatusDTO dto = new IntegrationStatusDTO();
            dto.setId(integration.getId());
            dto.setName(integration.getName());
            dto.setLastFileFound(cache.getLastFileFound());
            dto.setLastFileName(cache.getLastFileName());
            dto.setLastCheckedAt(cache.getLastCheckedAt());
            dto.setIsActive(integration.getIsActive());
            
            statuses.add(dto);
        }
        
        return ResponseEntity.ok(statuses);
    }
    
    @GetMapping("/integrations/{id}/status")
    public ResponseEntity<IntegrationStatusDTO> getIntegrationStatus(@PathVariable Long id) {
        return integrationRepository.findById(id)
            .map(integration -> {
                MonitoringCacheManager.IntegrationCache cache = cacheManager.getIntegrationCache(id);
                
                IntegrationStatusDTO dto = new IntegrationStatusDTO();
                dto.setId(integration.getId());
                dto.setName(integration.getName());
                dto.setLastFileFound(cache.getLastFileFound());
                dto.setLastFileName(cache.getLastFileName());
                dto.setLastCheckedAt(cache.getLastCheckedAt());
                dto.setIsActive(integration.getIsActive());
                
                return ResponseEntity.ok(dto);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/services/status")
    public ResponseEntity<List<ServiceStatusDTO>> getAllServiceStatuses() {
        List<Service> services = serviceRepository.findAll();
        List<ServiceStatusDTO> statuses = new ArrayList<>();
        
        for (Service service : services) {
            MonitoringCacheManager.ServiceCache cache = cacheManager.getServiceCache(service.getId());
            
            ServiceStatusDTO dto = new ServiceStatusDTO();
            dto.setId(service.getId());
            dto.setName(service.getName());
            dto.setStatus(cache.getStatus());
            dto.setLastError(cache.getLastError());
            dto.setLastCheckedAt(cache.getLastCheckedAt());
            dto.setLastSuccessfulCheck(cache.getLastSuccessfulCheck());
            dto.setIsActive(service.getIsActive());
            
            statuses.add(dto);
        }
        
        return ResponseEntity.ok(statuses);
    }
    
    @GetMapping("/services/{id}/status")
    public ResponseEntity<ServiceStatusDTO> getServiceStatus(@PathVariable Long id) {
        return serviceRepository.findById(id)
            .map(service -> {
                MonitoringCacheManager.ServiceCache cache = cacheManager.getServiceCache(id);
                
                ServiceStatusDTO dto = new ServiceStatusDTO();
                dto.setId(service.getId());
                dto.setName(service.getName());
                dto.setStatus(cache.getStatus());
                dto.setLastError(cache.getLastError());
                dto.setLastCheckedAt(cache.getLastCheckedAt());
                dto.setLastSuccessfulCheck(cache.getLastSuccessfulCheck());
                dto.setIsActive(service.getIsActive());
                
                return ResponseEntity.ok(dto);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAllCaches() {
        cacheManager.clearAllCaches();
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/integrations/{id}/clear")
    public ResponseEntity<Void> clearIntegrationCache(@PathVariable Long id) {
        cacheManager.clearIntegrationCache(id);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/services/{id}/clear")
    public ResponseEntity<Void> clearServiceCache(@PathVariable Long id) {
        cacheManager.clearServiceCache(id);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/storage/{id}/clear")
    public ResponseEntity<Void> clearStorageCache(@PathVariable Long id) {
        cacheManager.clearStorageCache(id);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/logs/{id}/clear")
    public ResponseEntity<Void> clearLogCache(@PathVariable Long id) {
        cacheManager.clearLogCache(id);
        return ResponseEntity.ok().build();
    }
}
