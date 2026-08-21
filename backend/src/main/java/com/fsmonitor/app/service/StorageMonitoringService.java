package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.StorageCacheService;
import com.fsmonitor.app.cache.StorageCacheService.StorageCache;
import com.fsmonitor.app.cache.MonitoringCacheManager.StorageInfoData;
import com.fsmonitor.app.cache.MonitoringCacheManager.LargestFileData;
import com.fsmonitor.app.cache.MonitoringCacheManager.DiskSpaceData;
import com.fsmonitor.app.entity.StorageConfig;
import com.fsmonitor.app.repository.StorageConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
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
    private StorageCacheService storageCacheService;
    
    private final Map<Long, Long> lastCheckTimes = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeStorageMonitoring() {
        // Note: Don't scan immediately to avoid double scanning with @Scheduled
        // Let @Scheduled handle the first scan after initialDelay
    }
    
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
    
    @Scheduled(fixedDelay = 60000, initialDelay = 60000) // Check every minute, wait 1 min after startup
    public void monitorStorage() {
        List<StorageConfig> activeConfigs = storageConfigRepository.findByActiveTrue();
        long currentTime = System.currentTimeMillis();
        
        // Always scan system-wide storage first
        scanSystemStorage();
        
        for (StorageConfig config : activeConfigs) {
            Long lastCheck = lastCheckTimes.get(config.getId());
            
            // Initialize lastCheck for new configs and scan immediately
            if (lastCheck == null) {
                try {
                    scanStorage(config);
                    lastCheckTimes.put(config.getId(), currentTime);
                } catch (Exception e) {
                    logger.error("Error in initial scan for config {}: {}", config.getName(), e.getMessage());
                    lastCheckTimes.put(config.getId(), currentTime); // Still set time to avoid retry loops
                }
                continue;
            }
            
            long intervalMillis = calculateIntervalMillis(config);
            
            if ((currentTime - lastCheck) >= intervalMillis) {
                try {
                    scanStorage(config);
                    lastCheckTimes.put(config.getId(), currentTime);
                } catch (Exception e) {
                    logger.error("Error in monitorStorage for config {}: {}", config.getName(), e.getMessage());
                }
            }
        }
    }
    
    private void scanSystemStorage() {
        long startTime = System.currentTimeMillis();
        try {
            // Create a virtual system config
            StorageConfig systemConfig = new StorageConfig();
            systemConfig.setId(-1L);
            systemConfig.setName("System Storage");
            systemConfig.setPath("/");
            systemConfig.setActive(true);
            
            StorageCache cache = storageCacheService.getCache(-1L);
            
            // Get disk space using df command
            List<StorageInfoData> storageInfoList = getDiskSpaceInfo();
            cache.setStorageInfoList(storageInfoList);
            
            // Get system disk space data
            DiskSpaceData diskSpace = getSystemDiskSpace();
            cache.setDiskSpace(diskSpace);
            
            // Clear largest files for system-wide (not relevant)
            cache.setLargestFiles(new ArrayList<>());
            
        } catch (Exception e) {
            logger.error("Error scanning system-wide storage: {}", e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        logger.info("File System Storage scan completed - Duration: {}ms", (endTime - startTime));
    }
    
    private List<StorageInfoData> getDiskSpaceInfo() {
        List<StorageInfoData> result = new ArrayList<>();
        try {
            // Use df -h with built-in exclusion flags for local filesystems only
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "df -h -l -x tmpfs -x devtmpfs -x efivarfs");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean firstLine = true; // Skip header
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    
                    // Skip total line
                    if (line.trim().startsWith("total")) {
                        continue;
                    }
                    
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 6) {
                        String mountPath = parts[5]; // Mounted on path
                        
                                                
                        StorageInfoData info = new StorageInfoData();
                        info.setPath(mountPath);
                        info.setFileCount(0); // Not available from df
                        info.setDirectoryCount(1); // It's a mount point
                        info.setTotalSizeBytes(parseSize(parts[1])); // Size
                        info.setScannedAt(LocalDateTime.now());
                        result.add(info);
                    }
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("df command exited with code: {}", exitCode);
            }
            
        } catch (Exception e) {
            logger.error("Error executing df command: {}", e.getMessage());
        }
        return result;
    }
    
    private DiskSpaceData getSystemDiskSpace() {
        DiskSpaceData diskSpace = new DiskSpaceData();
        try {
            // Get total disk space using --total flag
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "df -h -l -x tmpfs -x devtmpfs -x efivarfs --total");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    
                    // Look for the total line
                    if (line.trim().startsWith("total")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 4) {
                            // parts[1] = Size, parts[2] = Used, parts[3] = Avail
                            diskSpace.setTotalBytes(parseSize(parts[1]));
                            diskSpace.setUsableBytes(parseSize(parts[3])); // Available space
                            diskSpace.setScannedAt(LocalDateTime.now());
                            break;
                        }
                    }
                }
            }
            
            process.waitFor();
            
        } catch (Exception e) {
            logger.error("Error getting system disk space: {}", e.getMessage());
            // Set default values
            diskSpace.setTotalBytes(0L);
            diskSpace.setUsableBytes(0L);
            diskSpace.setScannedAt(LocalDateTime.now());
        }
        return diskSpace;
    }
    
    private long parseSize(String sizeStr) {
        try {
            // Parse sizes like "10G", "500M", "1.5T", "2P", "5E"
            sizeStr = sizeStr.toUpperCase();
            if (sizeStr.endsWith("K")) {
                return (long) (Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) * 1024);
            } else if (sizeStr.endsWith("M")) {
                return (long) (Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) * 1024 * 1024);
            } else if (sizeStr.endsWith("G")) {
                return (long) (Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) * 1024 * 1024 * 1024);
            } else if (sizeStr.endsWith("T")) {
                return (long) (Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) * 1024L * 1024 * 1024 * 1024);
            } else if (sizeStr.endsWith("P")) {
                return (long) (Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) * 1024L * 1024 * 1024 * 1024 * 1024);
            } else if (sizeStr.endsWith("E")) {
                return (long) (Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1)) * 1024L * 1024 * 1024 * 1024 * 1024 * 1024);
            } else {
                // Assume bytes
                return Long.parseLong(sizeStr);
            }
        } catch (Exception e) {
            logger.debug("Error parsing size '{}': {}", sizeStr, e.getMessage());
            return 0L;
        }
    }
    
    public void scanStorage(StorageConfig config) {
        long startTime = System.currentTimeMillis();
        try {
            Path rootPath = Paths.get(config.getPath());
            
            if (!Files.exists(rootPath)) {
                logger.warn("Path does not exist: {}", config.getPath());
                return;
            }
            
            StorageCache cache = storageCacheService.getCache(config.getId());
            
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
            
            List<StorageInfoData> storageInfoList = new ArrayList<>();
            for (Map.Entry<String, DirectoryStats> entry : topDirectories) {
                StorageInfoData info = new StorageInfoData();
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
            
            List<LargestFileData> largestFilesList = new ArrayList<>();
            for (FileInfo fileInfo : topFiles) {
                LargestFileData largestFile = new LargestFileData();
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
            DiskSpaceData diskSpace = new DiskSpaceData();
            diskSpace.setTotalBytes(totalSize);
            diskSpace.setUsableBytes(0L);
            diskSpace.setScannedAt(LocalDateTime.now());
            cache.setDiskSpace(diskSpace);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            logger.info("Manual Storage scan completed - Config: {} - Duration: {}ms", 
                config.getName(), duration);
            
        } catch (Exception e) {
            logger.error("Error scanning storage for config {}: {}", config.getName(), e.getMessage());
        }
    }
    
    private void scanRecursive(Path rootPath, StorageConfig config, 
                               Map<String, DirectoryStats> directoryStatsMap, 
                               List<FileInfo> allFiles) throws IOException {
        try {
            String path = rootPath.toString();
            
            // Get directory statistics using du command
            getDirectoryStats(path, config, directoryStatsMap);
            
            // Get file information using find command
            getFileInfo(path, config, allFiles);
            
        } catch (Exception e) {
            logger.warn("Error scanning storage with Linux commands: {}", e.getMessage());
            throw new IOException("Failed to scan storage", e);
        }
    }
    
    private void getDirectoryStats(String path, StorageConfig config, 
                                   Map<String, DirectoryStats> directoryStatsMap) {
        try {
            // Check if this is individual storage (not system-wide with id -1)
            boolean isIndividualStorage = config.getId() == null || !config.getId().equals(-1L);
            
            if (isIndividualStorage) {
                // For individual storage, get total stats for the entire path once
                DirectoryStats totalStats = new DirectoryStats();
                
                // Get total size using du
                try {
                    String sizeCommand = String.format("du -sb '%s' 2>/dev/null", path.replace("'", "'\"'\"'"));
                    ProcessBuilder sizePb = new ProcessBuilder("bash", "-c", sizeCommand);
                    sizePb.redirectErrorStream(true);
                    Process sizeProcess = sizePb.start();
                    
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(sizeProcess.getInputStream()))) {
                        String line = reader.readLine();
                        if (line != null && !line.trim().isEmpty()) {
                            String[] parts = line.trim().split("\\s+");
                            if (parts.length >= 1) {
                                try {
                                    totalStats.totalSize = Long.parseLong(parts[0]);
                                } catch (NumberFormatException e) {
                                    logger.debug("Error parsing total size for {}: {}", path, line);
                                }
                            }
                        }
                    }
                    sizeProcess.waitFor();
                } catch (Exception e) {
                    logger.debug("Error getting total size for {}: {}", path, e.getMessage());
                }
                
                // Get total file count
                try {
                    String fileCountCommand = String.format("find '%s' -type f ! -path '*/.*' 2>/dev/null | wc -l", 
                        path.replace("'", "'\"'\"'"));
                    ProcessBuilder fileCountPb = new ProcessBuilder("bash", "-c", fileCountCommand);
                    fileCountPb.redirectErrorStream(true);
                    Process fileCountProcess = fileCountPb.start();
                    
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileCountProcess.getInputStream()))) {
                        String output = reader.readLine();
                        if (output != null && !output.trim().isEmpty()) {
                            try {
                                totalStats.fileCount = Integer.parseInt(output.trim());
                            } catch (NumberFormatException e) {
                                logger.debug("Error parsing total file count for {}: {}", path, output);
                            }
                        }
                    }
                    fileCountProcess.waitFor();
                } catch (Exception e) {
                    logger.debug("Error getting total file count for {}: {}", path, e.getMessage());
                }
                
                // Get directory count
                try {
                    String dirCountCommand = String.format("find '%s' -type d ! -path '*/.*' 2>/dev/null | wc -l", 
                        path.replace("'", "'\"'\"'"));
                    ProcessBuilder dirCountPb = new ProcessBuilder("bash", "-c", dirCountCommand);
                    dirCountPb.redirectErrorStream(true);
                    Process dirCountProcess = dirCountPb.start();
                    
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(dirCountProcess.getInputStream()))) {
                        String output = reader.readLine();
                        if (output != null && !output.trim().isEmpty()) {
                            try {
                                totalStats.directoryCount = Integer.parseInt(output.trim());
                            } catch (NumberFormatException e) {
                                logger.debug("Error parsing directory count for {}: {}", path, output);
                            }
                        }
                    }
                    dirCountProcess.waitFor();
                } catch (Exception e) {
                    logger.debug("Error getting directory count for {}: {}", path, e.getMessage());
                }
                
                // Store total stats for the main path
                directoryStatsMap.put(path, totalStats);
                
            } else {
                // System-wide scanning (original behavior)
                // Use du to get directory sizes
                String command = String.format("find '%s' -type d ! -path '*/.*' 2>/dev/null | head -n %d | xargs -I {} du -sb {} 2>/dev/null", 
                    path.replace("'", "'\"'\"'"), TOP_DIRECTORIES_LIMIT);
                
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            try {
                                long size = Long.parseLong(parts[0]);
                                String dirPath = parts[1];
                                
                                // Skip system directories for system-wide scanning
                                if (dirPath.startsWith("/proc/") || 
                                    dirPath.startsWith("/sys/") || 
                                    dirPath.startsWith("/dev/") ||
                                    dirPath.startsWith("/run/") ||
                                    dirPath.startsWith("/snap/") ||
                                    dirPath.startsWith("/var/lib/") ||
                                    dirPath.startsWith("/var/cache/") ||
                                    dirPath.startsWith("/var/log/") ||
                                    dirPath.startsWith("/tmp/") ||
                                    dirPath.startsWith("/lost+found")) {
                                    continue;
                                }
                                
                                DirectoryStats stats = directoryStatsMap.computeIfAbsent(dirPath, k -> new DirectoryStats());
                                stats.totalSize = size;
                                stats.directoryCount = 1;
                                
                            } catch (NumberFormatException e) {
                                logger.debug("Error parsing size from du output: {}", line);
                            }
                        }
                    }
                }
                
                process.waitFor();
                
                // Now count files in each directory
                for (String dirPath : directoryStatsMap.keySet()) {
                    try {
                        // Count files recursively in this directory
                        String fileCountCommand = String.format("find '%s' -type f ! -path '*/.*' 2>/dev/null | wc -l", 
                            dirPath.replace("'", "'\"'\"'"));
                        
                        ProcessBuilder fileCountPb = new ProcessBuilder("bash", "-c", fileCountCommand);
                        fileCountPb.redirectErrorStream(true);
                        Process fileCountProcess = fileCountPb.start();
                        
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileCountProcess.getInputStream()))) {
                            String output = reader.readLine();
                            if (output != null && !output.trim().isEmpty()) {
                                try {
                                    int fileCount = Integer.parseInt(output.trim());
                                    DirectoryStats stats = directoryStatsMap.get(dirPath);
                                    if (stats != null) {
                                        stats.fileCount = fileCount;
                                    }
                                } catch (NumberFormatException e) {
                                    logger.debug("Error parsing file count for {}: {}", dirPath, output);
                                }
                            }
                        }
                        
                        fileCountProcess.waitFor();
                        
                    } catch (Exception e) {
                        logger.debug("Error counting files in directory {}: {}", dirPath, e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error getting directory stats: {}", e.getMessage());
        }
    }
    
    private void getFileInfo(String path, StorageConfig config, List<FileInfo> allFiles) {
        try {
            // Use find to get file information for largest files
            String command = String.format("find '%s' -type f ! -path '*/.*' -printf '%%s %%p\\n' 2>/dev/null | sort -nr | head -n %d", 
                path.replace("'", "'\"'\"'"), TOP_FILES_LIMIT);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+", 2);
                    if (parts.length >= 2) {
                        try {
                            long size = Long.parseLong(parts[0]);
                            String filePath = parts[1];
                            
                            // Skip system directories for system-wide scanning
                            if (config.getId() != null && config.getId().equals(-1L)) {
                                if (filePath.startsWith("/proc/") || 
                                    filePath.startsWith("/sys/") || 
                                    filePath.startsWith("/dev/") ||
                                    filePath.startsWith("/run/") ||
                                    filePath.startsWith("/snap/") ||
                                    filePath.startsWith("/var/lib/") ||
                                    filePath.startsWith("/var/cache/") ||
                                    filePath.startsWith("/var/log/") ||
                                    filePath.startsWith("/tmp/") ||
                                    filePath.startsWith("/lost+found")) {
                                    continue;
                                }
                            }
                            
                            allFiles.add(new FileInfo(filePath, size));
                            
                        } catch (NumberFormatException e) {
                            logger.debug("Error parsing size from find output: {}", line);
                        }
                    }
                }
            }
            
            process.waitFor();
            
        } catch (Exception e) {
            logger.warn("Error getting file info: {}", e.getMessage());
        }
    }
    
    private void scanSingleDirectory(Path rootPath, StorageConfig config,
                                     Map<String, DirectoryStats> directoryStatsMap,
                                     List<FileInfo> allFiles) throws IOException {
        DirectoryStats stats = new DirectoryStats();
        String rootPathStr = rootPath.toString();
        
        try {
            // Use ls -la to list directory contents with sizes
            String command = String.format("ls -la '%s' 2>/dev/null", rootPathStr.replace("'", "'\"'\"'"));
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean firstLine = true; // Skip "total" line
                while ((line = reader.readLine()) != null) {
                    if (firstLine || line.trim().isEmpty()) {
                        firstLine = false;
                        continue;
                    }
                    
                    // Parse ls -la output: permissions links owner group size date time name
                    String[] parts = line.trim().split("\\s+", 9);
                    if (parts.length >= 9) {
                        String permissions = parts[0];
                        String name = parts[8];
                        
                        // Skip hidden files and . and ..
                        if (name.startsWith(".")) {
                            continue;
                        }
                        
                        try {
                            if (permissions.startsWith("d")) {
                                // Directory
                                stats.addDirectory();
                            } else if (permissions.startsWith("-")) {
                                // Regular file
                                long size = Long.parseLong(parts[4]);
                                stats.addFile(size);
                                allFiles.add(new FileInfo(rootPathStr + "/" + name, size));
                            }
                        } catch (NumberFormatException e) {
                            logger.debug("Error parsing size from ls output: {}", line);
                        }
                    }
                }
            }
            
            process.waitFor();
            
        } catch (Exception e) {
            logger.warn("Error scanning single directory with Linux command: {}", e.getMessage());
            throw new IOException("Failed to scan directory", e);
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
