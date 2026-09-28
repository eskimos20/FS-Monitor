package com.fsmonitor.app.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for all cache services.
 * Provides unified cache management, cleanup, and metrics.
 * 
 * @param <T> The cache data type
 */
public abstract class AbstractCacheService<T> {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Map<Long, T> cache = new ConcurrentHashMap<>();
    
    /**
     * Get cache entry for specific ID. Creates new entry if it doesn't exist.
     */
    public T getCache(Long id) {
        return cache.computeIfAbsent(id, k -> createNewCacheEntry());
    }
    
    /**
     * Clear cache for specific ID
     */
    public void clearCache(Long id) {
        cache.remove(id);
        logger.debug("Cleared cache for ID: {}", id);
    }
    
    /**
     * Clear all cache entries
     */
    public void clearAllCache() {
        cache.clear();
        logger.debug("Cleared all cache entries");
    }
    
    /**
     * Get all cached IDs
     */
    public Set<Long> getAllCachedIds() {
        return new HashSet<>(cache.keySet());
    }
    
    /**
     * Get cache size
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Check if cache contains entry for ID
     */
    public boolean hasCache(Long id) {
        return cache.containsKey(id);
    }
    
    /**
     * Remove cache entries for deleted configurations
     * This prevents memory leaks when configurations are deleted from database
     */
    public void cleanupDeletedConfigs(Set<Long> activeConfigIds) {
        Set<Long> cachedIds = getAllCachedIds();
        for (Long cachedId : cachedIds) {
            if (!activeConfigIds.contains(cachedId)) {
                clearCache(cachedId);
                logger.debug("Cleaned up cache for deleted config ID: {}", cachedId);
            }
        }
    }
    
    /**
     * Scheduled cleanup of old data
     * Runs every 5 minutes - override in subclasses for specific cleanup logic
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 300000) // Every 5 minutes, wait 5 min after startup
    public void scheduledCleanup() {
        try {
            cleanupOldData();
            logger.debug("Completed scheduled cleanup for {}", getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("Error during scheduled cleanup for {}", getClass().getSimpleName(), e);
        }
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", cache.size());
        stats.put("cachedIds", new ArrayList<>(cache.keySet()));
        stats.put("serviceName", getClass().getSimpleName());
        stats.put("lastCleanup", LocalDateTime.now());
        return stats;
    }
    
    /**
     * Create a new cache entry - must be implemented by subclasses
     */
    protected abstract T createNewCacheEntry();
    
    /**
     * Cleanup old data - override in subclasses for specific cleanup logic
     */
    protected abstract void cleanupOldData();
    
    /**
     * Get cache entry details - override in subclasses for specific metrics
     */
    protected abstract Map<String, Object> getCacheEntryDetails(T cacheEntry);
    
    /**
     * Get detailed cache statistics including entry-specific data
     */
    public Map<String, Object> getDetailedCacheStats() {
        Map<String, Object> stats = getCacheStats();
        
        Map<Long, Object> entryDetails = new HashMap<>();
        for (Map.Entry<Long, T> entry : cache.entrySet()) {
            entryDetails.put(entry.getKey(), getCacheEntryDetails(entry.getValue()));
        }
        stats.put("entryDetails", entryDetails);
        
        return stats;
    }
    
    /**
     * Get cache entries that are older than specified time
     * Override in subclasses if age-based cleanup is needed
     */
    protected Set<Long> getOldCacheEntries(LocalDateTime cutoffTime) {
        return new HashSet<>(); // Default: no age-based cleanup
    }
    
    /**
     * Force cleanup of entries older than cutoff time
     */
    public void cleanupOlderThan(LocalDateTime cutoffTime) {
        Set<Long> oldEntries = getOldCacheEntries(cutoffTime);
        for (Long id : oldEntries) {
            clearCache(id);
            logger.debug("Cleaned up old cache entry ID: {} (older than {})", id, cutoffTime);
        }
    }
}
