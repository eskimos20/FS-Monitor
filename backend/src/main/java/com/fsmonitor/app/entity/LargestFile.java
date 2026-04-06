package com.fsmonitor.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "largest_files")
public class LargestFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "storage_config_id", nullable = false)
    private StorageConfig storageConfig;
    
    @Column(nullable = false, length = 1000)
    private String filePath;
    
    @Column(nullable = false)
    private Long sizeBytes;
    
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
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getSizeBytes() {
        return sizeBytes;
    }
    
    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    
    public LocalDateTime getScannedAt() {
        return scannedAt;
    }
    
    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }
}
