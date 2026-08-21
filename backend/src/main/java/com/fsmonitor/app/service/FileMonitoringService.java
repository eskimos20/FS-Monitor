package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.IntegrationCacheService;
import com.fsmonitor.app.cache.IntegrationCacheService.IntegrationCache;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.entity.FileType;
import com.fsmonitor.app.repository.IntegrationRepository;
import com.fsmonitor.app.util.ShellCommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(FileMonitoringService.class);

    // Track when the scheduler actually runs
    private LocalDateTime lastSchedulerRun;
    private LocalDateTime nextSchedulerRun;

    @jakarta.annotation.PostConstruct
    public void initializeIntegrationMonitoring() {
        logger.info("Initializing integration monitoring on startup");
        // Note: Don't set lastCheckedAt to avoid false timestamps
        // Cache will be populated when actual checks occur
    }

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private IntegrationCacheService integrationCacheService;

    @Scheduled(fixedDelay = 60000, initialDelay = 60000) // Run every minute, wait 1 min after startup
    public void monitorIntegrations() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        lastSchedulerRun = now;
        
        logger.debug("Starting file monitoring check...");
        
        List<Integration> activeIntegrations = integrationRepository.findActiveMonitoringIntegrations();
        
        // MEMORY CLEANUP: Clear cache entries for deleted integrations
        cleanupDeletedIntegrations(activeIntegrations);
        
        for (Integration integration : activeIntegrations) {
            try {
                if (shouldCheckIntegration(integration, now)) {
                    long startTime = System.currentTimeMillis();
                    checkIntegration(integration, now);
                    long endTime = System.currentTimeMillis();
                    logger.info("Integration scan completed - Name: {} - Duration: {}ms", integration.getName(), (endTime - startTime));
                }
            } catch (Exception e) {
                logger.error("Error monitoring integration: " + integration.getName(), e);
            }
        }
        
        // Set next run AFTER all integrations have been checked
        // This prevents race conditions where frontend sees stale nextRun time
        nextSchedulerRun = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusSeconds(60);
        
        logger.debug("File monitoring check completed for {} integrations", activeIntegrations.size());
    }

    public LocalDateTime getLastSchedulerRun() { return lastSchedulerRun; }
    public LocalDateTime getNextSchedulerRun() { return nextSchedulerRun; }

    private boolean shouldCheckIntegration(Integration integration, LocalDateTime now) {
        // Check schedule constraints first
        if (Boolean.TRUE.equals(integration.getScheduleEnabled())) {
            if (!isWithinSchedule(integration.getActiveDays(), 
                                   integration.getActiveStartHour(), 
                                   integration.getActiveEndHour(), 
                                   now)) {
                logger.debug("Integration {} is outside scheduled time, skipping", integration.getName());
                return false;
            }
        }
        
        IntegrationCache cache = integrationCacheService.getCache(integration.getId());
        LocalDateTime lastCheck = cache.getLastCheckedAt();
        if (lastCheck == null) {
            // First check - run immediately
            return true;
        }
        
        LocalDateTime nextCheck = lastCheck.truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
            .plusMinutes(integration.getCheckIntervalMinutes().longValue());
        return !now.isBefore(nextCheck);
    }

    private boolean isWithinSchedule(String activeDays, Integer startHour, Integer endHour, LocalDateTime now) {
        // Check day of week
        if (activeDays != null && !activeDays.isEmpty()) {
            String currentDay = now.getDayOfWeek().name().substring(0, 3).toUpperCase();
            if (!activeDays.toUpperCase().contains(currentDay)) {
                return false;
            }
        }
        
        // Check time of day
        if (startHour != null && endHour != null) {
            int currentHour = now.getHour();
            if (currentHour < startHour || currentHour >= endHour) {
                return false;
            }
        }
        
        return true;
    }

    private void checkIntegration(Integration integration, LocalDateTime now) {
        logger.debug("Checking integration: {}", integration.getName());
        
        String path = integration.getPath();
        File directory = new File(path);
        
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warn("Directory does not exist for integration {}: {}", integration.getName(), path);
            return;
        }

        IntegrationCache cache = integrationCacheService.getCache(integration.getId());
        
        // Mark that the scheduler checked this integration at this cycle's timestamp
        cache.setLastCheckedAt(now);

        // Find the latest file recursively
        File latestFile;
        if (Boolean.TRUE.equals(integration.getMonitorAllFiles())) {
            latestFile = findLatestFileAllTypes(directory);
        } else {
            latestFile = findLatestFile(directory, integration.getMonitoredFileTypes());
        }
        
        if (latestFile != null) {
            LocalDateTime fileModifiedTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(latestFile.lastModified()), 
                ZoneId.systemDefault()
            );
            
            cache.setLastFileFound(fileModifiedTime);
            cache.setLastFileName(latestFile.getAbsolutePath());
        } else {
            logger.debug("No monitored files found for integration: {}", integration.getName());
        }
        
        // Check if integration is inactive and send notification
        if (integration.getIsActive() && integration.getMonitoringEnabled()) {
            LocalDateTime lastFileFound = cache.getLastFileFound();
            LocalDateTime threshold = now.minusMinutes(integration.getThresholdMinutes().longValue());
            
            if (lastFileFound == null || lastFileFound.isBefore(threshold)) {
                // Integration is inactive
                notificationService.checkAndSendIntegrationNotification(
                    integration, 
                    lastFileFound
                );
            } else {
                // Integration is active, clear any existing notification
                notificationService.clearIntegrationNotification(integration);
            }
        }
    }

    private File findLatestFile(File directory, Set<FileType> monitoredFileTypes) {
        logger.debug("Looking for latest file in directory: {}", directory.getAbsolutePath());
        
        if (monitoredFileTypes == null || monitoredFileTypes.isEmpty()) {
            logger.debug("No specific file types configured, monitoring all files");
            return findLatestFileAllTypes(directory);
        }
        
        Set<String> extensions = monitoredFileTypes.stream()
            .map(FileType::getExtension)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        
        logger.debug("Looking for files with extensions: {}", extensions);
        
        return findLatestFileByExtensions(directory, extensions);
    }

    
    private File findLatestFileAllTypes(File directory) {
        logger.debug("Finding latest file using Linux find command in directory: {}", directory.getAbsolutePath());
        
        try {
            // Use Linux find command to find the most recently modified file
            String command = String.format("find %s -type f -printf '%%T@ %%p\\n' | sort -nr | head -n1 | cut -d' ' -f2-", 
                ShellCommandUtil.escapeForBash(directory.getAbsolutePath()));
            
            ShellCommandUtil.CommandResult result = ShellCommandUtil.execute(command, 60);
            
            if (result.getExitCode() == 0 && !result.getOutput().isEmpty()) {
                String line = result.getOutput().get(0).trim();
                if (!line.isEmpty()) {
                    File latestFile = new File(line);
                    if (latestFile.exists()) {
                        logger.debug("Latest file found: {} (modified: {})", latestFile.getName(), new Date(latestFile.lastModified()));
                        return latestFile;
                    }
                }
            }
            
            logger.debug("No files found in directory");
            return null;
            
        } catch (Exception e) {
            logger.warn("Failed to find latest file using Linux command: {}", e.getMessage());
            return null;
        }
    }

    private File findLatestFileByExtensions(File directory, Set<String> extensions) {
        logger.debug("Finding latest file with extensions {} using Linux find command in directory: {}", extensions, directory.getAbsolutePath());
        
        try {
            // Build extension pattern for find command
            String extensionPattern = extensions.stream()
                .map(ext -> ext.startsWith(".") ? ext : "." + ext)
                .map(ext -> "-name " + ShellCommandUtil.escapeForBash("*" + ext))
                .collect(Collectors.joining(" -o "));
            
            String command = String.format("find %s -type f \\( %s \\) -printf '%%T@ %%p\\n' | sort -nr | head -n1 | cut -d' ' -f2-", 
                ShellCommandUtil.escapeForBash(directory.getAbsolutePath()), extensionPattern);
            
            ShellCommandUtil.CommandResult result = ShellCommandUtil.execute(command, 60);
            
            if (result.getExitCode() == 0 && !result.getOutput().isEmpty()) {
                String line = result.getOutput().get(0).trim();
                if (!line.isEmpty()) {
                    File latestFile = new File(line);
                    if (latestFile.exists()) {
                        logger.debug("Latest matching file found: {} (modified: {})", latestFile.getName(), new Date(latestFile.lastModified()));
                        return latestFile;
                    }
                }
            }
            
            logger.debug("No matching files found");
            return null;
            
        } catch (Exception e) {
            logger.warn("Failed to find latest file with extensions using Linux command: {}", e.getMessage());
            return null;
        }
    }

    
    public boolean isIntegrationActive(Integration integration) {
        if (!integration.getIsActive() || !integration.getMonitoringEnabled()) {
            return false;
        }
        
        IntegrationCache cache = integrationCacheService.getCache(integration.getId());
        LocalDateTime lastFileFound = cache.getLastFileFound();
        if (lastFileFound == null) {
            return false;
        }
        
        LocalDateTime threshold = lastFileFound.plusMinutes(integration.getThresholdMinutes());
        return LocalDateTime.now().isBefore(threshold);
    }
    
    /**
     * MEMORY CLEANUP: Remove cache entries for deleted integrations
     * This prevents memory leaks when integrations are deleted from database
     */
    private void cleanupDeletedIntegrations(List<Integration> activeIntegrations) {
        // Get all active integration IDs
        Set<Long> activeIntegrationIds = activeIntegrations.stream()
            .map(Integration::getId)
            .collect(Collectors.toSet());
        
        // Clear cache entries for deleted integrations
        Set<Long> cachedIntegrationIds = integrationCacheService.getAllCachedIds();
        for (Long cachedId : cachedIntegrationIds) {
            if (!activeIntegrationIds.contains(cachedId)) {
                integrationCacheService.clearCache(cachedId);
                logger.debug("Cleared cache for deleted integration ID: {}", cachedId);
            }
        }
    }
}
