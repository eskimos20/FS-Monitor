package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.MonitoringCacheManager;
import com.fsmonitor.app.entity.StorageConfig;
import com.fsmonitor.app.repository.StorageConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class StorageMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(StorageMonitoringService.class);
    private static final int TOP_FILES_LIMIT = 20;
    private static final int TOP_DIRECTORIES_LIMIT = 20;
    
    @Autowired
    private StorageConfigRepository storageConfigRepository;
    
    @Autowired
    private MonitoringCacheManager cacheManager;
    
    private final Map<Long, Long> lastCheckTimes = new ConcurrentHashMap<>();
    
    private long calculateIntervalMillis(StorageConfig config) {
        int interval = config.getCheckIntervalMinutes();
        String unit = config.getIntervalUnit();
        
        switch (unit) {
            case "HOURS":
                return interval * 60L * 60 * 1000;
            case "DAYS":
                return interval * 24L * 60 * 60 * 1000;
            case "MONTHS":
                return interval * 30L * 24 * 60 * 60 * 1000;
            case "MINUTES":
            default:
                return interval * 60L * 1000;
        }
    }
    
    @Scheduled(fixedRate = 60000, initialDelay = 60000) // Check every minute, wait 1 min after startup
    public void monitorStorage() {
        List<StorageConfig> activeConfigs = storageConfigRepository.findByActiveTrue();
        long currentTime = System.currentTimeMillis();
        
        for (StorageConfig config : activeConfigs) {
            Long lastCheck = lastCheckTimes.get(config.getId());
            
            // Initialize lastCheck for new configs without scanning immediately
            if (lastCheck == null) {
                lastCheckTimes.put(config.getId(), currentTime);
                logger.info("Initialized storage monitoring for config: {} - will scan after interval", config.getName());
                continue;
            }
            
            long intervalMillis = calculateIntervalMillis(config);
            
            if ((currentTime - lastCheck) >= intervalMillis) {
                logger.info("Scanning storage for config: {} ({})", config.getName(), config.getPath());
                try {
                    // Clear cache for this storage config when a new scan runs
                    cacheManager.clearStorageCache(config.getId());
                    scanStorage(config);
                    lastCheckTimes.put(config.getId(), currentTime);
                } catch (Exception e) {
                    logger.error("Error in monitorStorage for config {}: {}", config.getName(), e.getMessage());
                }
            }
        }
    }
    
    public void scanStorage(StorageConfig config) {
        try {
            Path rootPath = Paths.get(config.getPath());
            
            if (!Files.exists(rootPath)) {
                logger.warn("Path does not exist: {}", config.getPath());
                return;
            }
            
            MonitoringCacheManager.StorageCache cache = cacheManager.getStorageCache(config.getId());
            
            // Scan directories
            Map<String, DirectoryStats> directoryStatsMap = new HashMap<>();
            List<FileInfo> allFiles = new ArrayList<>();
            
            if (config.getRecursive()) {
                scanRecursive(rootPath, config, directoryStatsMap, allFiles);
            } else {
                scanSingleDirectory(rootPath, config, directoryStatsMap, allFiles);
            }
            
            // Save top largest directories by size
            List<Map.Entry<String, DirectoryStats>> topDirectories = directoryStatsMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().totalSize, e1.getValue().totalSize))
                .limit(TOP_DIRECTORIES_LIMIT)
                .collect(Collectors.toList());
            
            List<MonitoringCacheManager.StorageInfoData> storageInfoList = new ArrayList<>();
            for (Map.Entry<String, DirectoryStats> entry : topDirectories) {
                MonitoringCacheManager.StorageInfoData info = new MonitoringCacheManager.StorageInfoData();
                info.setPath(entry.getKey());
                info.setTotalSizeBytes(entry.getValue().totalSize);
                info.setFileCount(entry.getValue().fileCount);
                info.setDirectoryCount(entry.getValue().directoryCount);
                info.setScannedAt(LocalDateTime.now());
                storageInfoList.add(info);
            }
            cache.setStorageInfoList(storageInfoList);
            
            // Save top largest files
            List<FileInfo> topFiles = allFiles.stream()
                .sorted(Comparator.comparingLong(f -> -f.size))
                .limit(TOP_FILES_LIMIT)
                .collect(Collectors.toList());
            
            List<MonitoringCacheManager.LargestFileData> largestFilesList = new ArrayList<>();
            for (FileInfo fileInfo : topFiles) {
                MonitoringCacheManager.LargestFileData largestFile = new MonitoringCacheManager.LargestFileData();
                largestFile.setFilePath(fileInfo.path);
                largestFile.setSizeBytes(fileInfo.size);
                largestFile.setScannedAt(LocalDateTime.now());
                largestFilesList.add(largestFile);
            }
            cache.setLargestFiles(largestFilesList);
            
            // Calculate total size of the directory
            long totalSize = directoryStatsMap.values().stream()
                .mapToLong(stats -> stats.totalSize)
                .sum();
            
            // Save disk space information to cache
            MonitoringCacheManager.DiskSpaceData diskSpace = new MonitoringCacheManager.DiskSpaceData();
            diskSpace.setTotalBytes(totalSize);
            diskSpace.setUsableBytes(0L);
            diskSpace.setScannedAt(LocalDateTime.now());
            cache.setDiskSpace(diskSpace);
            
            logger.info("Storage scan completed for {}: {} directories, {} files, total size: {} bytes", 
                config.getName(), directoryStatsMap.size(), allFiles.size(), totalSize);
            
        } catch (Exception e) {
            logger.error("Error scanning storage for config {}: {}", config.getName(), e.getMessage());
        }
    }
    
    private void scanRecursive(Path rootPath, StorageConfig config, 
                               Map<String, DirectoryStats> directoryStatsMap, 
                               List<FileInfo> allFiles) throws IOException {
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    // Skip hidden files
                    if (file.getFileName().toString().startsWith(".")) {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    if (attrs.isRegularFile()) {
                        long size = attrs.size();
                        String parentPath = file.getParent().toString();
                        
                        // Add to parent directory stats
                        directoryStatsMap.computeIfAbsent(parentPath, k -> new DirectoryStats())
                            .addFile(size);
                        
                        // Add to all files list
                        allFiles.add(new FileInfo(file.toString(), size));
                    }
                } catch (Exception e) {
                    logger.debug("Error processing file {}: {}", file, e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Skip hidden directories
                if (!dir.equals(rootPath) && dir.getFileName().toString().startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                String dirPath = dir.toString();
                directoryStatsMap.computeIfAbsent(dirPath, k -> new DirectoryStats())
                    .addDirectory();
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.debug("Cannot access {}: {}", file, exc.getMessage());
                return FileVisitResult.SKIP_SUBTREE;
            }
        });
    }
    
    private void scanSingleDirectory(Path rootPath, StorageConfig config,
                                     Map<String, DirectoryStats> directoryStatsMap,
                                     List<FileInfo> allFiles) throws IOException {
        DirectoryStats stats = new DirectoryStats();
        String rootPathStr = rootPath.toString();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
            for (Path entry : stream) {
                try {
                    // Skip hidden files and directories
                    if (entry.getFileName().toString().startsWith(".")) {
                        continue;
                    }
                    
                    BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                    
                    if (attrs.isRegularFile()) {
                        long size = attrs.size();
                        stats.addFile(size);
                        allFiles.add(new FileInfo(entry.toString(), size));
                    } else if (attrs.isDirectory()) {
                        stats.addDirectory();
                    }
                } catch (Exception e) {
                    logger.debug("Error processing {}: {}", entry, e.getMessage());
                }
            }
        }
        
        directoryStatsMap.put(rootPathStr, stats);
    }
    
    private static class DirectoryStats {
        long totalSize = 0;
        int fileCount = 0;
        int directoryCount = 0;
        
        void addFile(long size) {
            totalSize += size;
            fileCount++;
        }
        
        void addDirectory() {
            directoryCount++;
        }
    }
    
    private static class FileInfo {
        String path;
        long size;
        
        FileInfo(String path, long size) {
            this.path = path;
            this.size = size;
        }
    }
}
