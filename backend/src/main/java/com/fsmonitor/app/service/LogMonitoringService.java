package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.LogCacheService;
import com.fsmonitor.app.cache.LogCacheService.LogCache;
import com.fsmonitor.app.cache.model.LogMatchResultData;
import com.fsmonitor.app.dto.LogMatch;
import com.fsmonitor.app.entity.LogConfig;
import com.fsmonitor.app.repository.LogConfigRepository;
import com.fsmonitor.app.util.ShellCommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LogMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(LogMonitoringService.class);

    @Value("${fsmonitor.command-timeout-seconds:120}")
    private int commandTimeout;

    private final LogConfigRepository logConfigRepository;
    private final LogCacheService logCacheService;

    public LogMonitoringService(LogConfigRepository logConfigRepository,
                                LogCacheService logCacheService) {
        this.logConfigRepository = logConfigRepository;
        this.logCacheService = logCacheService;
    }

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

    private static final int CONTEXT_LINES = 10;
    private static final int MAX_MATCHES = 500;
    private static final int MAX_CONTEXT_MATCHES_PER_FILE = 100;
    private static final int FILE_BATCH_SIZE = 200;

    private List<LogMatch> searchLogsInternal(LogConfig config) {
        logger.debug("Searching logs for config: {} (path: {}, keywords: {})",
            config.getName(), config.getPath(), config.getKeywords());

        List<LogMatch> matches = new ArrayList<>();
        try {
            Path path = Paths.get(config.getPath());
            List<String> keywords = splitCsv(config.getKeywords());
            if (keywords.isEmpty()) {
                return matches;
            }

            List<Path> files = collectCandidateFiles(path, splitCsv(config.getFileTypes()), config.isRecursive());
            if (files.isEmpty()) {
                return matches;
            }

            for (int i = 0; i < files.size() && matches.size() < MAX_MATCHES; i += FILE_BATCH_SIZE) {
                List<Path> batch = files.subList(i, Math.min(i + FILE_BATCH_SIZE, files.size()));
                matches.addAll(grepBatch(batch, keywords));
            }
            if (matches.size() > MAX_MATCHES) {
                matches = new ArrayList<>(matches.subList(0, MAX_MATCHES));
            }

            attachContext(matches);
        } catch (Exception e) {
            logger.error("Error searching logs for config {}: {}", config.getId(), e.getMessage(), e);
        }

        logger.debug("Log search completed for config {}, found {} matches", config.getName(), matches.size());
        return matches;
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<Path> collectCandidateFiles(Path path, List<String> fileTypes, boolean recursive) throws IOException {
        if (Files.isRegularFile(path)) {
            return List.of(path);
        }
        if (!Files.isDirectory(path)) {
            logger.warn("Path does not exist or is not accessible: {}", path);
            return List.of();
        }
        try (Stream<Path> stream = recursive ? Files.walk(path) : Files.list(path)) {
            return stream
                    .filter(f -> Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS))
                    .filter(f -> !Files.isSymbolicLink(f))
                    .filter(f -> matchesFileType(f.getFileName().toString(), fileTypes))
                    .collect(Collectors.toList());
        }
    }

    static boolean matchesFileType(String fileName, List<String> fileTypes) {
        if (fileTypes == null || fileTypes.isEmpty()) {
            return true;
        }
        String lower = fileName.toLowerCase();
        return fileTypes.stream().map(LogMonitoringService::normalizeExt).anyMatch(lower::endsWith);
    }

    private static String normalizeExt(String type) {
        String t = type.trim().toLowerCase();
        if (t.equals("*")) {
            return "";
        }
        int dot = t.lastIndexOf('.');
        return dot >= 0 ? t.substring(dot) : "." + t;
    }

    /**
     * Runs grep in argv form (no shell): match lines only, NUL-separated filenames.
     * Output format is deterministically {@code file\0lineno:content} so parsing is exact.
     * Context lines are fetched afterwards via {@link #attachContext}.
     */
    private List<LogMatch> grepBatch(List<Path> files, List<String> keywords) {
        List<String> argv = new ArrayList<>();
        argv.add("grep");
        argv.add("-Hn");   // filename + line number
        argv.add("-i");    // case-insensitive
        argv.add("-F");    // keywords are literal strings, not regexes
        argv.add("-I");    // skip binary files
        argv.add("-Z");    // NUL after filename -> unambiguous parsing
        for (String kw : keywords) {
            argv.add("-e");
            argv.add(kw);
        }
        argv.add("--");
        for (Path f : files) {
            argv.add(f.toAbsolutePath().toString());
        }

        ShellCommandUtil.CommandResult result;
        try {
            result = ShellCommandUtil.execute(argv, commandTimeout);
        } catch (Exception e) {
            logger.error("grep execution failed", e);
            return List.of();
        }
        // exit 0 = matches found, 1 = no matches (fine), >1 = error
        if (result.exitCode() > 1) {
            logger.warn("grep exited with code {} (some files may be unreadable)", result.exitCode());
        }

        List<LogMatch> matches = new ArrayList<>();
        for (String line : result.output()) {
            LogMatch m = parseGrepLine(line, keywords);
            if (m != null) {
                matches.add(m);
            }
        }
        return matches;
    }

    static LogMatch parseGrepLine(String line, List<String> keywords) {
        int nul = line.indexOf('\0');
        if (nul <= 0) {
            return null;
        }
        int colon = line.indexOf(':', nul + 1);
        if (colon < 0) {
            return null;
        }
        int lineNumber;
        try {
            lineNumber = Integer.parseInt(line.substring(nul + 1, colon));
        } catch (NumberFormatException e) {
            return null;
        }

        String content = line.substring(colon + 1);
        LogMatch match = new LogMatch();
        match.setFileName(line.substring(0, nul));
        match.setLineNumber(lineNumber);
        match.setMatchedLine(content);
        match.setTimestamp(extractTimestamp(content));
        match.setKeyword(detectKeyword(content, keywords));
        match.setContextBefore(List.of());
        match.setContextAfter(List.of());
        return match;
    }

    private static String detectKeyword(String content, List<String> keywords) {
        String lower = content.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) {
                return kw;
            }
        }
        return keywords.get(0);
    }

    /** Fills contextBefore/contextAfter by streaming each matched file once. */
    private void attachContext(List<LogMatch> matches) {
        Map<String, List<LogMatch>> byFile = matches.stream()
                .collect(Collectors.groupingBy(LogMatch::getFileName));
        byFile.forEach((file, fileMatches) -> {
            try {
                fillContext(Paths.get(file), fileMatches);
            } catch (IOException e) {
                logger.debug("Could not read context for {}: {}", file, e.getMessage());
            }
        });
    }

    static void fillContext(Path file, List<LogMatch> fileMatches) throws IOException {
        List<LogMatch> limited = fileMatches.size() > MAX_CONTEXT_MATCHES_PER_FILE
                ? fileMatches.subList(0, MAX_CONTEXT_MATCHES_PER_FILE) : fileMatches;

        Set<Integer> needed = new HashSet<>();
        int maxLine = 0;
        for (LogMatch m : limited) {
            for (int i = Math.max(1, m.getLineNumber() - CONTEXT_LINES); i <= m.getLineNumber() + CONTEXT_LINES; i++) {
                needed.add(i);
            }
            maxLine = Math.max(maxLine, m.getLineNumber() + CONTEXT_LINES);
        }

        Map<Integer, String> lines = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(file),
                StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE)))) {
            String line;
            int n = 0;
            while ((line = reader.readLine()) != null) {
                n++;
                if (n > maxLine) {
                    break;
                }
                if (needed.contains(n)) {
                    lines.put(n, line);
                }
            }
        }

        for (LogMatch m : limited) {
            List<String> before = new ArrayList<>();
            for (int i = Math.max(1, m.getLineNumber() - CONTEXT_LINES); i < m.getLineNumber(); i++) {
                String l = lines.get(i);
                if (l != null) {
                    before.add(l);
                }
            }
            List<String> after = new ArrayList<>();
            for (int i = m.getLineNumber() + 1; i <= m.getLineNumber() + CONTEXT_LINES; i++) {
                String l = lines.get(i);
                if (l != null) {
                    after.add(l);
                }
            }
            m.setContextBefore(before);
            m.setContextAfter(after);
        }
    }

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}");

    static String extractTimestamp(String line) {
        if (line == null) {
            return null;
        }
        Matcher matcher = TIMESTAMP_PATTERN.matcher(line);
        return matcher.find() ? matcher.group() : null;
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
