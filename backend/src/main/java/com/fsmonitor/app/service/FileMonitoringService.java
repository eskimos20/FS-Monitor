package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.entity.FileType;
import com.fsmonitor.app.repository.IntegrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    @Transactional
    public void init() {
        // First run will be 60s after startup
        nextSchedulerRun = LocalDateTime.now().plusSeconds(60);
        
        // Reset lastCheckedAt for all integrations on startup
        // This ensures timers start from their full interval
        List<Integration> allIntegrations = integrationRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (Integration integration : allIntegrations) {
            integration.setLastCheckedAt(now);
            integrationRepository.save(integration);
        }
    }

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(fixedRate = 60000, initialDelay = 60000) // Run every minute, wait 1 min after startup
    @Transactional
    public void monitorIntegrations() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        lastSchedulerRun = now;
        nextSchedulerRun = now.plusSeconds(60);
        
        logger.debug("Starting file monitoring check...");
        
        List<Integration> activeIntegrations = integrationRepository.findActiveMonitoringIntegrations();
        
        for (Integration integration : activeIntegrations) {
            try {
                if (shouldCheckIntegration(integration, now)) {
                    logger.info("Starting check for integration: {} (ID: {})", integration.getName(), integration.getId());
                    long startTime = System.currentTimeMillis();
                    checkIntegration(integration, now);
                    long endTime = System.currentTimeMillis();
                    logger.info("Completed check for integration: {} in {}ms", integration.getName(), (endTime - startTime));
                }
            } catch (Exception e) {
                logger.error("Error monitoring integration: " + integration.getName(), e);
            }
        }
        
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
        
        LocalDateTime lastCheck = integration.getLastCheckedAt();
        if (lastCheck == null) {
            integration.setLastCheckedAt(now);
            integrationRepository.save(integration);
            return false;
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

        // Mark that the scheduler checked this integration at this cycle's timestamp
        integration.setLastCheckedAt(now);

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
            
            integration.setLastFileFound(fileModifiedTime);
            integration.setLastFileName(latestFile.getAbsolutePath());
        } else {
            logger.debug("No monitored files found for integration: {}", integration.getName());
        }
        
        integrationRepository.save(integration);
        
        // Check if integration is inactive and send notification
        if (integration.getIsActive() && integration.getMonitoringEnabled()) {
            LocalDateTime lastFileFound = integration.getLastFileFound();
            LocalDateTime threshold = now.minusMinutes(integration.getThresholdMinutes().longValue());
            
            if (lastFileFound == null || lastFileFound.isBefore(threshold)) {
                // Integration is inactive
                notificationService.checkAndSendIntegrationNotification(
                    integration.getId(), 
                    integration.getName(), 
                    lastFileFound
                );
            } else {
                // Integration is active, clear any existing notification
                notificationService.clearIntegrationNotification(integration.getId());
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

    private boolean isHidden(Path path) {
        return path.getFileName() != null && path.getFileName().toString().startsWith(".");
    }

    private File findLatestFileAllTypes(File directory) {
        logger.debug("Scanning all files in directory: {}", directory.getAbsolutePath());
        File latestFile = null;
        long latestTime = 0;
        
        try {
            List<File> files = Files.walk(directory.toPath())
                .filter(p -> !isHidden(p))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
            
            logger.debug("Found {} total files in directory", files.size());
            
            for (File file : files) {
                if (file.lastModified() > latestTime) {
                    latestTime = file.lastModified();
                    latestFile = file;
                }
            }
            
            if (latestFile != null) {
                logger.debug("Latest file found: {} (modified: {})", latestFile.getName(), new Date(latestFile.lastModified()));
            } else {
                logger.debug("No files found in directory");
            }
        } catch (IOException e) {
            logger.error("Error walking directory: {}", directory.getAbsolutePath(), e);
        }
        
        return latestFile;
    }

    private File findLatestFileByExtensions(File directory, Set<String> extensions) {
        logger.debug("Scanning files with extensions {} in directory: {}", extensions, directory.getAbsolutePath());
        File latestFile = null;
        long latestTime = 0;
        
        try {
            List<File> files = Files.walk(directory.toPath())
                .filter(p -> !isHidden(p))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(file -> hasMatchingExtension(file, extensions))
                .collect(Collectors.toList());
            
            logger.debug("Found {} matching files", files.size());
            
            for (File file : files) {
                if (file.lastModified() > latestTime) {
                    latestTime = file.lastModified();
                    latestFile = file;
                }
            }
            
            if (latestFile != null) {
                logger.debug("Latest matching file found: {} (modified: {})", latestFile.getName(), new Date(latestFile.lastModified()));
            } else {
                logger.debug("No matching files found");
            }
        } catch (IOException e) {
            logger.error("Error walking directory: {}", directory.getAbsolutePath(), e);
        }
        
        return latestFile;
    }

    private boolean hasMatchingExtension(File file, Set<String> extensions) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        
        if (lastDotIndex == -1) {
            return extensions.contains(""); // No extension
        }
        
        String extension = fileName.substring(lastDotIndex).toLowerCase();
        return extensions.contains(extension);
    }

    public boolean isIntegrationActive(Integration integration) {
        if (!integration.getIsActive() || !integration.getMonitoringEnabled()) {
            return false;
        }
        
        LocalDateTime lastFileFound = integration.getLastFileFound();
        if (lastFileFound == null) {
            return false;
        }
        
        LocalDateTime threshold = lastFileFound.plusMinutes(integration.getThresholdMinutes());
        return LocalDateTime.now().isBefore(threshold);
    }
}
