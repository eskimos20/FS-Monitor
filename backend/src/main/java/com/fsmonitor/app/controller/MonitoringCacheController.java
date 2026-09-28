package com.fsmonitor.app.controller;

import com.fsmonitor.app.cache.IntegrationCacheService;
import com.fsmonitor.app.cache.IntegrationCacheService.IntegrationCache;
import com.fsmonitor.app.cache.ServiceCacheService;
import com.fsmonitor.app.cache.ServiceCacheService.ServiceCache;
import com.fsmonitor.app.dto.IntegrationStatusDTO;
import com.fsmonitor.app.dto.ServiceStatusDTO;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.repository.IntegrationRepository;
import com.fsmonitor.app.repository.ServiceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for cache status endpoints.
 * Note: Clear endpoints are removed per "Alternative 3" - no explicit cache clearing.
 * Only status endpoints are provided as they are used by the frontend.
 */
@RestController
@RequestMapping("/api/monitoring-cache")
public class MonitoringCacheController {

    private final IntegrationCacheService integrationCacheService;
    private final ServiceCacheService serviceCacheService;
    private final IntegrationRepository integrationRepository;
    private final ServiceRepository serviceRepository;

    public MonitoringCacheController(IntegrationCacheService integrationCacheService,
                                     ServiceCacheService serviceCacheService,
                                     IntegrationRepository integrationRepository,
                                     ServiceRepository serviceRepository) {
        this.integrationCacheService = integrationCacheService;
        this.serviceCacheService = serviceCacheService;
        this.integrationRepository = integrationRepository;
        this.serviceRepository = serviceRepository;
    }
    
    /**
     * Get cache statistics for monitoring purposes
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Integration cache stats
        Set<Long> integrationIds = integrationCacheService.getAllCachedIds();
        stats.put("integrationCaches", integrationIds.size());
        
        // Service cache stats  
        Set<Long> serviceIds = serviceCacheService.getAllCachedIds();
        stats.put("serviceCaches", serviceIds.size());
        
        stats.put("totalCaches", integrationIds.size() + serviceIds.size());
        stats.put("lastCleanup", java.time.LocalDateTime.now()); // Simplified for now
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get status for all integrations from cache
     */
    @GetMapping("/integrations/status")
    public ResponseEntity<List<IntegrationStatusDTO>> getAllIntegrationStatuses() {
        List<Integration> integrations = integrationRepository.findAll();
        List<IntegrationStatusDTO> statuses = new ArrayList<>();
        
        for (Integration integration : integrations) {
            IntegrationCache cache = integrationCacheService.getCache(integration.getId());
            
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
    
    /**
     * Get status for specific integration from cache
     */
    @GetMapping("/integrations/{id}/status")
    public ResponseEntity<IntegrationStatusDTO> getIntegrationStatus(@PathVariable Long id) {
        return integrationRepository.findById(id)
            .map(integration -> {
                IntegrationCache cache = integrationCacheService.getCache(id);
                
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
    
    /**
     * Get status for all services from cache
     */
    @GetMapping("/services/status")
    public ResponseEntity<List<ServiceStatusDTO>> getAllServiceStatuses() {
        List<Service> services = serviceRepository.findAll();
        List<ServiceStatusDTO> statuses = new ArrayList<>();
        
        for (Service service : services) {
            ServiceCache cache = serviceCacheService.getCache(service.getId());
            
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
    
    /**
     * Get status for specific service from cache
     */
    @GetMapping("/services/{id}/status")
    public ResponseEntity<ServiceStatusDTO> getServiceStatus(@PathVariable Long id) {
        return serviceRepository.findById(id)
            .map(service -> {
                ServiceCache cache = serviceCacheService.getCache(id);
                
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
}
