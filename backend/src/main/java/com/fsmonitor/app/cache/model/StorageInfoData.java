package com.fsmonitor.app.cache.model;

import java.time.LocalDateTime;

public class StorageInfoData {
    private String path;
    private Long totalSizeBytes;
    private Long freeSpaceBytes;
    private Integer fileCount;
    private Integer directoryCount;
    private LocalDateTime scannedAt;

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Long getTotalSizeBytes() { return totalSizeBytes; }
    public void setTotalSizeBytes(Long totalSizeBytes) { this.totalSizeBytes = totalSizeBytes; }

    public Long getFreeSpaceBytes() { return freeSpaceBytes; }
    public void setFreeSpaceBytes(Long freeSpaceBytes) { this.freeSpaceBytes = freeSpaceBytes; }

    public Integer getFileCount() { return fileCount; }
    public void setFileCount(Integer fileCount) { this.fileCount = fileCount; }

    public Integer getDirectoryCount() { return directoryCount; }
    public void setDirectoryCount(Integer directoryCount) { this.directoryCount = directoryCount; }

    public LocalDateTime getScannedAt() { return scannedAt; }
    public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }
}
