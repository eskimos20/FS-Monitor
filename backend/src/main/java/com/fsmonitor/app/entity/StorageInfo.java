package com.fsmonitor.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "storage_info")
public class StorageInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "storage_config_id", nullable = false)
    private StorageConfig storageConfig;
    
    @Column(nullable = false)
    private String path;
    
    @Column(nullable = false)
    private Long totalSizeBytes = 0L;
    
    @Column(nullable = false)
    private Integer fileCount = 0;
    
    @Column(nullable = false)
    private Integer directoryCount = 0;
    
    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;
    
    @PrePersist
    @PreUpdate
    protected void onSave() {
        scannedAt = LocalDateTime.now();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public StorageConfig getStorageConfig() {
        return storageConfig;
    }
    
    public void setStorageConfig(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Long getTotalSizeBytes() {
        return totalSizeBytes;
    }
    
    public void setTotalSizeBytes(Long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }
    
    public Integer getFileCount() {
        return fileCount;
    }
    
    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }
    
    public Integer getDirectoryCount() {
        return directoryCount;
    }
    
    public void setDirectoryCount(Integer directoryCount) {
        this.directoryCount = directoryCount;
    }
    
    public LocalDateTime getScannedAt() {
        return scannedAt;
    }
    
    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }
}
