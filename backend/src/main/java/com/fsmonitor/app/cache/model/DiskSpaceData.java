package com.fsmonitor.app.cache.model;

import java.time.LocalDateTime;

public class DiskSpaceData {
    private Long totalBytes;
    private Long usableBytes;
    private LocalDateTime scannedAt;

    public Long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(Long totalBytes) { this.totalBytes = totalBytes; }

    public Long getUsableBytes() { return usableBytes; }
    public void setUsableBytes(Long usableBytes) { this.usableBytes = usableBytes; }

    public LocalDateTime getScannedAt() { return scannedAt; }
    public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }
}
