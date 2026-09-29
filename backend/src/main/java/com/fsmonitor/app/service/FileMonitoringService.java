package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.IntegrationCacheService;
import com.fsmonitor.app.cache.IntegrationCacheService.IntegrationCache;
import com.fsmonitor.app.entity.FileType;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.repository.IntegrationRepository;
import com.fsmonitor.app.util.ScheduleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(FileMonitoringService.class);

    private final IntegrationRepository integrationRepository;
    private final NotificationService notificationService;
    private final IntegrationCacheService integrationCacheService;

    // Track when the scheduler actually runs (for the dashboard countdown)
    private volatile LocalDateTime lastSchedulerRun;
    private volatile LocalDateTime nextSchedulerRun;

    public FileMonitoringService(IntegrationRepository integrationRepository,
                                 NotificationService notificationService,
                                 IntegrationCacheService integrationCacheService) {
        this.integrationRepository = integrationRepository;
        this.notificationService = notificationService;
        this.integrationCacheService = integrationCacheService;
    }

    @PostConstruct
    public void initializeIntegrationMonitoring() {
        logger.info("Initializing integration monitoring on startup");
        // The scheduler fires 60s after startup - seed the countdown state so the
        // API reports a real schedule instead of a flat 60s estimate until then
        lastSchedulerRun = LocalDateTime.now();
        nextSchedulerRun = LocalDateTime.now().plusSeconds(60);
        // A restart starts a fresh cycle for every integration: intervals count
        // from startup (countdown shows the full configured interval) and no
        // rescan storm is triggered.
        try {
            LocalDateTime startup = LocalDateTime.now();
            for (Integration integration : integrationRepository.findAll()) {
                integrationCacheService.getCache(integration.getId())
                        .setLastCheckedAt(startup);
            }
        } catch (Exception e) {
            logger.warn("Could not seed integration check timestamps: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void monitorIntegrations() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        lastSchedulerRun = now;
        // Estimate next run up-front so the countdown never reads a stale (past)
        // timestamp while a long scan is in progress; refined to the real value below
        nextSchedulerRun = now.plusSeconds(60);

        logger.debug("Starting file monitoring check...");

        List<Integration> activeIntegrations = integrationRepository.findActiveMonitoringIntegrations();

        // Drop cache entries for deleted integrations
        Set<Long> activeIds = activeIntegrations.stream()
                .map(Integration::getId)
                .collect(Collectors.toSet());
        integrationCacheService.cleanupDeletedConfigs(activeIds);

        for (Integration integration : activeIntegrations) {
            try {
                if (shouldCheckIntegration(integration, now)) {
                    long startTime = System.currentTimeMillis();
                    checkIntegration(integration, now);
                    logger.info("Integration scan completed - Name: {} - Duration: {}ms",
                            integration.getName(), System.currentTimeMillis() - startTime);
                }
            } catch (Exception e) {
                logger.error("Error monitoring integration: {}", integration.getName(), e);
            }
        }

        // Set next run AFTER all integrations have been checked
        nextSchedulerRun = LocalDateTime.now()
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusSeconds(60);

        logger.debug("File monitoring check completed for {} integrations", activeIntegrations.size());
    }

    public LocalDateTime getLastSchedulerRun() { return lastSchedulerRun; }
    public LocalDateTime getNextSchedulerRun() { return nextSchedulerRun; }

    private boolean shouldCheckIntegration(Integration integration, LocalDateTime now) {
        // Check schedule constraints first
        if (Boolean.TRUE.equals(integration.getScheduleEnabled())) {
            if (!ScheduleUtil.isWithinSchedule(integration.getActiveDays(),
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
            return true; // First check - run immediately
        }

        Long intervalMin = integration.getCheckIntervalMinutes();
        LocalDateTime nextCheck = lastCheck.truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
            .plusMinutes(intervalMin != null ? intervalMin : 5L);
        return !now.isBefore(nextCheck);
    }

    private void checkIntegration(Integration integration, LocalDateTime now) {
        logger.debug("Checking integration: {}", integration.getName());

        IntegrationCache cache = integrationCacheService.getCache(integration.getId());
        cache.setLastCheckedAt(now);
        // Persist so check pacing survives restarts (bulk update - does not touch updatedAt)
        try {
            integrationRepository.updateLastCheck(integration.getId(), now);
        } catch (Exception e) {
            logger.debug("Could not persist lastCheck for integration {}: {}", integration.getId(), e.getMessage());
        }

        String path = integration.getPath();
        Path directory = Paths.get(path);

        if (!Files.isDirectory(directory)) {
            // Missing directory (e.g. unmounted share) - fall through to the
            // notification block so a stale lastFileFound still alerts.
            logger.warn("Directory does not exist for integration {}: {}", integration.getName(), path);
        } else {
            Path latestFile;
            if (Boolean.TRUE.equals(integration.getMonitorAllFiles())) {
                latestFile = findLatestFile(directory, null);
            } else {
                latestFile = findLatestFile(directory, extensionSet(integration));
            }

            if (latestFile != null) {
                try {
                    LocalDateTime fileModifiedTime = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(latestFile).toInstant(), ZoneId.systemDefault());
                    cache.setLastFileFound(fileModifiedTime);
                    cache.setLastFileName(latestFile.toAbsolutePath().toString());
                } catch (IOException e) {
                    logger.warn("Could not read modification time of {}: {}", latestFile, e.getMessage());
                }
            } else {
                logger.debug("No monitored files found for integration: {}", integration.getName());
            }
        }

        // Check if integration is inactive and send notification
        if (Boolean.TRUE.equals(integration.getIsActive())
                && Boolean.TRUE.equals(integration.getMonitoringEnabled())) {
            LocalDateTime lastFileFound = cache.getLastFileFound();
            LocalDateTime threshold = now.minusMinutes(integration.getThresholdMinutes());

            if (lastFileFound == null || lastFileFound.isBefore(threshold)) {
                notificationService.checkAndSendIntegrationNotification(integration, lastFileFound);
            } else {
                notificationService.clearIntegrationNotification(integration);
            }
        }
    }

    /** Trigger an immediate check (used by the "check now" API endpoint).
     *  Runs async so the HTTP request returns immediately. */
    @org.springframework.scheduling.annotation.Async("monitorTaskExecutor")
    public void checkIntegrationNow(Integration integration) {
        checkIntegration(integration,
                LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }

    private Set<String> extensionSet(Integration integration) {
        Set<FileType> monitoredFileTypes = integration.getMonitoredFileTypes();
        if (monitoredFileTypes == null || monitoredFileTypes.isEmpty()) {
            return null; // no filter - monitor all files
        }
        return monitoredFileTypes.stream()
                .map(FileType::getExtension)
                .map(String::toLowerCase)
                .map(ext -> ext.startsWith(".") ? ext : "." + ext)
                .collect(Collectors.toSet());
    }

    /**
     * Find the most recently modified regular file below the directory.
     * {@code extensions} filters by file extension (null = all files).
     * Implemented with java.nio - no external find/sort processes.
     */
    private Path findLatestFile(Path directory, Set<String> extensions) {
        try (Stream<Path> stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !Files.isSymbolicLink(p))
                    .filter(p -> extensions == null || matchesExtension(p, extensions))
                    .max((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .orElse(null);
        } catch (IOException e) {
            logger.warn("Failed to scan directory {}: {}", directory, e.getMessage());
            return null;
        }
    }

    private boolean matchesExtension(Path file, Set<String> extensions) {
        String name = file.getFileName().toString().toLowerCase();
        for (String ext : extensions) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public boolean isIntegrationActive(Integration integration) {
        if (!Boolean.TRUE.equals(integration.getIsActive())
                || !Boolean.TRUE.equals(integration.getMonitoringEnabled())) {
            return false;
        }

        IntegrationCache cache = integrationCacheService.getCache(integration.getId());
        LocalDateTime lastFileFound = cache.getLastFileFound();
        if (lastFileFound == null) {
            return false;
        }

        Long thresholdMin = integration.getThresholdMinutes();
        LocalDateTime threshold = lastFileFound.plusMinutes(thresholdMin != null ? thresholdMin : 15L);
        return LocalDateTime.now().isBefore(threshold);
    }
}
