package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.DeleteService;
import com.fsmonitor.app.repository.DeleteServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Scheduled file-cleanup services: deletes aged files matching configured
 * file types and optionally removes empty directories afterwards.
 * Filesystem traversal is pure java.nio - no external "find" processes.
 */
@Service
public class DeleteMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteMonitoringService.class);

    /**
     * Paths where a delete service is never allowed to run - guard against
     * catastrophic misconfiguration (e.g. "/" with deleteAge=0).
     */
    private static final Set<String> PROTECTED_ROOTS = Set.of(
            "/", "/bin", "/boot", "/dev", "/etc", "/lib", "/lib64",
            "/proc", "/root", "/sbin", "/sys", "/usr", "/var/lib", "/var/log");

    private final DeleteServiceRepository deleteServiceRepository;

    private volatile LocalDateTime lastSchedulerRun;
    private volatile LocalDateTime nextSchedulerRun;

    public DeleteMonitoringService(DeleteServiceRepository deleteServiceRepository) {
        this.deleteServiceRepository = deleteServiceRepository;
    }

    public LocalDateTime getLastSchedulerRun() { return lastSchedulerRun; }
    public LocalDateTime getNextSchedulerRun() { return nextSchedulerRun; }

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

                    logger.info("Delete service cleanup completed - Name: {} - Files deleted: {} - Folders deleted: {} - Duration: {}ms",
                            service.getName(), result.filesDeleted, result.foldersDeleted,
                            System.currentTimeMillis() - startTime);

                    service.setLastCleanup(now);
                    service.setFilesDeletedLastScan(result.filesDeleted);
                    service.setFoldersDeletedLastScan(result.foldersDeleted);
                    deleteServiceRepository.save(service);
                }
            } catch (Exception e) {
                logger.error("Error in delete service: {}", service.getName(), e);
            }
        }

        nextSchedulerRun = LocalDateTime.now()
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusSeconds(60);

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

        // Resolve symlinks so a configured symlink (e.g. /tmp/link -> /)
        // cannot bypass the protected-path guard below
        Path directory = resolveRealPath(
                Paths.get(service.getPath()).normalize().toAbsolutePath());

        if (isProtectedPath(directory)) {
            logger.error("Refusing to run delete service {} on protected path {}", service.getName(), directory);
            return new CleanupResult();
        }
        if (service.getDeleteAgeMinutes() == null || service.getDeleteAgeMinutes() <= 0) {
            logger.error("Refusing to run delete service {} - delete age must be > 0", service.getName());
            return new CleanupResult();
        }
        if (!Files.isDirectory(directory)) {
            logger.warn("Directory does not exist for delete service {}: {}", service.getName(), service.getPath());
            return new CleanupResult();
        }

        LocalDateTime cutoffTime = now.minusMinutes(service.getDeleteAgeMinutes());
        Set<String> extensions = parseFileTypes(service.getFileTypes());
        boolean deleteEmptyDirs = Boolean.TRUE.equals(service.getDeleteEmptyDirectories());
        boolean recursive = Boolean.TRUE.equals(service.getRecursive());

        CleanupResult result = new CleanupResult();

        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Non-recursive mode: never descend into subdirectories
                    if (!recursive && !dir.equals(directory)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!attrs.isSymbolicLink() && shouldDeleteFile(file, attrs, cutoffTime, extensions)) {
                        try {
                            Files.delete(file);
                            result.filesDeleted++;
                            logger.debug("Deleted file: {}", file);
                        } catch (IOException e) {
                            logger.warn("Failed to delete file: {} - {}", file, e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    // Delete emptied subdirectories (never the configured root itself)
                    if (deleteEmptyDirs && !dir.equals(directory)) {
                        try (var stream = Files.list(dir)) {
                            if (stream.findFirst().isEmpty()) {
                                Files.delete(dir);
                                result.foldersDeleted++;
                                logger.debug("Deleted empty directory: {}", dir);
                            }
                        } catch (IOException e) {
                            logger.debug("Directory not empty or cannot be deleted: {} - {}", dir, e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.debug("Cannot access {}: {}", file, exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Error walking directory {}: {}", directory, e.getMessage());
        }

        return result;
    }

    /** Follows the path to its real target; falls back to the unresolved path
     *  when it does not exist (the isDirectory check reports that case). */
    private static Path resolveRealPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path;
        }
    }

    /** A configured path must not BE a protected directory or a parent of one
     *  ("/" or "/var" would wipe system directories). Cleaning inside a
     *  protected tree (e.g. /var/log/myapp) is still allowed. */
    static boolean isProtectedPath(Path directory) {
        String normalized = directory.toString();
        for (String protectedPath : PROTECTED_ROOTS) {
            if (normalized.equals(protectedPath)
                    || (!normalized.equals("/") && protectedPath.startsWith(normalized + "/"))) {
                return true;
            }
        }
        return false;
    }

    static Set<String> parseFileTypes(String fileTypes) {
        if (fileTypes == null || fileTypes.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> extensions = new HashSet<>();
        for (String type : fileTypes.split(",")) {
            String ext = type.trim().toLowerCase();
            if (!ext.isEmpty()) {
                extensions.add(ext.startsWith(".") ? ext : "." + ext);
            }
        }
        return extensions;
    }

    private boolean shouldDeleteFile(Path file, BasicFileAttributes attrs,
                                     LocalDateTime cutoffTime, Set<String> extensions) {
        LocalDateTime fileModifiedTime = LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());

        if (fileModifiedTime.isAfter(cutoffTime)) {
            return false;
        }

        if (extensions.isEmpty()) {
            return true;
        }

        String fileName = file.getFileName().toString().toLowerCase();
        for (String ext : extensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    // ---- CRUD (used by DeleteServiceController) ----

    public List<DeleteService> findAll() {
        return deleteServiceRepository.findAll();
    }

    public Optional<DeleteService> findById(Long id) {
        return deleteServiceRepository.findById(id);
    }

    public DeleteService create(DeleteService service) {
        return deleteServiceRepository.save(service);
    }

    public Optional<DeleteService> update(Long id, DeleteService incoming) {
        return deleteServiceRepository.findById(id).map(existing -> {
            existing.setName(incoming.getName());
            existing.setPath(incoming.getPath());
            existing.setFileTypes(incoming.getFileTypes());
            existing.setCleanupEnabled(incoming.getCleanupEnabled());
            existing.setCleanupIntervalValue(incoming.getCleanupIntervalValue());
            existing.setCleanupIntervalUnit(incoming.getCleanupIntervalUnit());
            existing.setDeleteAgeValue(incoming.getDeleteAgeValue());
            existing.setDeleteAgeUnit(incoming.getDeleteAgeUnit());
            existing.setRecursive(incoming.getRecursive());
            existing.setDeleteEmptyDirectories(incoming.getDeleteEmptyDirectories());
            return deleteServiceRepository.save(existing);
        });
    }

    public boolean delete(Long id) {
        return deleteServiceRepository.findById(id).map(service -> {
            deleteServiceRepository.delete(service);
            return true;
        }).orElse(false);
    }

    public Optional<DeleteService> toggle(Long id) {
        return deleteServiceRepository.findById(id).map(service -> {
            service.setCleanupEnabled(!Boolean.TRUE.equals(service.getCleanupEnabled()));
            return deleteServiceRepository.save(service);
        });
    }

    /** Manually trigger a cleanup run (used by a potential run-now endpoint). */
    public void runServiceNow(Long serviceId) {
        DeleteService service = deleteServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Delete service not found: " + serviceId));
        CleanupResult result = performCleanup(service, LocalDateTime.now());
        service.setLastCleanup(LocalDateTime.now());
        service.setFilesDeletedLastScan(result.filesDeleted);
        service.setFoldersDeletedLastScan(result.foldersDeleted);
        deleteServiceRepository.save(service);
    }

    private static class CleanupResult {
        int filesDeleted;
        int foldersDeleted;
    }
}
