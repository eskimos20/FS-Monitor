package com.fsmonitor.app.cache;

import com.fsmonitor.app.cache.model.StorageInfoData;
import com.fsmonitor.app.cache.model.LargestFileData;
import com.fsmonitor.app.cache.model.DiskSpaceData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage-specific cache service extending AbstractCacheService
 */
@Service
public class StorageCacheService extends AbstractCacheService<StorageCacheService.StorageCache> {
    
    /**
     * Storage cache data structure
     */
    public static class StorageCache {
        private List<StorageInfoData> storageInfoList = new ArrayList<>();
        private List<LargestFileData> largestFiles = new ArrayList<>();
        private DiskSpaceData diskSpace;
        private LocalDateTime cacheTime;
        
        public StorageCache() {
            this.cacheTime = LocalDateTime.now();
        }
        
        public List<StorageInfoData> getStorageInfoList() { return storageInfoList; }
        
        public void setStorageInfoList(List<StorageInfoData> storageInfoList) { 
            this.storageInfoList = storageInfoList != null ? storageInfoList : new ArrayList<>();
            this.cacheTime = LocalDateTime.now();
        }
        
        public List<LargestFileData> getLargestFiles() { return largestFiles; }
        
        public void setLargestFiles(List<LargestFileData> largestFiles) { 
            this.largestFiles = largestFiles != null ? largestFiles : new ArrayList<>();
            this.cacheTime = LocalDateTime.now();
        }
        
        public DiskSpaceData getDiskSpace() { return diskSpace; }
        
        public void setDiskSpace(DiskSpaceData diskSpace) { 
            this.diskSpace = diskSpace;
            this.cacheTime = LocalDateTime.now();
        }
        
        public LocalDateTime getCacheTime() { return cacheTime; }
        
        public void updateCacheTime() {
            this.cacheTime = LocalDateTime.now();
        }
    }
    
    @Override
    protected StorageCache createNewCacheEntry() {
        return new StorageCache();
    }
    
    @Override
    protected void cleanupOldData() {
        // Storage cache doesn't accumulate data over time, so no specific cleanup needed
        // Data is replaced, not appended
        logger.debug("Storage cache cleanup completed - no action needed");
    }
    
    @Override
    protected Map<String, Object> getCacheEntryDetails(StorageCache cacheEntry) {
        Map<String, Object> details = new HashMap<>();
        details.put("storageInfoCount", cacheEntry.getStorageInfoList().size());
        details.put("largestFilesCount", cacheEntry.getLargestFiles().size());
        details.put("hasDiskSpace", cacheEntry.getDiskSpace() != null);
        details.put("cacheTime", cacheEntry.getCacheTime());
        
        if (cacheEntry.getDiskSpace() != null) {
            Map<String, Object> diskSpaceInfo = new HashMap<>();
            diskSpaceInfo.put("totalBytes", cacheEntry.getDiskSpace().getTotalBytes());
            diskSpaceInfo.put("usableBytes", cacheEntry.getDiskSpace().getUsableBytes());
            details.put("diskSpaceInfo", diskSpaceInfo);
        }
        
        return details;
    }
    
    // Storage-specific convenience methods
    
    /**
     * Get storage info list for specific config
     */
    public List<StorageInfoData> getStorageInfo(Long configId) {
        return getCache(configId).getStorageInfoList();
    }
    
    /**
     * Get largest files for specific config
     */
    public List<LargestFileData> getLargestFiles(Long configId) {
        return getCache(configId).getLargestFiles();
    }
    
    /**
     * Get disk space data for specific config
     */
    public DiskSpaceData getDiskSpace(Long configId) {
        return getCache(configId).getDiskSpace();
    }
    
    /**
     * Update storage info for specific config
     */
    public void updateStorageInfo(Long configId, List<StorageInfoData> storageInfoList) {
        StorageCache cache = getCache(configId);
        cache.setStorageInfoList(storageInfoList);
        logger.debug("Updated storage info for config {}: {} entries", configId, 
            storageInfoList != null ? storageInfoList.size() : 0);
    }
    
    /**
     * Update largest files for specific config
     */
    public void updateLargestFiles(Long configId, List<LargestFileData> largestFiles) {
        StorageCache cache = getCache(configId);
        cache.setLargestFiles(largestFiles);
        logger.debug("Updated largest files for config {}: {} files", configId, 
            largestFiles != null ? largestFiles.size() : 0);
    }
    
    /**
     * Update disk space for specific config
     */
    public void updateDiskSpace(Long configId, DiskSpaceData diskSpace) {
        StorageCache cache = getCache(configId);
        cache.setDiskSpace(diskSpace);
        logger.debug("Updated disk space for config {}: total={}GB, usable={}GB", configId,
            diskSpace != null ? diskSpace.getTotalBytes() / (1024L * 1024 * 1024) : 0,
            diskSpace != null ? diskSpace.getUsableBytes() / (1024L * 1024 * 1024) : 0);
    }
    
    /**
     * Get all storage data for all configs
     */
    public Map<String, Object> getAllStorageData() {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<Long, StorageCache> entry : cache.entrySet()) {
            Long configId = entry.getKey();
            StorageCache cacheEntry = entry.getValue();
            
            Map<String, Object> configData = new HashMap<>();
            configData.put("info", cacheEntry.getStorageInfoList());
            configData.put("largestFiles", cacheEntry.getLargestFiles());
            configData.put("diskSpace", cacheEntry.getDiskSpace());
            configData.put("cacheTime", cacheEntry.getCacheTime());
            
            result.put(configId.toString(), configData);
        }
        
        return result;
    }
    
    /**
     * Get cache age for specific config
     */
    public long getCacheAgeMinutes(Long configId) {
        StorageCache cache = getCache(configId);
        return java.time.Duration.between(cache.getCacheTime(), LocalDateTime.now()).toMinutes();
    }
    
    /**
     * Check if cache is fresh (less than 5 minutes old)
     */
    public boolean isCacheFresh(Long configId) {
        return getCacheAgeMinutes(configId) < 5;
    }
    
    /**
     * Get summary statistics for all storage caches
     */
    public Map<String, Object> getStorageSummaryStats() {
        Map<String, Object> summary = new HashMap<>();
        
        int totalStorageInfos = 0;
        int totalLargestFiles = 0;
        int cachesWithDiskSpace = 0;
        
        for (StorageCache cache : cache.values()) {
            totalStorageInfos += cache.getStorageInfoList().size();
            totalLargestFiles += cache.getLargestFiles().size();
            if (cache.getDiskSpace() != null) {
                cachesWithDiskSpace++;
            }
        }
        
        summary.put("totalCaches", cache.size());
        summary.put("totalStorageInfos", totalStorageInfos);
        summary.put("totalLargestFiles", totalLargestFiles);
        summary.put("cachesWithDiskSpace", cachesWithDiskSpace);
        summary.put("averageStorageInfosPerCache", cache.size() > 0 ? (double) totalStorageInfos / cache.size() : 0);
        summary.put("averageLargestFilesPerCache", cache.size() > 0 ? (double) totalLargestFiles / cache.size() : 0);
        
        return summary;
    }
}
