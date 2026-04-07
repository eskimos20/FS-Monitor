package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.MonitoringCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StorageCacheService {
    
    @Autowired
    private MonitoringCacheManager cacheManager;
    
    public List<MonitoringCacheManager.StorageInfoData> getStorageInfo(Long configId) {
        MonitoringCacheManager.StorageCache cache = cacheManager.getStorageCache(configId);
        return cache.getStorageInfoList();
    }
    
    public List<MonitoringCacheManager.LargestFileData> getLargestFiles(Long configId) {
        MonitoringCacheManager.StorageCache cache = cacheManager.getStorageCache(configId);
        return cache.getLargestFiles();
    }
    
    public MonitoringCacheManager.DiskSpaceData getDiskSpace(Long configId) {
        MonitoringCacheManager.StorageCache cache = cacheManager.getStorageCache(configId);
        return cache.getDiskSpace();
    }
    
    public void clearStorageData(Long configId) {
        cacheManager.clearStorageCache(configId);
    }
    
    public Map<String, Object> getAllStorageData() {
        Map<String, Object> result = new HashMap<>();
        
        // Get all storage cache entries
        Set<Long> configIds = cacheManager.getAllStorageCacheIds();
        
        for (Long configId : configIds) {
            MonitoringCacheManager.StorageCache cache = cacheManager.getStorageCache(configId);
            
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("info", cache.getStorageInfoList());
            cacheData.put("largestFiles", cache.getLargestFiles());
            cacheData.put("diskSpace", cache.getDiskSpace());
            
            result.put(configId.toString(), cacheData);
        }
        
        return result;
    }
}
