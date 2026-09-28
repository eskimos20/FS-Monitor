package com.fsmonitor.app.cache;

import com.fsmonitor.app.entity.ServiceStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service-specific cache service extending AbstractCacheService
 */
@Service
public class ServiceCacheService extends AbstractCacheService<ServiceCacheService.ServiceCache> {
    
    /**
     * Service cache data structure
     */
    public static class ServiceCache {
        private ServiceStatus status;
        private String lastError;
        private LocalDateTime lastCheckedAt;
        private LocalDateTime lastSuccessfulCheck;
        private LocalDateTime cacheTime;
        
        public ServiceCache() {
            this.cacheTime = LocalDateTime.now();
        }
        
        public ServiceStatus getStatus() { return status; }
        public void setStatus(ServiceStatus status) { 
            this.status = status;
            this.cacheTime = LocalDateTime.now();
        }
        
        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { 
            this.lastError = lastError;
            this.cacheTime = LocalDateTime.now();
        }
        
        public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
        public void setLastCheckedAt(LocalDateTime lastCheckedAt) { 
            this.lastCheckedAt = lastCheckedAt;
            this.cacheTime = LocalDateTime.now();
        }
        
        public LocalDateTime getLastSuccessfulCheck() { return lastSuccessfulCheck; }
        public void setLastSuccessfulCheck(LocalDateTime lastSuccessfulCheck) { 
            this.lastSuccessfulCheck = lastSuccessfulCheck;
            this.cacheTime = LocalDateTime.now();
        }
        
        public LocalDateTime getCacheTime() { return cacheTime; }
        
        public void updateCacheTime() {
            this.cacheTime = LocalDateTime.now();
        }
        
        public boolean isOnline() {
            return ServiceStatus.ONLINE.equals(status);
        }
        
        public boolean hasError() {
            return lastError != null && !lastError.trim().isEmpty();
        }
        
        public long getMinutesSinceLastCheck() {
            return lastCheckedAt != null ? 
                java.time.Duration.between(lastCheckedAt, LocalDateTime.now()).toMinutes() : -1;
        }
        
        public long getMinutesSinceLastSuccess() {
            return lastSuccessfulCheck != null ? 
                java.time.Duration.between(lastSuccessfulCheck, LocalDateTime.now()).toMinutes() : -1;
        }
        
        public long getMinutesSinceLastFailure() {
            if (!hasError() || lastCheckedAt == null) return -1;
            return java.time.Duration.between(lastCheckedAt, LocalDateTime.now()).toMinutes();
        }
    }
    
    @Override
    protected ServiceCache createNewCacheEntry() {
        return new ServiceCache();
    }
    
    @Override
    protected void cleanupOldData() {
        // Service cache doesn't accumulate data, so no specific cleanup needed
        // Data is replaced, not appended
        logger.debug("Service cache cleanup completed - no action needed");
    }
    
    @Override
    protected Map<String, Object> getCacheEntryDetails(ServiceCache cacheEntry) {
        Map<String, Object> details = new HashMap<>();
        details.put("status", cacheEntry.getStatus());
        details.put("isOnline", cacheEntry.isOnline());
        details.put("hasError", cacheEntry.hasError());
        details.put("lastError", cacheEntry.getLastError());
        details.put("lastCheckedAt", cacheEntry.getLastCheckedAt());
        details.put("lastSuccessfulCheck", cacheEntry.getLastSuccessfulCheck());
        details.put("cacheTime", cacheEntry.getCacheTime());
        details.put("minutesSinceLastCheck", cacheEntry.getMinutesSinceLastCheck());
        details.put("minutesSinceLastSuccess", cacheEntry.getMinutesSinceLastSuccess());
        details.put("minutesSinceLastFailure", cacheEntry.getMinutesSinceLastFailure());
        
        return details;
    }
    
    // Service-specific convenience methods
    
