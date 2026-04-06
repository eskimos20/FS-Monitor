package com.fsmonitor.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "disk_space")
public class DiskSpace {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "storage_config_id", nullable = false)
    private StorageConfig storageConfig;
    
    @Column(nullable = false)
    private Long totalBytes;
    
    @Column(nullable = false)
    private Long usableBytes;
    
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
    
    public Long getTotalBytes() {
        return totalBytes;
    }
    
    public void setTotalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
    }
    
    public Long getUsableBytes() {
        return usableBytes;
    }
    
    public void setUsableBytes(Long usableBytes) {
        this.usableBytes = usableBytes;
    }
    
    public LocalDateTime getScannedAt() {
        return scannedAt;
    }
    
    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }
}
