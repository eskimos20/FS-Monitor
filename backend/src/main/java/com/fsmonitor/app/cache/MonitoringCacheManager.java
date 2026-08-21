package com.fsmonitor.app.cache;

import com.fsmonitor.app.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MonitoringCacheManager {
    
    private final Map<Long, IntegrationCache> integrationCache = new ConcurrentHashMap<>();
    private final Map<Long, ServiceCache> serviceCache = new ConcurrentHashMap<>();
    private final Map<Long, StorageCache> storageCache = new ConcurrentHashMap<>();
    private final Map<Long, LogCache> logCache = new ConcurrentHashMap<>();
    private final Map<String, NotificationCache> notificationCache = new ConcurrentHashMap<>();
    
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
    }
    
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
    }
    
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
            this.storageInfoList = storageInfoList;
            this.cacheTime = LocalDateTime.now();
        }
        
        public List<LargestFileData> getLargestFiles() { return largestFiles; }
        public void setLargestFiles(List<LargestFileData> largestFiles) { 
            this.largestFiles = largestFiles;
            this.cacheTime = LocalDateTime.now();
        }
        
        public DiskSpaceData getDiskSpace() { return diskSpace; }
        public void setDiskSpace(DiskSpaceData diskSpace) { 
            this.diskSpace = diskSpace;
            this.cacheTime = LocalDateTime.now();
        }
        
        public LocalDateTime getCacheTime() { return cacheTime; }
    }
    
    public static class StorageInfoData {
        private String path;
        private Long totalSizeBytes;
        private Integer fileCount;
        private Integer directoryCount;
        private LocalDateTime scannedAt;
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public Long getTotalSizeBytes() { return totalSizeBytes; }
        public void setTotalSizeBytes(Long totalSizeBytes) { this.totalSizeBytes = totalSizeBytes; }
        
        public Integer getFileCount() { return fileCount; }
        public void setFileCount(Integer fileCount) { this.fileCount = fileCount; }
        
        public Integer getDirectoryCount() { return directoryCount; }
        public void setDirectoryCount(Integer directoryCount) { this.directoryCount = directoryCount; }
        
        public LocalDateTime getScannedAt() { return scannedAt; }
        public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }
    }
    
    public static class LargestFileData {
        private String filePath;
        private Long sizeBytes;
        private LocalDateTime scannedAt;
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public Long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
        
        public LocalDateTime getScannedAt() { return scannedAt; }
        public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }
    }
    
    public static class DiskSpaceData {
        private Long totalBytes;
        private Long usableBytes;
        private LocalDateTime scannedAt;
        
        public Long getTotalBytes() { return totalBytes; }
        public void setTotalBytes(Long totalBytes) { this.totalBytes = totalBytes; }
        
        public Long getUsableBytes() { return usableBytes; }
        public void setUsableBytes(Long usableBytes) { this.usableBytes = usableBytes; }
        
        public LocalDateTime getScannedAt() { return scannedAt; }
        public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }
    }
    
    public static class LogCache {
        private List<LogMatchResultData> matches = new java.util.concurrent.CopyOnWriteArrayList<>();
        private LocalDateTime cacheTime;
        private static final int MAX_MATCHES = 1000;
        
        public LogCache() {
            this.cacheTime = LocalDateTime.now();
        }
        
        public List<LogMatchResultData> getMatches() { return matches; }
        
        public void addMatch(LogMatchResultData match) {
            matches.add(0, match);
            if (matches.size() > MAX_MATCHES) {
                matches.remove(matches.size() - 1);
            }
            this.cacheTime = LocalDateTime.now();
        }
        
        public void clearMatches() {
            matches.clear();
            this.cacheTime = LocalDateTime.now();
        }
        
        public LocalDateTime getCacheTime() { return cacheTime; }
    }
    
    public static class LogMatchResultData {
        private Long logConfigId;
        private String fileName;
        private String keyword;
        private int lineNumber;
        private String matchedLine;
        private String contextBefore;
        private String contextAfter;
        private LocalDateTime foundAt;
        
        public Long getLogConfigId() { return logConfigId; }
        public void setLogConfigId(Long logConfigId) { this.logConfigId = logConfigId; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
        
        public String getMatchedLine() { return matchedLine; }
        public void setMatchedLine(String matchedLine) { this.matchedLine = matchedLine; }
        
        public String getContextBefore() { return contextBefore; }
        public void setContextBefore(String contextBefore) { this.contextBefore = contextBefore; }
        
        public String getContextAfter() { return contextAfter; }
        public void setContextAfter(String contextAfter) { this.contextAfter = contextAfter; }
        
        public LocalDateTime getFoundAt() { return foundAt; }
        public void setFoundAt(LocalDateTime foundAt) { this.foundAt = foundAt; }
    }
    
    public static class NotificationCache {
        private NotificationType type;
        private Long entityId;
        private String entityName;
        private LocalDateTime sentAt;
        
        public NotificationType getType() { return type; }
        public void setType(NotificationType type) { this.type = type; }
        
        public Long getEntityId() { return entityId; }
        public void setEntityId(Long entityId) { this.entityId = entityId; }
        
        public String getEntityName() { return entityName; }
        public void setEntityName(String entityName) { this.entityName = entityName; }
        
        public LocalDateTime getSentAt() { return sentAt; }
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    }
    
    public IntegrationCache getIntegrationCache(Long integrationId) {
        return integrationCache.computeIfAbsent(integrationId, k -> new IntegrationCache());
    }
    
    public void clearIntegrationCache(Long integrationId) {
        integrationCache.remove(integrationId);
    }
    
    public ServiceCache getServiceCache(Long serviceId) {
        return serviceCache.computeIfAbsent(serviceId, k -> new ServiceCache());
    }
    
    public void clearServiceCache(Long serviceId) {
        serviceCache.remove(serviceId);
    }
    
    public StorageCache getStorageCache(Long storageConfigId) {
        return storageCache.computeIfAbsent(storageConfigId, k -> new StorageCache());
    }
    
    public void clearStorageCache(Long storageConfigId) {
        storageCache.remove(storageConfigId);
    }
    
    public LogCache getLogCache(Long logConfigId) {
        return logCache.computeIfAbsent(logConfigId, k -> new LogCache());
    }
    
    public void clearLogCache(Long logConfigId) {
        logCache.remove(logConfigId);
    }
    
    public Optional<NotificationCache> getNotification(NotificationType type, Long entityId) {
        String key = type.name() + "_" + entityId;
        return Optional.ofNullable(notificationCache.get(key));
    }
    
    public void saveNotification(NotificationType type, Long entityId, String entityName) {
        String key = type.name() + "_" + entityId;
        NotificationCache cache = new NotificationCache();
        cache.setType(type);
        cache.setEntityId(entityId);
        cache.setEntityName(entityName);
        cache.setSentAt(LocalDateTime.now());
        notificationCache.put(key, cache);
    }
    
    public void clearNotification(NotificationType type, Long entityId) {
        String key = type.name() + "_" + entityId;
        notificationCache.remove(key);
    }
    
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("integrationCacheSize", integrationCache.size());
        stats.put("serviceCacheSize", serviceCache.size());
        stats.put("storageCacheSize", storageCache.size());
        stats.put("logCacheSize", logCache.size());
        stats.put("notificationCacheSize", notificationCache.size());
        
        int totalLogMatches = logCache.values().stream()
            .mapToInt(cache -> cache.getMatches().size())
            .sum();
        stats.put("totalLogMatches", totalLogMatches);
        
        return stats;
    }
    
    public void clearAllCaches() {
        integrationCache.clear();
        serviceCache.clear();
        storageCache.clear();
        logCache.clear();
        notificationCache.clear();
    }
    
    public Set<Long> getAllIntegrationCacheIds() {
        return new HashSet<>(integrationCache.keySet());
    }
    
    public Set<Long> getAllServiceCacheIds() {
        return new HashSet<>(serviceCache.keySet());
    }
    
    public Set<Long> getAllStorageCacheIds() {
        return new HashSet<>(storageCache.keySet());
    }
    
    public Set<Long> getAllLogCacheIds() {
        return new HashSet<>(logCache.keySet());
    }
}
