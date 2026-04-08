package com.fsmonitor.app.cache;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Integration-specific cache service extending AbstractCacheService
 */
@Service
public class IntegrationCacheService extends AbstractCacheService<IntegrationCacheService.IntegrationCache> {
    
    /**
     * Integration cache data structure
     */
    public static class IntegrationCache {
        private LocalDateTime lastFileFound;
        private String lastFileName;
        private LocalDateTime lastCheckedAt;
        private LocalDateTime cacheTime;
        
        public IntegrationCache() {
            this.cacheTime = LocalDateTime.now();
        }
        
        public LocalDateTime getLastFileFound() { return lastFileFound; }
        public void setLastFileFound(LocalDateTime lastFileFound) { 
            this.lastFileFound = lastFileFound;
            this.cacheTime = LocalDateTime.now();
        }
        
        public String getLastFileName() { return lastFileName; }
        public void setLastFileName(String lastFileName) { 
            this.lastFileName = lastFileName;
            this.cacheTime = LocalDateTime.now();
        }
        
        public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
        public void setLastCheckedAt(LocalDateTime lastCheckedAt) { 
            this.lastCheckedAt = lastCheckedAt;
            this.cacheTime = LocalDateTime.now();
        }
        
        public LocalDateTime getCacheTime() { return cacheTime; }
        
        public void updateCacheTime() {
            this.cacheTime = LocalDateTime.now();
        }
        
        public boolean hasRecentFile() {
            return lastFileFound != null && lastFileName != null;
        }
        
        public long getMinutesSinceLastCheck() {
            return lastCheckedAt != null ? 
                java.time.Duration.between(lastCheckedAt, LocalDateTime.now()).toMinutes() : -1;
        }
        
        public long getMinutesSinceLastFile() {
            return lastFileFound != null ? 
                java.time.Duration.between(lastFileFound, LocalDateTime.now()).toMinutes() : -1;
        }
    }
    
    @Override
    protected IntegrationCache createNewCacheEntry() {
        return new IntegrationCache();
    }
    
    @Override
    protected void cleanupOldData() {
        // Integration cache doesn't accumulate data, so no specific cleanup needed
        // Data is replaced, not appended
        logger.debug("Integration cache cleanup completed - no action needed");
    }
    
    @Override
    protected Map<String, Object> getCacheEntryDetails(IntegrationCache cacheEntry) {
        Map<String, Object> details = new HashMap<>();
        details.put("hasLastFile", cacheEntry.hasRecentFile());
        details.put("lastFileName", cacheEntry.getLastFileName());
        details.put("lastFileFound", cacheEntry.getLastFileFound());
        details.put("lastCheckedAt", cacheEntry.getLastCheckedAt());
        details.put("cacheTime", cacheEntry.getCacheTime());
        details.put("minutesSinceLastCheck", cacheEntry.getMinutesSinceLastCheck());
        details.put("minutesSinceLastFile", cacheEntry.getMinutesSinceLastFile());
        
        return details;
    }
    
    // Integration-specific convenience methods
    
    /**
     * Update file information for integration
     */
    public void updateFileInfo(Long integrationId, String fileName, LocalDateTime fileFoundTime) {
        IntegrationCache cache = getCache(integrationId);
        cache.setLastFileName(fileName);
        cache.setLastFileFound(fileFoundTime);
        logger.debug("Updated file info for integration {}: file={}, time={}", 
            integrationId, fileName, fileFoundTime);
    }
    
    /**
     * Update last checked time for integration
     */
    public void updateLastChecked(Long integrationId, LocalDateTime checkedTime) {
        IntegrationCache cache = getCache(integrationId);
        cache.setLastCheckedAt(checkedTime);
        logger.debug("Updated last checked time for integration {}: {}", integrationId, checkedTime);
    }
    
    /**
     * Get last file name for integration
     */
    public String getLastFileName(Long integrationId) {
        return getCache(integrationId).getLastFileName();
    }
    
    /**
     * Get last file found time for integration
     */
    public LocalDateTime getLastFileFound(Long integrationId) {
        return getCache(integrationId).getLastFileFound();
    }
    
    /**
     * Get last checked time for integration
     */
    public LocalDateTime getLastCheckedAt(Long integrationId) {
        return getCache(integrationId).getLastCheckedAt();
    }
    
    /**
     * Check if integration has recent file
     */
    public boolean hasRecentFile(Long integrationId) {
        return getCache(integrationId).hasRecentFile();
    }
    
    /**
     * Get minutes since last file was found
     */
    public long getMinutesSinceLastFile(Long integrationId) {
        return getCache(integrationId).getMinutesSinceLastFile();
    }
    
    /**
     * Get minutes since last check
     */
    public long getMinutesSinceLastCheck(Long integrationId) {
        return getCache(integrationId).getMinutesSinceLastCheck();
    }
    
