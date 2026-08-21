package com.fsmonitor.app.cache;

import com.fsmonitor.app.cache.MonitoringCacheManager.LogMatchResultData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Log-specific cache service extending AbstractCacheService
 */
@Service
public class LogCacheService extends AbstractCacheService<LogCacheService.LogCache> {
    
    private static final int MAX_MATCHES = 1000;
    
    /**
     * Log cache data structure with built-in size limit
     */
    public static class LogCache {
        private List<LogMatchResultData> matches = new CopyOnWriteArrayList<>();
        private LocalDateTime cacheTime;
        
        public LogCache() {
            this.cacheTime = LocalDateTime.now();
        }
        
        public List<LogMatchResultData> getMatches() { return matches; }
        
        public void addMatch(LogMatchResultData match) {
            matches.add(0, match); // Add to front (newest first)
            if (matches.size() > MAX_MATCHES) {
                matches.remove(matches.size() - 1); // Remove oldest
            }
            this.cacheTime = LocalDateTime.now();
        }
        
        public void clearMatches() {
            matches.clear();
            this.cacheTime = LocalDateTime.now();
        }
        
        public LocalDateTime getCacheTime() { return cacheTime; }
        
        public int getMatchCount() { return matches.size(); }
        
        public boolean isFull() { return matches.size() >= MAX_MATCHES; }
    }
    
    @Override
    protected LogCache createNewCacheEntry() {
        return new LogCache();
    }
    
    @Override
    protected void cleanupOldData() {
        // Keep only last 100 matches per config to prevent memory growth
        for (Map.Entry<Long, LogCache> entry : cache.entrySet()) {
            Long configId = entry.getKey();
            LogCache logCache = entry.getValue();
            
            if (logCache.getMatches().size() > 100) {
                List<LogMatchResultData> matches = logCache.getMatches();
                List<LogMatchResultData> toKeep = new ArrayList<>(matches.subList(0, 100));
                logCache.clearMatches();
                logCache.getMatches().addAll(toKeep);
                logger.debug("Cleaned up old matches for config {}: kept 100 of {}", 
                    configId, matches.size());
            }
        }
    }
    
    @Override
    protected Map<String, Object> getCacheEntryDetails(LogCache cacheEntry) {
        Map<String, Object> details = new HashMap<>();
        details.put("matchCount", cacheEntry.getMatchCount());
        details.put("isFull", cacheEntry.isFull());
        details.put("cacheTime", cacheEntry.getCacheTime());
        details.put("maxMatches", MAX_MATCHES);
        
        // Count matches by keyword
        Map<String, Integer> keywordCounts = new HashMap<>();
        for (LogMatchResultData match : cacheEntry.getMatches()) {
            String keyword = match.getKeyword();
            keywordCounts.put(keyword, keywordCounts.getOrDefault(keyword, 0) + 1);
        }
        details.put("keywordCounts", keywordCounts);
        
        // Get oldest and newest match times
        if (!cacheEntry.getMatches().isEmpty()) {
            LogMatchResultData newest = cacheEntry.getMatches().get(0);
            LogMatchResultData oldest = cacheEntry.getMatches().get(cacheEntry.getMatches().size() - 1);
            details.put("newestMatchTime", newest.getFoundAt());
            details.put("oldestMatchTime", oldest.getFoundAt());
        }
        
        return details;
    }
    
    // Log-specific convenience methods
    
    /**
     * Add a new match to the cache
     */
    public void addMatch(Long configId, LogMatchResultData match) {
        LogCache logCache = getCache(configId);
        logCache.addMatch(match);
        logger.debug("Added match for config {}: keyword={}, total matches={}", 
            configId, match.getKeyword(), logCache.getMatchCount());
    }
    
    /**
     * Get all matches for specific config
     */
    public List<LogMatchResultData> getMatches(Long configId) {
        return getCache(configId).getMatches();
    }
    
    /**
     * Get matches for specific config within time range
     */
    public List<LogMatchResultData> getMatchesInTimeRange(Long configId, LocalDateTime start, LocalDateTime end) {
        List<LogMatchResultData> allMatches = getMatches(configId);
        List<LogMatchResultData> filteredMatches = new ArrayList<>();
        
        for (LogMatchResultData match : allMatches) {
            LocalDateTime matchTime = match.getFoundAt();
            if ((start == null || !matchTime.isBefore(start)) && 
                (end == null || !matchTime.isAfter(end))) {
                filteredMatches.add(match);
            }
        }
        
        return filteredMatches;
    }
    