    /**
     * Update service status
     */
    public void updateStatus(Long serviceId, ServiceStatus status, String errorMessage) {
        ServiceCache cache = getCache(serviceId);
        LocalDateTime now = LocalDateTime.now();
        
        cache.setStatus(status);
        cache.setLastCheckedAt(now);
        
        if (ServiceStatus.ONLINE.equals(status)) {
            cache.setLastSuccessfulCheck(now);
            cache.setLastError(null); // Clear error on success
        } else if (errorMessage != null) {
            cache.setLastError(errorMessage);
        }
        
        logger.debug("Updated status for service {}: {}, error={}", 
            serviceId, status, errorMessage);
    }
    
    /**
     * Update service status (without error message)
     */
    public void updateStatus(Long serviceId, ServiceStatus status) {
        updateStatus(serviceId, status, null);
    }
    
    /**
     * Get service status
     */
    public ServiceStatus getStatus(Long serviceId) {
        return getCache(serviceId).getStatus();
    }
    
    /**
     * Get last error message
     */
    public String getLastError(Long serviceId) {
        return getCache(serviceId).getLastError();
    }
    
    /**
     * Get last checked time
     */
    public LocalDateTime getLastCheckedAt(Long serviceId) {
        return getCache(serviceId).getLastCheckedAt();
    }
    
    /**
     * Get last successful check time
     */
    public LocalDateTime getLastSuccessfulCheck(Long serviceId) {
        return getCache(serviceId).getLastSuccessfulCheck();
    }
    
    /**
     * Check if service is online
     */
    public boolean isOnline(Long serviceId) {
        return getCache(serviceId).isOnline();
    }
    
    /**
     * Check if service has error
     */
    public boolean hasError(Long serviceId) {
        return getCache(serviceId).hasError();
    }
    
    /**
     * Get services that are currently online
     */
    public Set<Long> getOnlineServices() {
        Set<Long> onlineServices = new HashSet<>();
        
        for (Map.Entry<Long, ServiceCache> entry : cache.entrySet()) {
            if (entry.getValue().isOnline()) {
                onlineServices.add(entry.getKey());
            }
        }
        
        return onlineServices;
    }
    
    /**
     * Get services that are currently offline or have errors
     */
    public Set<Long> getOfflineServices() {
        Set<Long> offlineServices = new HashSet<>();
        
        for (Map.Entry<Long, ServiceCache> entry : cache.entrySet()) {
            if (!entry.getValue().isOnline()) {
                offlineServices.add(entry.getKey());
            }
        }
        
        return offlineServices;
    }
    
    /**
     * Get services with errors
     */
    public Set<Long> getServicesWithErrors() {
        Set<Long> errorServices = new HashSet<>();
        
        for (Map.Entry<Long, ServiceCache> entry : cache.entrySet()) {
            if (entry.getValue().hasError()) {
                errorServices.add(entry.getKey());
            }
        }
        
        return errorServices;
    }
    
    /**
     * Get services that haven't been checked recently
     */
    public Set<Long> getStaleServices(int maxMinutesSinceCheck) {
        Set<Long> staleServices = new HashSet<>();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(maxMinutesSinceCheck);
        
        for (Map.Entry<Long, ServiceCache> entry : cache.entrySet()) {
            Long serviceId = entry.getKey();
            ServiceCache cacheEntry = entry.getValue();
            
            if (cacheEntry.getLastCheckedAt() == null || 
                cacheEntry.getLastCheckedAt().isBefore(cutoff)) {
                staleServices.add(serviceId);
            }
        }
        
        return staleServices;
    }
    
    /**
     * Get services that have been down for a long time
     */
    public Set<Long> getLongDownServices(int maxMinutesSinceSuccess) {
        Set<Long> longDownServices = new HashSet<>();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(maxMinutesSinceSuccess);
        
        for (Map.Entry<Long, ServiceCache> entry : cache.entrySet()) {
            Long serviceId = entry.getKey();
            ServiceCache cacheEntry = entry.getValue();
            
            if (!cacheEntry.isOnline() && 
                (cacheEntry.getLastSuccessfulCheck() == null || 
                 cacheEntry.getLastSuccessfulCheck().isBefore(cutoff))) {
                longDownServices.add(serviceId);
            }
        }
        
        return longDownServices;
    }
    
