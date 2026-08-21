package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.LogCacheService;
import com.fsmonitor.app.cache.LogCacheService.LogCache;
import com.fsmonitor.app.cache.MonitoringCacheManager.LogMatchResultData;
import com.fsmonitor.app.dto.LogMatch;
import com.fsmonitor.app.entity.LogConfig;
import com.fsmonitor.app.repository.LogConfigRepository;
import com.fsmonitor.app.util.ShellCommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LogMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(LogMonitoringService.class);

    @Autowired
    private LogConfigRepository logConfigRepository;

    @Autowired
    private LogCacheService logCacheService;

    public List<LogMatch> searchLogs(Long configId) {
        List<LogMatch> matches = new ArrayList<>();
        LogConfig config = logConfigRepository.findById(configId).orElse(null);
        
        if (config == null) {
            return matches;
        }

        try {
            // Use the new Linux-based search method
            matches = searchLogsInternal(config);

            config.setLastCheck(LocalDateTime.now());
            logConfigRepository.save(config);

        } catch (Exception e) {
            logger.error("Error searching logs for config {}: {}", configId, e.getMessage());
        }

        return matches;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000) // Run every minute, wait 1 min after startup
    public void monitorLogs() {
        long startTime = System.currentTimeMillis();
        List<LogConfig> activeConfigs = logConfigRepository.findByActiveTrue();
        
        // MEMORY CLEANUP: Clear cache entries for deleted log configs
        cleanupDeletedLogConfigs(activeConfigs);
        
        boolean anyScanned = false;
        
        for (LogConfig config : activeConfigs) {
            try {
                // Check if it's time to check this config
                if (config.getLastCheck() != null) {
                    LocalDateTime nextCheck = config.getLastCheck().plusMinutes(config.getCheckIntervalMinutes());
                    if (LocalDateTime.now().isBefore(nextCheck)) {
                        continue;
                    }
                }
                
                // Search logs using Linux grep
                List<LogMatch> matches = searchLogsInternal(config);
                
                // Clear this config's cache before adding new matches
                // This prevents cache growth while keeping other configs' cache intact
                LogCache logCache = logCacheService.getCache(config.getId());
                logCache.clearMatches();
                
                // Add new matches to cache
                for (LogMatch match : matches) {
                    LogMatchResultData matchData = new LogMatchResultData();
                    matchData.setLogConfigId(config.getId());
                    matchData.setFileName(match.getFileName());
                    matchData.setKeyword(match.getKeyword());
                    matchData.setLineNumber(match.getLineNumber());
                    matchData.setMatchedLine(match.getMatchedLine());
                    matchData.setContextBefore(String.join("\n", match.getContextBefore()));
                    matchData.setContextAfter(String.join("\n", match.getContextAfter()));
                    matchData.setFoundAt(LocalDateTime.now());
                    logCache.addMatch(matchData);
                }
                
                config.setLastCheck(LocalDateTime.now());
                logConfigRepository.save(config);
                
                anyScanned = true;
                
            } catch (Exception e) {
                logger.error("Error monitoring logs for config {}: {}", config.getName(), e.getMessage(), e);
            }
        }
        
        if (anyScanned) {
            long endTime = System.currentTimeMillis();
            logger.info("Log Control scan completed - Duration: {}ms", (endTime - startTime));
        }
    }

    private List<LogMatch> searchLogsInternal(LogConfig config) {
        List<LogMatch> matches = new ArrayList<>();
        
        logger.debug("Searching logs for config: {} (path: {}, keywords: {})", 
            config.getName(), config.getPath(), config.getKeywords());

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
                matches.addAll(searchDirectoryWithGrep(path, fileTypes, keywords, config.isRecursive()));
            } else if (Files.isRegularFile(path)) {
                matches.addAll(searchFileWithGrep(path, keywords));
            } else {
                logger.warn("Path does not exist or is not accessible: {}", path);
            }

        } catch (Exception e) {
            logger.error("Error searching logs for config {}: {}", config.getId(), e.getMessage(), e);
        }
        
        logger.debug("Log search completed for config {}, found {} matches", config.getName(), matches.size());
        return matches;
    }
    
    /**
     * Search directory using Linux grep commands for memory efficiency
     */
    private List<LogMatch> searchDirectoryWithGrep(Path directory, List<String> fileTypes, List<String> keywords, boolean recursive) throws IOException {
        List<LogMatch> matches = new ArrayList<>();
        
        // Build file type pattern for grep
        String fileTypePattern = buildFileTypePattern(fileTypes);
        
        // Build grep command
        String grepCommand = buildGrepCommand(directory.toString(), fileTypePattern, keywords, recursive);
        
        // Execute grep and parse results
        List<String> output = executeGrepCommand(grepCommand);
        
        // Parse grep output with context into LogMatch objects
        matches.addAll(parseGrepOutputWithContext(output, keywords));
        
        return matches;
    }
    
    /**
     * Search single file using Linux grep
     */
    private List<LogMatch> searchFileWithGrep(Path file, List<String> keywords) throws IOException {
        List<LogMatch> matches = new ArrayList<>();
        
        // Build grep command for single file
        String grepCommand = buildGrepCommandForFile(file.toString(), keywords);
        
        // Execute grep and parse results
        List<String> output = executeGrepCommand(grepCommand);
        
        // Parse grep output with context into LogMatch objects
        matches.addAll(parseGrepOutputWithContext(output, keywords));
        
        return matches;
    }
    
    /**
     * Build file type pattern for grep (robust for different log file types)
     */
    private String buildFileTypePattern(List<String> fileTypes) {
        if (fileTypes.isEmpty()) {
            return "*.log *.txt *.out *.err";
        }
        
        return String.join(" ", fileTypes.stream()
            .map(ft -> "*" + ft)
            .collect(Collectors.toList()));
    }
    
    /**
     * Build grep command for directory search
     */
    private String buildGrepCommand(String path, String fileTypePattern, List<String> keywords, boolean recursive) {
        StringBuilder command = new StringBuilder();
        
        // Find command to locate files
        command.append("find ").append(ShellCommandUtil.escapeForBash(path));
        
        if (recursive) {
            command.append(" -type f");
        } else {
            command.append(" -maxdepth 1 -type f");
        }
        
        // Add file type filter - use regex to match only files ending with extension
        command.append(" \\( ");
        String[] types = fileTypePattern.isBlank() ? new String[0] : fileTypePattern.split(" +");
        for (int i = 0; i < types.length; i++) {
            if (i > 0) command.append(" -o ");
            // Convert *.log to regex pattern that matches files ending with .log
            String extension = types[i].replace("*", "").replace(".", "\\.");
            command.append("-regex ").append(ShellCommandUtil.escapeForBash(".*" + extension)).append(" ");
        }
        command.append(" \\) ");
        
        // Execute grep on found files using xargs (more reliable with Java ProcessBuilder)
        command.append(" | xargs -r grep -H -n -C 10 -i");
        
        // Add keywords (safely escaped)
        for (String keyword : keywords) {
            command.append(" -e ").append(ShellCommandUtil.escapeForBash(keyword)).append(" ");
        }
        
        command.append("2>/dev/null");
        
        return command.toString();
    }
    
    /**
     * Build grep command for single file
     */
    private String buildGrepCommandForFile(String filePath, List<String> keywords) {
        StringBuilder command = new StringBuilder();
        command.append("grep -H -n -C 10 -i");
        
        // Add keywords (safely escaped)
        for (String keyword : keywords) {
            command.append(" -e ").append(ShellCommandUtil.escapeForBash(keyword)).append(" ");
        }
        
        command.append(" ").append(ShellCommandUtil.escapeForBash(filePath)).append(" 2>/dev/null");
        
        return command.toString();
    }
    
    /**
     * Execute grep command and return output lines
     */
    private List<String> executeGrepCommand(String command) throws IOException {
        ShellCommandUtil.CommandResult result = ShellCommandUtil.execute(command, 60);
        int exitCode = result.getExitCode();
        
        // grep returns 0 when matches found, 1 when no matches found, 2+ for errors
        // We accept both 0 and 1 as valid (1 can mean no matches OR matches with warnings)
        if (exitCode > 1) {
            // Check if this is a common fallback scenario (non-critical directory access)
            if (command.contains("/var/log") || command.contains("/proc") || command.contains("/sys")) {
                logger.debug("Grep command failed for system directory (exit code {}): {}", exitCode, command);
            } else {
                logger.warn("Grep command failed with exit code {}: {}", exitCode, command);
            }
        }
        logger.debug("Grep command completed with exit code {}: {} lines returned", exitCode, result.getOutput().size());
        
        return result.getOutput();
    }
    
    /**
     * Parse grep output with context (-C 10) into LogMatch objects
     * Grep -C format:
     *   file.log-90-context line before
     *   file.log:100:MATCH LINE
     *   file.log-101-context line after
     *   --
     */
    private List<LogMatch> parseGrepOutputWithContext(List<String> grepOutput, List<String> keywords) {
        List<LogMatch> matches = new ArrayList<>();
        
        List<String> contextBefore = new ArrayList<>();
        List<String> contextAfter = new ArrayList<>();
        LogMatch currentMatch = null;
        boolean collectingAfter = false;
        
        for (String line : grepOutput) {
            if (line.trim().isEmpty()) continue;
            
            // Context separator (--) - indicates end of a match block
            if (line.startsWith("--")) {
                // Save current match if exists
                if (currentMatch != null) {
                    currentMatch.setContextAfter(new ArrayList<>(contextAfter));
                    matches.add(currentMatch);
                    currentMatch = null;
                }
                // Reset for next block
                contextBefore.clear();
                contextAfter.clear();
                collectingAfter = false;
                continue;
            }
            
            // Check if this is a match line (filename:linenumber:content)
            // Match lines use : separator
            // Must distinguish from context lines that contain : in timestamps
            if (line.contains(":")) {
                String[] parts = line.split(":", 3);
                if (parts.length >= 3) {
                    try {
                        // Verify second part is a line number (not a timestamp like "20" or "07")
                        int lineNum = Integer.parseInt(parts[1]);
                        
                        // Context lines look like: /var/log/file.log-6539-2026-04-07 07:20:46 ...
                        // Match lines look like: /var/log/file.log:6539:content
                        // Check if first part contains date pattern (YYYY-MM-DD or ends with space+number)
                        String firstPart = parts[0];
                        boolean isContextLine = firstPart.matches(".*-\\d{4}-\\d{2}-\\d{2}.*") || // Contains date
                                                firstPart.matches(".*\\s+\\d+$") || // Ends with space+number (hour)
                                                lineNum < 100; // Line numbers in timestamps are usually < 100 (hours/minutes)
                        
                        if (!isContextLine && firstPart.contains("/")) {
                            // This is a real match line
                            // Save previous match if exists
                            if (currentMatch != null) {
                                currentMatch.setContextAfter(new ArrayList<>(contextAfter));
                                matches.add(currentMatch);
                                // Don't clear contextBefore - next match in same block shares it
                                contextAfter.clear();
                            }
                            
                            // Parse new match line
                            currentMatch = parseGrepOutput(line, keywords);
                            // Set context before from accumulated lines (shared by all matches in block)
                            currentMatch.setContextBefore(new ArrayList<>(contextBefore));
                            contextAfter.clear();
                            collectingAfter = true; // Now collect context after
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        // Not a valid match line, treat as context
                    }
                }
            }
            
            // Context line (filename-linenumber-content)
            // Context lines use - separator
            if (line.contains("-")) {
                String[] parts = line.split("-", 3);
                if (parts.length >= 3) {
                    String content = parts[2];
                    if (collectingAfter && currentMatch != null) {
                        contextAfter.add(content);
                    } else {
                        contextBefore.add(content);
                    }
                }
            }
        }
        
        // Save the last match
        if (currentMatch != null) {
            currentMatch.setContextAfter(new ArrayList<>(contextAfter));
            matches.add(currentMatch);
        }
        
        return matches;
    }
    
    /**
     * Parse grep output into LogMatch object
     * Grep format: filename:linenumber:matched_line
     */
    private LogMatch parseGrepOutput(String grepLine, List<String> keywords) {
        if (grepLine == null || grepLine.trim().isEmpty()) {
            return null;
        }
        
        // Parse filename:linenumber:content
        String[] parts = grepLine.split(":", 3);
        if (parts.length < 3) {
            return null;
        }
        
        try {
            String fileName = parts[0];
            int lineNumber = Integer.parseInt(parts[1]);
            String matchedLine = parts[2];
            
            // Find which keyword matched
            String matchedKeyword = null;
            for (String keyword : keywords) {
                if (matchedLine.toLowerCase().contains(keyword.toLowerCase())) {
                    matchedKeyword = keyword;
                    break;
                }
            }
            
            if (matchedKeyword == null) {
                matchedKeyword = keywords.get(0); // fallback
            }
            
            LogMatch match = new LogMatch();
            match.setFileName(fileName);
            match.setKeyword(matchedKeyword);
            match.setLineNumber(lineNumber);
            match.setMatchedLine(matchedLine);
            
            // Get context lines (simplified - could be enhanced with more grep options)
            List<String> contextBefore = new ArrayList<>();
            List<String> contextAfter = new ArrayList<>();
            match.setContextBefore(contextBefore);
            match.setContextAfter(contextAfter);
            
            return match;
            
        } catch (NumberFormatException e) {
            logger.debug("Could not parse grep output line: {}", grepLine);
            return null;
        }
    }

    public List<LogMatchResultData> getRecentMatches(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<LogMatchResultData> allMatches = new ArrayList<>();
        
        List<LogConfig> configs = logConfigRepository.findAll();
        for (LogConfig config : configs) {
            LogCache logCache = logCacheService.getCache(config.getId());
            allMatches.addAll(logCache.getMatches().stream()
                    .filter(m -> m.getFoundAt() != null && m.getFoundAt().isAfter(since))
                    .collect(Collectors.toList()));
        }
        return allMatches;
    }

    public List<LogMatchResultData> getMatchesForConfig(Long configId) {
        LogCache logCache = logCacheService.getCache(configId);
        return new ArrayList<>(logCache.getMatches());
    }
    
    public void clearMatches() {
        List<LogConfig> configs = logConfigRepository.findAll();
        for (LogConfig config : configs) {
            logCacheService.clearCache(config.getId());
        }
        logger.debug("Cleared all matches from cache");
    }

    public void clearMatchesForConfig(Long configId) {
        logCacheService.clearCache(configId);
        logger.debug("Cleared matches for config {} from cache", configId);
    }

    public Map<String, Object> getLogStats() {
        List<LogConfig> configs = logConfigRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConfigs", configs.size());
        stats.put("activeConfigs", configs.stream().filter(LogConfig::isActive).count());
        // Total matches now come from cache, not from config
        int totalMatches = 0;
        for (LogConfig config : configs) {
            LogCache cache = logCacheService.getCache(config.getId());
            totalMatches += cache.getMatches().size();
        }
        stats.put("totalMatches", totalMatches);
        
        return stats;
    }
    
    
    /**
     * MEMORY CLEANUP: Remove cache entries for deleted log configs
     * This prevents memory leaks when log configs are deleted from database
     */
    private void cleanupDeletedLogConfigs(List<LogConfig> activeConfigs) {
        // Get all active log config IDs
        Set<Long> activeConfigIds = activeConfigs.stream()
            .map(LogConfig::getId)
            .collect(Collectors.toSet());
        
        // Clear cache entries for deleted log configs
        Set<Long> cachedConfigIds = logCacheService.getAllCachedIds();
        for (Long cachedId : cachedConfigIds) {
            if (!activeConfigIds.contains(cachedId)) {
                logCacheService.clearCache(cachedId);
                logger.debug("Cleared cache for deleted log config ID: {}", cachedId);
            }
        }
    }
}
