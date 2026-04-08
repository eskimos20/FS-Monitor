package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.FileType;
import com.fsmonitor.app.entity.Integration;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FileCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(FileCleanupService.class);

    @Autowired
    private IntegrationRepository integrationRepository;

    @Scheduled(fixedRate = 60000, initialDelay = 60000) // Run every minute, wait 1 min after startup
    @Transactional
    public void cleanupOldFiles() {
        logger.debug("Starting scheduled file cleanup check...");
        
        List<Integration> activeCleanupIntegrations = integrationRepository.findActiveCleanupIntegrations();
        
        if (activeCleanupIntegrations.isEmpty()) {
            logger.debug("No integrations with cleanup enabled");
            return;
        }
        
        for (Integration integration : activeCleanupIntegrations) {
            try {
                cleanupIntegrationFiles(integration);
            } catch (Exception e) {
                logger.error("Error during cleanup for integration: " + integration.getName(), e);
            }
        }
    }

    private void cleanupIntegrationFiles(Integration integration) {
        long startTime = System.currentTimeMillis();
        String path = integration.getPath();
        File directory = new File(path);
        
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warn("Directory does not exist for integration {}: {}", integration.getName(), path);
            return;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusMinutes(integration.getCleanupAgeMinutes());
        long cutoffTime = cutoffDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        Set<String> extensionsToClean = getExtensionsToClean(integration);
        
        int deletedFiles = 0;
        int failedDeletes = 0;
        
        try {
            List<File> allFiles = Files.walk(directory.toPath())
                .filter(Files::isRegularFile)
                .filter(p -> !isHidden(p))
                .map(Path::toFile)
                .collect(Collectors.toList());
            
            List<File> filesToDelete = allFiles.stream()
                .filter(file -> shouldDeleteFile(file, cutoffTime, extensionsToClean))
                .collect(Collectors.toList());
            
            for (File file : filesToDelete) {
                try {
                    Files.delete(file.toPath());
                    deletedFiles++;
                } catch (IOException e) {
                    failedDeletes++;
                    logger.warn("Failed to delete file: {}", file.getAbsolutePath(), e);
                }
            }
            
            // Clean up empty directories (except root)
            int deletedDirs = cleanupEmptyDirectories(directory, directory);
            
            long endTime = System.currentTimeMillis();
            if (deletedFiles > 0 || deletedDirs > 0) {
                logger.info("File cleanup completed - Name: {} - Deleted files: {} - Deleted dirs: {} - Failed: {} - Duration: {}ms", 
                    integration.getName(), deletedFiles, deletedDirs, failedDeletes, (endTime - startTime));
            }
                
        } catch (IOException e) {
            logger.error("Error walking directory during cleanup: " + directory.getAbsolutePath(), e);
        }
    }

    private Set<String> getExtensionsToClean(Integration integration) {
        // If monitorAllFiles is true, clean all file types
        if (Boolean.TRUE.equals(integration.getMonitorAllFiles())) {
            return Collections.emptySet();
        }
        
        Set<FileType> monitoredFileTypes = integration.getMonitoredFileTypes();
        
        if (monitoredFileTypes == null || monitoredFileTypes.isEmpty()) {
            // If no specific file types, clean all files
            return Collections.emptySet();
        }
        
        return monitoredFileTypes.stream()
            .map(FileType::getExtension)
            .map(String::toLowerCase)  // Convert to lowercase for consistent matching
            .collect(Collectors.toSet());
    }

    private boolean shouldDeleteFile(File file, long cutoffTime, Set<String> extensions) {
        try {
            // Get both timestamps for comparison
            long fileModifiedTime = file.lastModified();
            long nioModifiedTime = Files.getLastModifiedTime(file.toPath()).toMillis();
            
            // Use the older timestamp (more likely to be correct)
            long actualModifiedTime = Math.min(fileModifiedTime, nioModifiedTime);
            
            // Check file age
            if (actualModifiedTime > cutoffTime) {
                return false; // File is too new
            }
            
            // If no specific extensions, delete all old files
            if (extensions.isEmpty()) {
                return true;
            }
            
            // Check if file extension matches monitored types
            return hasMatchingExtension(file, extensions);
        } catch (IOException e) {
            logger.error("Error checking file: {}", file.getName(), e);
            return false;
        }
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

    public void cleanupIntegrationFilesNow(Long integrationId) {
        Integration integration = integrationRepository.findById(integrationId)
            .orElseThrow(() -> new RuntimeException("Integration not found with id: " + integrationId));
        
        cleanupIntegrationFiles(integration);
    }

    public Map<String, Object> getCleanupPreview(Long integrationId) {
        Integration integration = integrationRepository.findById(integrationId)
            .orElseThrow(() -> new RuntimeException("Integration not found with id: " + integrationId));
        
        String path = integration.getPath();
        File directory = new File(path);
        
        if (!directory.exists() || !directory.isDirectory()) {
            return Map.of(
                "error", "Directory does not exist",
                "path", path
            );
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusMinutes(integration.getCleanupAgeMinutes());
        long cutoffTime = cutoffDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        Set<String> extensionsToClean = getExtensionsToClean(integration);
        
        try {
            List<Map<String, Object>> filesToDelete = Files.walk(directory.toPath())
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(file -> shouldDeleteFile(file, cutoffTime, extensionsToClean))
                .map(file -> {
                    Map<String, Object> fileMap = new HashMap<>();
                    fileMap.put("name", file.getName());
                    fileMap.put("path", file.getAbsolutePath());
                    fileMap.put("size", file.length());
                    fileMap.put("lastModified", file.lastModified());
                    return fileMap;
                })
                .collect(Collectors.toList());
            
            long totalSize = filesToDelete.stream()
                .mapToLong(file -> (Long) file.get("size"))
                .sum();
            
            return Map.of(
                "integrationName", integration.getName(),
                "cutoffDate", cutoffDate.toString(),
                "filesToDelete", filesToDelete,
                "fileCount", filesToDelete.size(),
                "totalSize", Long.valueOf(totalSize)
            );
            
        } catch (IOException e) {
            logger.error("Error during cleanup preview for integration: " + integration.getName(), e);
            return Map.of(
                "error", "Error scanning directory: " + e.getMessage()
            );
        }
    }

    private int cleanupEmptyDirectories(File directory, File rootDirectory) {
        int deletedCount = 0;
        
        try {
            // Walk directory bottom-up to delete empty dirs
            List<File> directories = Files.walk(directory.toPath())
                .filter(Files::isDirectory)
                .filter(p -> !isHidden(p))
                .map(Path::toFile)
                .sorted((a, b) -> b.getAbsolutePath().compareTo(a.getAbsolutePath())) // Sort reverse (deepest first)
                .collect(Collectors.toList());
            
            for (File dir : directories) {
                // Don't delete the root directory
                if (dir.getAbsolutePath().equals(rootDirectory.getAbsolutePath())) {
                    continue;
                }
                
                // Check if directory is empty (no files or subdirectories)
                File[] contents = dir.listFiles();
                if (contents != null && contents.length == 0) {
                    try {
                        Files.delete(dir.toPath());
                        deletedCount++;
                        logger.debug("Deleted empty directory: {}", dir.getAbsolutePath());
                    } catch (IOException e) {
                        logger.error("Failed to delete empty directory: " + dir.getAbsolutePath(), e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error during empty directory cleanup: " + directory.getAbsolutePath(), e);
        }
        
        return deletedCount;
    }

    private boolean isHidden(Path path) {
        return path.getFileName() != null && path.getFileName().toString().startsWith(".");
    }

    private String getFileExtension(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        
        if (lastDotIndex == -1) {
            return ""; // No extension
        }
        
        return fileName.substring(lastDotIndex).toLowerCase();
    }
}