    /**
     * Get service summary statistics
     */
    public Map<String, Object> getServiceSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        int onlineCount = 0;
        int offlineCount = 0;
        int errorCount = 0;
        int servicesCheckedRecently = 0;
        long totalMinutesSinceLastCheck = 0;
        long totalMinutesSinceLastSuccess = 0;
        
        LocalDateTime recentCutoff = LocalDateTime.now().minusMinutes(60); // Last hour
        
        for (ServiceCache cacheEntry : cache.values()) {
            if (cacheEntry.isOnline()) {
                onlineCount++;
            } else {
                offlineCount++;
            }
            
            if (cacheEntry.hasError()) {
                errorCount++;
            }
            
            if (cacheEntry.getLastCheckedAt() != null && 
                !cacheEntry.getLastCheckedAt().isBefore(recentCutoff)) {
                servicesCheckedRecently++;
            }
            
            long minutesSinceCheck = cacheEntry.getMinutesSinceLastCheck();
            if (minutesSinceCheck >= 0) {
                totalMinutesSinceLastCheck += minutesSinceCheck;
            }
            
            long minutesSinceSuccess = cacheEntry.getMinutesSinceLastSuccess();
            if (minutesSinceSuccess >= 0) {
                totalMinutesSinceLastSuccess += minutesSinceSuccess;
            }
        }
        
        summary.put("totalServices", cache.size());
        summary.put("onlineServices", onlineCount);
        summary.put("offlineServices", offlineCount);
        summary.put("servicesWithErrors", errorCount);
        summary.put("servicesCheckedRecently", servicesCheckedRecently);
        summary.put("averageMinutesSinceLastCheck", 
            cache.size() > 0 ? (double) totalMinutesSinceLastCheck / cache.size() : 0);
        summary.put("averageMinutesSinceLastSuccess", 
            onlineCount > 0 ? (double) totalMinutesSinceLastSuccess / cache.size() : 0);
        summary.put("onlinePercentage", 
            cache.size() > 0 ? (double) onlineCount / cache.size() * 100 : 0);
        
        return summary;
    }
    
    /**
     * Get old cache entries (older than specified time)
     */
    @Override
    protected Set<Long> getOldCacheEntries(LocalDateTime cutoffTime) {
        Set<Long> oldEntries = new HashSet<>();
        
        for (Map.Entry<Long, ServiceCache> entry : cache.entrySet()) {
            Long serviceId = entry.getKey();
            ServiceCache cacheEntry = entry.getValue();
            
            if (cacheEntry.getCacheTime().isBefore(cutoffTime)) {
                oldEntries.add(serviceId);
            }
        }
        
        return oldEntries;
    }
    
    /**
     * Get all service data for API response
     */
    public Map<String, Object> getAllServiceData() {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<Long, ServiceCache> entry : cache.entrySet()) {
            Long serviceId = entry.getKey();
            ServiceCache cacheEntry = entry.getValue();
            
            Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("status", cacheEntry.getStatus());
            serviceData.put("isOnline", cacheEntry.isOnline());
            serviceData.put("hasError", cacheEntry.hasError());
            serviceData.put("lastError", cacheEntry.getLastError());
            serviceData.put("lastCheckedAt", cacheEntry.getLastCheckedAt());
            serviceData.put("lastSuccessfulCheck", cacheEntry.getLastSuccessfulCheck());
            serviceData.put("minutesSinceLastCheck", cacheEntry.getMinutesSinceLastCheck());
            serviceData.put("minutesSinceLastSuccess", cacheEntry.getMinutesSinceLastSuccess());
            serviceData.put("minutesSinceLastFailure", cacheEntry.getMinutesSinceLastFailure());
            serviceData.put("cacheTime", cacheEntry.getCacheTime());
            
            result.put(serviceId.toString(), serviceData);
        }
        
        return result;
    }
    
    /**
     * Get status distribution
     */
    public Map<ServiceStatus, Integer> getStatusDistribution() {
        Map<ServiceStatus, Integer> distribution = new HashMap<>();
        
        for (ServiceCache cacheEntry : cache.values()) {
            ServiceStatus status = cacheEntry.getStatus();
            if (status != null) {
                distribution.put(status, distribution.getOrDefault(status, 0) + 1);
            }
        }
        
        return distribution;
    }
}
