package com.fsmonitor.app.service;

import com.fsmonitor.app.dto.LogMatch;
import com.fsmonitor.app.entity.LogConfig;
import com.fsmonitor.app.entity.LogMatchResult;
import com.fsmonitor.app.repository.LogConfigRepository;
import com.fsmonitor.app.repository.LogMatchResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final int MAX_MATCHES_IN_MEMORY = 1000; // Keep last 1000 matches

    @Autowired
    private LogConfigRepository logConfigRepository;

    @Autowired
    private LogMatchResultRepository logMatchResultRepository;

    // In-memory cache for recent matches
    private final List<LogMatchResult> recentMatches = new ArrayList<>();

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
            config.setLastMatchCount(matches.size());
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

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
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
                List<LogMatch> matches = searchLogsInternal(config);
                
                // Save matches to in-memory cache only
                synchronized (recentMatches) {
                    for (LogMatch match : matches) {
                        LogMatchResult result = new LogMatchResult();
                        result.setLogConfig(config);
                        result.setFileName(match.getFileName());
                        result.setKeyword(match.getKeyword());
                        result.setLineNumber(match.getLineNumber());
                        result.setMatchedLine(match.getMatchedLine());
                        result.setContextBefore(String.join("\n", match.getContextBefore()));
                        result.setContextAfter(String.join("\n", match.getContextAfter()));
                        result.setFoundAt(LocalDateTime.now());
                        recentMatches.add(0, result); // Add to beginning
                    }
                    
                    // Keep only last MAX_MATCHES_IN_MEMORY matches
                    if (recentMatches.size() > MAX_MATCHES_IN_MEMORY) {
                        recentMatches.subList(MAX_MATCHES_IN_MEMORY, recentMatches.size()).clear();
                    }
                }
                
                config.setLastCheck(LocalDateTime.now());
                config.setLastMatchCount(matches.size());
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

    public List<LogMatchResult> getRecentMatches(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        synchronized (recentMatches) {
            return recentMatches.stream()
                    .filter(m -> m.getFoundAt() != null && m.getFoundAt().isAfter(since))
                    .collect(Collectors.toList());
        }
    }

    public List<LogMatchResult> getMatchesForConfig(Long configId) {
        synchronized (recentMatches) {
            return recentMatches.stream()
                    .filter(m -> m.getLogConfig() != null && m.getLogConfig().getId().equals(configId))
                    .collect(Collectors.toList());
        }
    }
    
    public void clearMatches() {
        synchronized (recentMatches) {
            recentMatches.clear();
            logger.info("Cleared all matches from memory cache");
        }
    }

    public Map<String, Object> getLogStats() {
        List<LogConfig> configs = logConfigRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConfigs", configs.size());
        stats.put("activeConfigs", configs.stream().filter(LogConfig::isActive).count());
        stats.put("totalMatches", configs.stream().mapToInt(LogConfig::getLastMatchCount).sum());
        
        return stats;
    }
}