    /**
     * Get recent matches for all configs (last N hours)
     */
    public List<LogMatchResultData> getRecentMatches(int hours) {
        List<LogMatchResultData> recentMatches = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        
        for (LogCache logCache : cache.values()) {
            for (LogMatchResultData match : logCache.getMatches()) {
                if (!match.getFoundAt().isBefore(cutoff)) {
                    recentMatches.add(match);
                }
            }
        }
        
        // Sort by found time (newest first)
        recentMatches.sort((a, b) -> b.getFoundAt().compareTo(a.getFoundAt()));
        return recentMatches;
    }
    
    /**
     * Get matches for specific config and keyword
     */
    public List<LogMatchResultData> getMatchesForKeyword(Long configId, String keyword) {
        List<LogMatchResultData> allMatches = getMatches(configId);
        List<LogMatchResultData> keywordMatches = new ArrayList<>();
        
        for (LogMatchResultData match : allMatches) {
            if (keyword.equalsIgnoreCase(match.getKeyword())) {
                keywordMatches.add(match);
            }
        }
        
        return keywordMatches;
    }
    
    /**
     * Clear all matches for specific config
     */
    public void clearMatchesForConfig(Long configId) {
        LogCache logCache = getCache(configId);
        logCache.clearMatches();
        logger.debug("Cleared all matches for config {}", configId);
    }
    
    /**
     * Clear all matches for all configs
     */
    public void clearAllMatches() {
        for (LogCache logCache : cache.values()) {
            logCache.clearMatches();
        }
        logger.debug("Cleared all matches from all configs");
    }
    
    /**
     * Get match statistics for specific config
     */
    public Map<String, Object> getMatchStats(Long configId) {
        LogCache logCache = getCache(configId);
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalMatches", logCache.getMatchCount());
        stats.put("isFull", logCache.isFull());
        stats.put("cacheTime", logCache.getCacheTime());
        
        // Keyword distribution
        Map<String, Integer> keywordCounts = new HashMap<>();
        for (LogMatchResultData match : logCache.getMatches()) {
            String keyword = match.getKeyword();
            keywordCounts.put(keyword, keywordCounts.getOrDefault(keyword, 0) + 1);
        }
        stats.put("keywordDistribution", keywordCounts);
        
        // File distribution
        Map<String, Integer> fileCounts = new HashMap<>();
        for (LogMatchResultData match : logCache.getMatches()) {
            String fileName = match.getFileName();
            fileCounts.put(fileName, fileCounts.getOrDefault(fileName, 0) + 1);
        }
        stats.put("fileDistribution", fileCounts);
        
        return stats;
    }
    
    /**
     * Get overall log cache statistics
     */
    public Map<String, Object> getLogCacheSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        int totalMatches = 0;
        int fullCaches = 0;
        Map<String, Integer> globalKeywordCounts = new HashMap<>();
        
        for (Map.Entry<Long, LogCache> entry : cache.entrySet()) {
            Long configId = entry.getKey();
            LogCache logCache = entry.getValue();
            
            totalMatches += logCache.getMatchCount();
            if (logCache.isFull()) {
                fullCaches++;
            }
            
            // Aggregate keyword counts
            for (LogMatchResultData match : logCache.getMatches()) {
                String keyword = match.getKeyword();
                globalKeywordCounts.put(keyword, globalKeywordCounts.getOrDefault(keyword, 0) + 1);
            }
        }
        
        summary.put("totalConfigs", cache.size());
        summary.put("totalMatches", totalMatches);
        summary.put("fullCaches", fullCaches);
        summary.put("averageMatchesPerConfig", cache.size() > 0 ? (double) totalMatches / cache.size() : 0);
        summary.put("globalKeywordDistribution", globalKeywordCounts);
        summary.put("maxMatchesPerConfig", MAX_MATCHES);
        
        return summary;
    }
    
    /**
     * Get old cache entries (older than specified time)
     */
    @Override
    protected Set<Long> getOldCacheEntries(LocalDateTime cutoffTime) {
        Set<Long> oldEntries = new HashSet<>();
        
        for (Map.Entry<Long, LogCache> entry : cache.entrySet()) {
            Long configId = entry.getKey();
            LogCache logCache = entry.getValue();
            
            if (logCache.getCacheTime().isBefore(cutoffTime)) {
                oldEntries.add(configId);
            }
        }
        
        return oldEntries;
    }
}