    /**
     * Get integrations that haven't been checked recently
     */
    public Set<Long> getStaleIntegrations(int maxMinutesSinceCheck) {
        Set<Long> staleIntegrations = new HashSet<>();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(maxMinutesSinceCheck);
        
        for (Map.Entry<Long, IntegrationCache> entry : cache.entrySet()) {
            Long integrationId = entry.getKey();
            IntegrationCache cacheEntry = entry.getValue();
            
            if (cacheEntry.getLastCheckedAt() == null || 
                cacheEntry.getLastCheckedAt().isBefore(cutoff)) {
                staleIntegrations.add(integrationId);
            }
        }
        
        return staleIntegrations;
    }
    
    /**
     * Get integrations with recent file activity
     */
    public Set<Long> getActiveIntegrations(int maxMinutesSinceFile) {
        Set<Long> activeIntegrations = new HashSet<>();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(maxMinutesSinceFile);
        
        for (Map.Entry<Long, IntegrationCache> entry : cache.entrySet()) {
            Long integrationId = entry.getKey();
            IntegrationCache cacheEntry = entry.getValue();
            
            if (cacheEntry.getLastFileFound() != null && 
                !cacheEntry.getLastFileFound().isBefore(cutoff)) {
                activeIntegrations.add(integrationId);
            }
        }
        
        return activeIntegrations;
    }
    
    /**
     * Get integration summary statistics
     */
    public Map<String, Object> getIntegrationSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        int integrationsWithFiles = 0;
        int integrationsCheckedRecently = 0;
        int integrationsWithRecentFiles = 0;
        long totalMinutesSinceLastCheck = 0;
        long totalMinutesSinceLastFile = 0;
        
        LocalDateTime recentCutoff = LocalDateTime.now().minusMinutes(60); // Last hour
        LocalDateTime veryRecentCutoff = LocalDateTime.now().minusMinutes(10); // Last 10 minutes
        
        for (IntegrationCache cacheEntry : cache.values()) {
            if (cacheEntry.hasRecentFile()) {
                integrationsWithFiles++;
            }
            
            if (cacheEntry.getLastCheckedAt() != null && 
                !cacheEntry.getLastCheckedAt().isBefore(recentCutoff)) {
                integrationsCheckedRecently++;
            }
            
            if (cacheEntry.getLastFileFound() != null && 
                !cacheEntry.getLastFileFound().isBefore(veryRecentCutoff)) {
                integrationsWithRecentFiles++;
            }
            
            long minutesSinceCheck = cacheEntry.getMinutesSinceLastCheck();
            if (minutesSinceCheck >= 0) {
                totalMinutesSinceLastCheck += minutesSinceCheck;
            }
            
            long minutesSinceFile = cacheEntry.getMinutesSinceLastFile();
            if (minutesSinceFile >= 0) {
                totalMinutesSinceLastFile += minutesSinceFile;
            }
        }
        
        summary.put("totalIntegrations", cache.size());
        summary.put("integrationsWithFiles", integrationsWithFiles);
        summary.put("integrationsCheckedRecently", integrationsCheckedRecently);
        summary.put("integrationsWithRecentFiles", integrationsWithRecentFiles);
        summary.put("averageMinutesSinceLastCheck", 
            integrationsWithFiles > 0 ? (double) totalMinutesSinceLastCheck / cache.size() : 0);
        summary.put("averageMinutesSinceLastFile", 
            integrationsWithFiles > 0 ? (double) totalMinutesSinceLastFile / integrationsWithFiles : 0);
        
        return summary;
    }
    
    /**
     * Get old cache entries (older than specified time)
     */
    @Override
    protected Set<Long> getOldCacheEntries(LocalDateTime cutoffTime) {
        Set<Long> oldEntries = new HashSet<>();
        
        for (Map.Entry<Long, IntegrationCache> entry : cache.entrySet()) {
            Long integrationId = entry.getKey();
            IntegrationCache cacheEntry = entry.getValue();
            
            if (cacheEntry.getCacheTime().isBefore(cutoffTime)) {
                oldEntries.add(integrationId);
            }
        }
        
        return oldEntries;
    }
    
    /**
     * Get all integration data for API response
     */
    public Map<String, Object> getAllIntegrationData() {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<Long, IntegrationCache> entry : cache.entrySet()) {
            Long integrationId = entry.getKey();
            IntegrationCache cacheEntry = entry.getValue();
            
            Map<String, Object> integrationData = new HashMap<>();
            integrationData.put("lastFileName", cacheEntry.getLastFileName());
            integrationData.put("lastFileFound", cacheEntry.getLastFileFound());
            integrationData.put("lastCheckedAt", cacheEntry.getLastCheckedAt());
            integrationData.put("hasRecentFile", cacheEntry.hasRecentFile());
            integrationData.put("minutesSinceLastCheck", cacheEntry.getMinutesSinceLastCheck());
            integrationData.put("minutesSinceLastFile", cacheEntry.getMinutesSinceLastFile());
            integrationData.put("cacheTime", cacheEntry.getCacheTime());
            
            result.put(integrationId.toString(), integrationData);
        }
        
        return result;
    }
}
