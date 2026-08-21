package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.DeleteService;
import com.fsmonitor.app.repository.DeleteServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

class CleanupResult {
    int filesDeleted;
    int foldersDeleted;
    
    CleanupResult(int filesDeleted, int foldersDeleted) {
        this.filesDeleted = filesDeleted;
        this.foldersDeleted = foldersDeleted;
    }
}

@Service
public class DeleteServiceMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(DeleteServiceMonitoringService.class);

    @Autowired
    private DeleteServiceRepository deleteServiceRepository;

    private LocalDateTime lastSchedulerRun;
    private LocalDateTime nextSchedulerRun;
    private Map<Long, LocalDateTime> serviceNextRun = new HashMap<>();

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void monitorDeleteServices() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        lastSchedulerRun = now;
        
        logger.debug("Starting delete service cleanup check...");
        
        List<DeleteService> activeServices = deleteServiceRepository.findActiveCleanupServices();
        
        for (DeleteService service : activeServices) {
            try {
                if (shouldRunCleanup(service, now)) {
                    long startTime = System.currentTimeMillis();
                    CleanupResult result = performCleanup(service, now);
                    long endTime = System.currentTimeMillis();
                    
                    logger.info("Delete service cleanup completed - Name: {} - Files deleted: {} - Folders deleted: {} - Duration: {}ms", 
                        service.getName(), result.filesDeleted, result.foldersDeleted, (endTime - startTime));
                    
                    service.setLastCleanup(now);
                    service.setFilesDeletedLastScan(result.filesDeleted);
                    service.setFoldersDeletedLastScan(result.foldersDeleted);
                    deleteServiceRepository.save(service);
                    
                    serviceNextRun.put(service.getId(), now.plusMinutes(service.getCleanupIntervalMinutes()));
                }
            } catch (Exception e) {
                logger.error("Error in delete service: " + service.getName(), e);
            }
        }
        
        nextSchedulerRun = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusSeconds(60);
        
        logger.debug("Delete service cleanup check completed for {} services", activeServices.size());
    }

    private boolean shouldRunCleanup(DeleteService service, LocalDateTime now) {
        LocalDateTime lastCleanup = service.getLastCleanup();
        if (lastCleanup == null) {
            return true;
        }
        
        LocalDateTime nextCleanup = lastCleanup.truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
            .plusMinutes(service.getCleanupIntervalMinutes());
        return !now.isBefore(nextCleanup);
    }

    private CleanupResult performCleanup(DeleteService service, LocalDateTime now) {
        logger.debug("Performing cleanup for service: {}", service.getName());
        
        String path = service.getPath();
        File directory = new File(path);
        
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warn("Directory does not exist for delete service {}: {}", service.getName(), path);
            return new CleanupResult(0, 0);
        }

        LocalDateTime cutoffTime = now.minusMinutes(service.getDeleteAgeMinutes());
        Set<String> extensions = parseFileTypes(service.getFileTypes());
        boolean deleteEmptyDirs = Boolean.TRUE.equals(service.getDeleteEmptyDirectories());
        
        CleanupResult result;
        if (Boolean.TRUE.equals(service.getRecursive())) {
            result = deleteFilesRecursive(directory, cutoffTime, extensions, deleteEmptyDirs);
        } else {
            int filesDeleted = deleteFilesNonRecursive(directory, cutoffTime, extensions);
            result = new CleanupResult(filesDeleted, 0);
        }
        
        return result;
    }

    private Set<String> parseFileTypes(String fileTypes) {
        if (fileTypes == null || fileTypes.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<String> extensions = new HashSet<>();
        String[] types = fileTypes.split(",");
        for (String type : types) {
            String ext = type.trim().toLowerCase();
            if (!ext.isEmpty()) {
                if (!ext.startsWith(".")) {
                    ext = "." + ext;
                }
                extensions.add(ext);
            }
        }
        return extensions;
    }

    private CleanupResult deleteFilesRecursive(File directory, LocalDateTime cutoffTime, Set<String> extensions, boolean deleteEmptyDirs) {
        int filesDeleted = 0;
        int foldersDeleted = 0;
        File[] files = directory.listFiles();
        if (files == null) {
            return new CleanupResult(0, 0);
        }
        
        for (File file : files) {
            if (java.nio.file.Files.isSymbolicLink(file.toPath())) {
                logger.debug("Skipping symlink: {}", file.getAbsolutePath());
                continue;
            }
            if (file.isDirectory()) {
                CleanupResult subResult = deleteFilesRecursive(file, cutoffTime, extensions, deleteEmptyDirs);
                filesDeleted += subResult.filesDeleted;
                foldersDeleted += subResult.foldersDeleted;
                
                // Ta bort tomma mappar om flaggan är satt
                if (deleteEmptyDirs) {
                    File[] remainingFiles = file.listFiles();
                    if (remainingFiles != null && remainingFiles.length == 0) {
                        if (file.delete()) {
                            logger.debug("Deleted empty directory: {}", file.getAbsolutePath());
                            foldersDeleted++;
                        } else {
                            logger.warn("Failed to delete empty directory: {}", file.getAbsolutePath());
                        }
                    }
                }
            } else if (file.isFile()) {
                if (shouldDeleteFile(file, cutoffTime, extensions)) {
                    if (file.delete()) {
                        logger.debug("Deleted file: {}", file.getAbsolutePath());
                        filesDeleted++;
                    } else {
                        logger.warn("Failed to delete file: {}", file.getAbsolutePath());
                    }
                }
            }
        }
        
        return new CleanupResult(filesDeleted, foldersDeleted);
    }

    private int deleteFilesNonRecursive(File directory, LocalDateTime cutoffTime, Set<String> extensions) {
        int count = 0;
        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }
        
        for (File file : files) {
            if (java.nio.file.Files.isSymbolicLink(file.toPath())) {
                logger.debug("Skipping symlink: {}", file.getAbsolutePath());
                continue;
            }
            if (file.isFile()) {
                if (shouldDeleteFile(file, cutoffTime, extensions)) {
                    if (file.delete()) {
                        logger.debug("Deleted file: {}", file.getAbsolutePath());
                        count++;
                    } else {
                        logger.warn("Failed to delete file: {}", file.getAbsolutePath());
                    }
                }
            }
        }
        
        return count;
    }

    private boolean shouldDeleteFile(File file, LocalDateTime cutoffTime, Set<String> extensions) {
        LocalDateTime fileModifiedTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(file.lastModified()), 
            ZoneId.systemDefault()
        );
        
        if (fileModifiedTime.isAfter(cutoffTime)) {
            return false;
        }
        
        if (extensions.isEmpty()) {
            return true;
        }
        
        String fileName = file.getName().toLowerCase();
        for (String ext : extensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        
        return false;
    }

    public LocalDateTime getLastSchedulerRun() {
        return lastSchedulerRun;
    }

    public LocalDateTime getNextSchedulerRun() {
        return nextSchedulerRun;
    }

    public Map<Long, LocalDateTime> getServiceNextRun() {
        return serviceNextRun;
    }
}
