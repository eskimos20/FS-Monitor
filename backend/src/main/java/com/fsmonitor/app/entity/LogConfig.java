package com.fsmonitor.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_configs")
public class LogConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String fileTypes;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String keywords;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "check_interval_minutes")
    private Integer checkIntervalMinutes = 5;

    @Column(name = "recursive")
    private Boolean recursive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_check")
    private LocalDateTime lastCheck;

    @Column(name = "last_match_count")
    private int lastMatchCount = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getFileTypes() { return fileTypes; }
    public void setFileTypes(String fileTypes) { this.fileTypes = fileTypes; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastCheck() { return lastCheck; }
    public void setLastCheck(LocalDateTime lastCheck) { this.lastCheck = lastCheck; }

    public int getLastMatchCount() { return lastMatchCount; }
    public void setLastMatchCount(int lastMatchCount) { this.lastMatchCount = lastMatchCount; }

    public Integer getCheckIntervalMinutes() { return checkIntervalMinutes != null ? checkIntervalMinutes : 5; }
    public void setCheckIntervalMinutes(Integer checkIntervalMinutes) { this.checkIntervalMinutes = checkIntervalMinutes; }

    public boolean isRecursive() { return recursive != null ? recursive : true; }
    public void setRecursive(Boolean recursive) { this.recursive = recursive; }
}
