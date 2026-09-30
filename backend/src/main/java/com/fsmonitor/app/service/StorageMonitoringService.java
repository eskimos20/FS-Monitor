package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.StorageCacheService;
import com.fsmonitor.app.cache.StorageCacheService.StorageCache;
import com.fsmonitor.app.cache.model.DiskSpaceData;
import com.fsmonitor.app.cache.model.LargestFileData;
import com.fsmonitor.app.cache.model.StorageInfoData;
import com.fsmonitor.app.entity.StorageConfig;
import com.fsmonitor.app.repository.StorageConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Periodically scans configured storage paths and collects disk usage data.
 * All filesystem traversal uses java.nio - no external processes, no shell
 * escaping and no risk of hung subprocesses.
 */
@Service
public class StorageMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(StorageMonitoringService.class);
    private static final int TOP_FILES_LIMIT = 20;
    private static final int TOP_DIRECTORIES_LIMIT = 20;

    /** Pseudo/virtual filesystems that are never real storage. */
    private static final Set<String> PSEUDO_FS_TYPES = Set.of(
            "tmpfs", "devtmpfs", "efivarfs", "proc", "sysfs", "devpts",
            "cgroup", "cgroup2", "pstore", "securityfs", "debugfs", "tracefs",
            "configfs", "fusectl", "mqueue", "hugetlbfs", "rpc_pipefs",
            "binfmt_misc", "autofs", "bpf", "nsfs", "overlay", "squashfs", "selinuxfs");

    /** Remote filesystems - excluded like "df -l" does; statvfs on a stale
     *  NFS mount can also block for a long time. */
    private static final Set<String> REMOTE_FS_TYPES = Set.of(
            "nfs", "nfs4", "cifs", "smbfs", "ceph", "glusterfs", "9p", "davfs");

    private final StorageConfigRepository storageConfigRepository;
    private final StorageCacheService storageCacheService;
    private final Executor monitorExecutor;
    private final Map<Long, Long> lastCheckTimes = new ConcurrentHashMap<>();

    public StorageMonitoringService(StorageConfigRepository storageConfigRepository,
                                    StorageCacheService storageCacheService,
                                    @org.springframework.beans.factory.annotation.Qualifier("monitorTaskExecutor")
                                    Executor monitorExecutor) {
        this.storageConfigRepository = storageConfigRepository;
        this.storageCacheService = storageCacheService;
        this.monitorExecutor = monitorExecutor;
    }

    private long calculateIntervalMillis(StorageConfig config) {
        Integer configured = config.getCheckIntervalMinutes();
        int interval = configured != null && configured > 0 ? configured : 5;
        return switch (config.getIntervalUnit()) {
            case "HOURS" -> interval * 60L * 60 * 1000;
            case "DAYS" -> interval * 24L * 60 * 60 * 1000;
            case "MONTHS" -> interval * 30L * 24 * 60 * 60 * 1000;
            default -> interval * 60L * 1000; // MINUTES
        };
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void monitorStorage() {
        List<StorageConfig> activeConfigs = storageConfigRepository.findByActiveTrue();
        long currentTime = System.currentTimeMillis();

        // Always refresh system-wide storage (mount point usage)
        scanSystemStorage();

        // Drop cache and timer entries for configs that no longer exist
        Set<Long> activeIds = activeConfigs.stream()
                .map(StorageConfig::getId)
                .collect(Collectors.toSet());
        activeIds.add(-1L); // keep the virtual system cache
        storageCacheService.cleanupDeletedConfigs(activeIds);
        lastCheckTimes.keySet().retainAll(activeIds);

        for (StorageConfig config : activeConfigs) {
            Long lastCheck = lastCheckTimes.get(config.getId());

            if (lastCheck == null) {
                // New config - scan immediately, record time even on failure
                try {
                    scanStorage(config);
                } catch (Exception e) {
                    logger.error("Error in initial scan for config {}: {}", config.getName(), e.getMessage());
                }
                lastCheckTimes.put(config.getId(), currentTime);
                continue;
            }

            if ((currentTime - lastCheck) >= calculateIntervalMillis(config)) {
                try {
                    scanStorage(config);
                    lastCheckTimes.put(config.getId(), currentTime);
                } catch (Exception e) {
                    logger.error("Error in monitorStorage for config {}: {}", config.getName(), e.getMessage());
                }
            }
        }
    }

    private void scanSystemStorage() {
        long startTime = System.currentTimeMillis();
        try {
            StorageCache cache = storageCacheService.getCache(-1L);

            List<StorageInfoData> mounts = getMountPointInfo();
            cache.setStorageInfoList(mounts);

            DiskSpaceData diskSpace = new DiskSpaceData();
            diskSpace.setTotalBytes(mounts.stream()
                    .mapToLong(m -> m.getTotalSizeBytes() != null ? m.getTotalSizeBytes() : 0).sum());
            diskSpace.setUsableBytes(mounts.stream()
                    .mapToLong(m -> m.getFreeSpaceBytes() != null ? m.getFreeSpaceBytes() : 0).sum());
            diskSpace.setScannedAt(LocalDateTime.now());
            cache.setDiskSpace(diskSpace);

            cache.setLargestFiles(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error scanning system-wide storage: {}", e.getMessage());
        }

        logger.info("File System Storage scan completed - Duration: {}ms",
                System.currentTimeMillis() - startTime);
    }

    /**
     * Enumerate local filesystems via the NIO FileStore API (replaces parsing
     * of "df -l" output). Bind mounts and subvolume mounts of the same
     * filesystem are reported once - deduplicated by store name.
     */
    private List<StorageInfoData> getMountPointInfo() {
        List<StorageInfoData> result = new ArrayList<>();
        Set<String> seenStoreNames = new HashSet<>();

        for (FileStore store : FileSystems.getDefault().getFileStores()) {
            try {
                String type = store.type();
                if (type == null || PSEUDO_FS_TYPES.contains(type.toLowerCase())
                        || REMOTE_FS_TYPES.contains(type.toLowerCase())
                        || type.toLowerCase().startsWith("fuse")) {
                    continue;
                }

                long total = store.getTotalSpace();
                long usable = store.getUsableSpace();
                if (total <= 0) {
                    continue;
                }

                // Same underlying filesystem mounted at multiple paths
                // (bind mounts, btrfs subvolumes) - report it once.
                // Deduplicate by store name only: distinct filesystems that
                // happen to share identical space statistics are different
                // storage and must both be reported.
                if (!seenStoreNames.add(store.name())) {
                    continue;
                }

                // FileStore.toString() renders as "/path (type)"
                String mountPath = store.toString();
                int sep = mountPath.indexOf(" (");
                if (sep >= 0) {
                    mountPath = mountPath.substring(0, sep);
                }
                if (!Files.exists(Paths.get(mountPath))) {
                    continue;
                }

                StorageInfoData info = new StorageInfoData();
                info.setPath(mountPath);
                info.setTotalSizeBytes(total);
                info.setFreeSpaceBytes(usable);
                info.setDirectoryCount(1);
                info.setScannedAt(LocalDateTime.now());
                result.add(info);
            } catch (IOException e) {
                logger.debug("Skipping file store {}: {}", store, e.getMessage());
            }
        }
        return result;
    }

    // ---- CRUD (used by StorageConfigController) ----

    public List<StorageConfig> findAll() {
        return storageConfigRepository.findAll();
    }

    public Optional<StorageConfig> findById(Long id) {
        return storageConfigRepository.findById(id);
    }

    /** Saves the config and kicks off an asynchronous initial scan. */
    public StorageConfig create(StorageConfig config) {
        StorageConfig saved = storageConfigRepository.save(config);
        scanStorageAsync(saved);
        return saved;
    }

    public Optional<StorageConfig> update(Long id, StorageConfig incoming) {
        return storageConfigRepository.findById(id).map(existing -> {
            existing.setName(incoming.getName());
            existing.setPath(incoming.getPath());
            existing.setRecursive(incoming.getRecursive());
            existing.setCheckIntervalMinutes(incoming.getCheckIntervalMinutes());
            existing.setIntervalUnit(incoming.getIntervalUnit());
            existing.setActive(incoming.getActive());
            StorageConfig updated = storageConfigRepository.save(existing);
            scanStorageAsync(updated); // async rescan with the new settings
            return updated;
        });
    }

    public boolean delete(Long id) {
        return storageConfigRepository.findById(id).map(config -> {
            storageConfigRepository.delete(config);
            lastCheckTimes.remove(id);
            return true;
        }).orElse(false);
    }

    /** Submit a scan on the monitor executor - returns immediately. */
    public void scanStorageAsync(StorageConfig config) {
        monitorExecutor.execute(() -> {
            try {
                scanStorage(config);
            } catch (Exception e) {
                logger.error("Async storage scan failed for {}: {}", config.getName(), e.getMessage());
            }
        });
    }

    public void scanStorage(StorageConfig config) {
        long startTime = System.currentTimeMillis();
        try {
            Path rootPath = Paths.get(config.getPath());

            if (!Files.exists(rootPath)) {
                logger.warn("Path does not exist: {}", config.getPath());
                return;
            }
            if (!Files.isDirectory(rootPath)) {
                logger.warn("Configured path is not a directory: {}", config.getPath());
                return;
            }

            StorageCache cache = storageCacheService.getCache(config.getId());

            ScanResult scan = scanPath(rootPath, Boolean.TRUE.equals(config.getRecursive()));

            // Top directories by cumulative size
            List<StorageInfoData> storageInfoList = scan.directoryStats.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue().totalSize, e1.getValue().totalSize))
                    .limit(TOP_DIRECTORIES_LIMIT)
                    .map(entry -> {
                        StorageInfoData info = new StorageInfoData();
                        info.setPath(entry.getKey());
                        info.setTotalSizeBytes(entry.getValue().totalSize);
                        info.setFileCount(entry.getValue().fileCount);
                        info.setDirectoryCount(entry.getValue().directoryCount);
                        info.setScannedAt(LocalDateTime.now());
                        return info;
                    })
                    .collect(Collectors.toList());
            cache.setStorageInfoList(storageInfoList);

            // Top largest files (bounded heap - we never keep the full file list)
            List<LargestFileData> largestFilesList = scan.largestFiles.stream()
                    .sorted(Comparator.comparingLong(f -> -f.size))
                    .map(f -> {
                        LargestFileData data = new LargestFileData();
                        data.setFilePath(f.path);
                        data.setSizeBytes(f.size);
                        data.setScannedAt(LocalDateTime.now());
                        return data;
                    })
                    .collect(Collectors.toList());
            cache.setLargestFiles(largestFilesList);

            DiskSpaceData diskSpace = new DiskSpaceData();
            diskSpace.setTotalBytes(scan.rootStats.totalSize);
            diskSpace.setUsableBytes(0L);
            diskSpace.setScannedAt(LocalDateTime.now());
            cache.setDiskSpace(diskSpace);

            logger.info("Storage scan completed - Config: {} - Files: {} - Duration: {}ms",
                    config.getName(), scan.rootStats.fileCount, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Error scanning storage for config {}: {}", config.getName(), e.getMessage());
        }
    }

    /**
     * Walk the directory tree once, accumulating cumulative per-directory
     * sizes, file counts, directory counts and the list of all files.
     * Hidden entries and symlinks are skipped, matching the behaviour of the
     * previous shell-based implementation (find with hidden-file exclusion).
     */
    private ScanResult scanPath(Path rootPath, boolean recursive) throws IOException {
        ScanResult scan = new ScanResult();
        scan.directoryStats.put(rootPath.toString(), scan.rootStats);

        if (!recursive) {
            try (var stream = Files.list(rootPath)) {
                stream.forEach(child -> accumulateChild(rootPath, child, scan));
            }
            return scan;
        }

        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(rootPath)) {
                    if (isHidden(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    scan.directoryStats.put(dir.toString(), new DirectoryStats());
                    accumulateOnAncestors(scan, rootPath, dir, s -> s.directoryCount++);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && !attrs.isSymbolicLink() && !isHidden(file)) {
                    long size = attrs.size();
                    accumulateOnAncestors(scan, rootPath, file, s -> {
                        s.totalSize += size;
                        s.fileCount++;
                    });
                    offerLargest(scan, file.toString(), size);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.debug("Cannot access {}: {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
        return scan;
    }

    private void accumulateChild(Path rootPath, Path child, ScanResult scan) {
        try {
            if (Files.isSymbolicLink(child) || isHidden(child)) {
                return;
            }
            if (Files.isDirectory(child)) {
                scan.rootStats.directoryCount++;
            } else {
                long size = Files.size(child);
                scan.rootStats.totalSize += size;
                scan.rootStats.fileCount++;
                offerLargest(scan, child.toString(), size);
            }
        } catch (IOException e) {
            logger.debug("Skipping unreadable entry {}: {}", child, e.getMessage());
        }
    }

    /**
     * Apply {@code action} to the DirectoryStats of every ancestor of
     * {@code node}, from its parent directory up to and including rootPath.
     */
    private void accumulateOnAncestors(ScanResult scan, Path rootPath, Path node,
                                       Consumer<DirectoryStats> action) {
        Path dir = node.getParent();
        while (dir != null && dir.startsWith(rootPath)) {
            DirectoryStats stats = scan.directoryStats.get(dir.toString());
            if (stats != null) {
                action.accept(stats);
            }
            if (dir.equals(rootPath)) {
                break;
            }
            dir = dir.getParent();
        }
    }

    /** Keep only the TOP_FILES_LIMIT largest files - memory stays bounded. */
    private void offerLargest(ScanResult scan, String path, long size) {
        scan.largestFiles.offer(new FileInfo(path, size));
        if (scan.largestFiles.size() > TOP_FILES_LIMIT) {
            scan.largestFiles.poll();
        }
    }

    private boolean isHidden(Path path) {
        Path name = path.getFileName();
        return name != null && name.toString().startsWith(".");
    }

    private static class ScanResult {
        final Map<String, DirectoryStats> directoryStats = new HashMap<>();
        /** Min-heap of the largest files seen so far, capped at TOP_FILES_LIMIT. */
        final PriorityQueue<FileInfo> largestFiles =
                new PriorityQueue<>(Comparator.comparingLong(f -> f.size));
        final DirectoryStats rootStats = new DirectoryStats();
    }

    private static class DirectoryStats {
        long totalSize = 0;
        int fileCount = 0;
        int directoryCount = 0;
    }

    private static class FileInfo {
        final String path;
        final long size;

        FileInfo(String path, long size) {
            this.path = path;
            this.size = size;
        }
    }
}
