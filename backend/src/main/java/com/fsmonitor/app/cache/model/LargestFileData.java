package com.fsmonitor.app.cache.model;

import java.time.LocalDateTime;

public class LargestFileData {
    private String filePath;
    private Long sizeBytes;
    private LocalDateTime scannedAt;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public LocalDateTime getScannedAt() { return scannedAt; }
    public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }
}
