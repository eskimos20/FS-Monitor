package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.MonitoringCacheManager;
import com.fsmonitor.app.dto.LogMatch;
import com.fsmonitor.app.entity.LogConfig;
import com.fsmonitor.app.repository.LogConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LogMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(LogMonitoringService.class);
    private static final int CONTEXT_LINES = 10;

    @Autowired
    private LogConfigRepository logConfigRepository;

    @Autowired
    private MonitoringCacheManager cacheManager;

    public List<LogMatch> searchLogs(Long configId) {
        Optional<LogConfig> configOpt = logConfigRepository.findById(configId);
        if (!configOpt.isPresent()) {
            return Collections.emptyList();
        }

        LogConfig config = configOpt.get();
        List<LogMatch> matches = new ArrayList<>();

        try {
            Path path = Paths.get(config.getPath());
            List<String> keywords = Arrays.asList(config.getKeywords().split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            List<String> fileTypes = Arrays.asList(config.getFileTypes().split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (Files.isDirectory(path)) {
                matches.addAll(searchDirectory(path, fileTypes, keywords, config.isRecursive()));
            } else if (Files.isRegularFile(path)) {
                matches.addAll(searchFile(path, keywords));
            }

            config.setLastCheck(LocalDateTime.now());
            logConfigRepository.save(config);

        } catch (Exception e) {
            logger.error("Error searching logs for config {}: {}", configId, e.getMessage());
        }

        return matches;
    }

    private List<LogMatch> searchDirectory(Path directory, List<String> fileTypes, List<String> keywords, boolean recursive) {
        List<LogMatch> matches = new ArrayList<>();
        List<Path> logFiles = new ArrayList<>();

        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        if (attrs.isRegularFile() && Files.isReadable(file) && matchesFileType(file, fileTypes)) {
                            logFiles.add(file);
                        }
                    } catch (Exception e) {
                        logger.debug("Skipping file {}: {}", file, e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.debug("Cannot access {}: {}", file, exc.getMessage());
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!Files.isReadable(dir)) {
                        logger.debug("Skipping unreadable directory: {}", dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    // If not recursive, skip subdirectories
                    if (!recursive && !dir.equals(directory)) {
                        logger.debug("Skipping subdirectory (non-recursive): {}", dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            for (Path file : logFiles) {
                try {
                    matches.addAll(searchFile(file, keywords));
                } catch (Exception e) {
                    logger.debug("Error searching file {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.warn("Error walking directory {}: {}", directory, e.getMessage());
        }

        return matches;
    }

    private boolean matchesFileType(Path file, List<String> fileTypes) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileTypes.stream().anyMatch(type -> {
            String ext = type.startsWith(".") ? type : "." + type;
            return fileName.endsWith(ext.toLowerCase());
        });
    }

    private List<LogMatch> searchFile(Path file, List<String> keywords) {
        List<LogMatch> matches = new ArrayList<>();

        try {
            List<String> allLines = Files.readAllLines(file);

            for (int i = 0; i < allLines.size(); i++) {
                String line = allLines.get(i);
                
                for (String keyword : keywords) {
                    if (line.toLowerCase().contains(keyword.toLowerCase())) {
                        LogMatch match = new LogMatch();
                        match.setFileName(file.toString());
                        match.setKeyword(keyword);
                        match.setLineNumber(i + 1);
                        match.setMatchedLine(line);
                        
                        int startBefore = Math.max(0, i - CONTEXT_LINES);
                        int endAfter = Math.min(allLines.size(), i + CONTEXT_LINES + 1);
                        
                        match.setContextBefore(allLines.subList(startBefore, i));
                        match.setContextAfter(allLines.subList(i + 1, endAfter));
                        match.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        
                        matches.add(match);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error reading file {}: {}", file, e.getMessage());
        }

        return matches;
    }

    @Scheduled(fixedRate = 60000, initialDelay = 60000) // Run every minute, wait 1 min after startup
    public void monitorLogs() {
        List<LogConfig> activeConfigs = logConfigRepository.findByActiveTrue();
        
        for (LogConfig config : activeConfigs) {
            try {
                // Check if it's time to check this config
                if (config.getLastCheck() != null) {
                    LocalDateTime nextCheck = config.getLastCheck().plusMinutes(config.getCheckIntervalMinutes());
                    if (LocalDateTime.now().isBefore(nextCheck)) {
                        continue; // Not time yet
                    }
                }
                
                logger.debug("Checking logs for config: {} (path: {}, fileTypes: {}, keywords: {}, recursive: {})", 
                    config.getName(), config.getPath(), config.getFileTypes(), config.getKeywords(), config.isRecursive());
                
                // Clear old matches for this config before adding new ones
                cacheManager.clearLogCache(config.getId());
                
                List<LogMatch> matches = searchLogsInternal(config);
                
                // Save matches to cache
                MonitoringCacheManager.LogCache logCache = cacheManager.getLogCache(config.getId());
                for (LogMatch match : matches) {
                    MonitoringCacheManager.LogMatchResultData result = new MonitoringCacheManager.LogMatchResultData();
                    result.setLogConfigId(config.getId());
                    result.setFileName(match.getFileName());
                    result.setKeyword(match.getKeyword());
                    result.setLineNumber(match.getLineNumber());
                    result.setMatchedLine(match.getMatchedLine());
                    result.setContextBefore(String.join("\n", match.getContextBefore()));
                    result.setContextAfter(String.join("\n", match.getContextAfter()));
                    result.setFoundAt(LocalDateTime.now());
                    logCache.addMatch(result);
                }
                
                config.setLastCheck(LocalDateTime.now());
                logConfigRepository.save(config);
            } catch (Exception e) {
                logger.error("Error monitoring logs for config {}: {}", config.getName(), e.getMessage(), e);
            }
        }
    }

    private List<LogMatch> searchLogsInternal(LogConfig config) {
        List<LogMatch> matches = new ArrayList<>();

        try {
            Path path = Paths.get(config.getPath());
            List<String> keywords = Arrays.asList(config.getKeywords().split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            List<String> fileTypes = Arrays.asList(config.getFileTypes().split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (Files.isDirectory(path)) {
                matches.addAll(searchDirectory(path, fileTypes, keywords, config.isRecursive()));
            } else if (Files.isRegularFile(path)) {
                matches.addAll(searchFile(path, keywords));
            } else {
                logger.warn("Path does not exist or is not accessible: {}", path);
            }

        } catch (Exception e) {
            logger.error("Error searching logs for config {}: {}", config.getId(), e.getMessage(), e);
        }

        return matches;
    }

    public List<MonitoringCacheManager.LogMatchResultData> getRecentMatches(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<MonitoringCacheManager.LogMatchResultData> allMatches = new ArrayList<>();
        
        List<LogConfig> configs = logConfigRepository.findAll();
        for (LogConfig config : configs) {
            MonitoringCacheManager.LogCache logCache = cacheManager.getLogCache(config.getId());
            allMatches.addAll(logCache.getMatches().stream()
                    .filter(m -> m.getFoundAt() != null && m.getFoundAt().isAfter(since))
                    .collect(Collectors.toList()));
        }
        return allMatches;
    }

    public List<MonitoringCacheManager.LogMatchResultData> getMatchesForConfig(Long configId) {
        MonitoringCacheManager.LogCache logCache = cacheManager.getLogCache(configId);
        return new ArrayList<>(logCache.getMatches());
    }
    
    public void clearMatches() {
        List<LogConfig> configs = logConfigRepository.findAll();
        for (LogConfig config : configs) {
            cacheManager.clearLogCache(config.getId());
        }
        logger.info("Cleared all matches from cache");
    }

    public void clearMatchesForConfig(Long configId) {
        cacheManager.clearLogCache(configId);
        logger.info("Cleared matches for config {} from cache", configId);
    }

    public Map<String, Object> getLogStats() {
        List<LogConfig> configs = logConfigRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConfigs", configs.size());
        stats.put("activeConfigs", configs.stream().filter(LogConfig::isActive).count());
        // Total matches now come from cache, not from config
        int totalMatches = 0;
        for (LogConfig config : configs) {
            MonitoringCacheManager.LogCache cache = cacheManager.getLogCache(config.getId());
            totalMatches += cache.getMatches().size();
        }
        stats.put("totalMatches", totalMatches);
        
        return stats;
    }
}
