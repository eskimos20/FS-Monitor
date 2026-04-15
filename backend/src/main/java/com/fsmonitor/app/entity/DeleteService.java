package com.fsmonitor.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "delete_services")
public class DeleteService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path;

    @Column(name = "file_types")
    private String fileTypes;

    @Column(name = "cleanup_enabled", nullable = false)
    private Boolean cleanupEnabled = true;

    @Column(name = "cleanup_interval_value", nullable = false)
    private Integer cleanupIntervalValue = 1;

    @Column(name = "cleanup_interval_unit", nullable = false)
    @Enumerated(EnumType.STRING)
    private TimeInterval cleanupIntervalUnit = TimeInterval.HOURS;

    @Column(name = "delete_age_value", nullable = false)
    private Integer deleteAgeValue = 30;

    @Column(name = "delete_age_unit", nullable = false)
    @Enumerated(EnumType.STRING)
    private TimeInterval deleteAgeUnit = TimeInterval.DAYS;

    @Column(name = "recursive")
    private Boolean recursive = true;

    @Column(name = "delete_empty_directories")
    private Boolean deleteEmptyDirectories = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_cleanup")
    private LocalDateTime lastCleanup;

    @Column(name = "files_deleted_last_scan")
    private Integer filesDeletedLastScan = 0;

    @Column(name = "folders_deleted_last_scan")
    private Integer foldersDeletedLastScan = 0;

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

    public Boolean getCleanupEnabled() { return cleanupEnabled; }
    public void setCleanupEnabled(Boolean cleanupEnabled) { this.cleanupEnabled = cleanupEnabled; }

    public Integer getCleanupIntervalValue() { return cleanupIntervalValue; }
    public void setCleanupIntervalValue(Integer cleanupIntervalValue) { this.cleanupIntervalValue = cleanupIntervalValue; }

    public TimeInterval getCleanupIntervalUnit() { return cleanupIntervalUnit; }
    public void setCleanupIntervalUnit(TimeInterval cleanupIntervalUnit) { this.cleanupIntervalUnit = cleanupIntervalUnit; }

    public Long getCleanupIntervalMinutes() {
        return cleanupIntervalUnit.toMinutes(cleanupIntervalValue);
    }

    public Integer getDeleteAgeValue() { return deleteAgeValue; }
    public void setDeleteAgeValue(Integer deleteAgeValue) { this.deleteAgeValue = deleteAgeValue; }

    public TimeInterval getDeleteAgeUnit() { return deleteAgeUnit; }
    public void setDeleteAgeUnit(TimeInterval deleteAgeUnit) { this.deleteAgeUnit = deleteAgeUnit; }

    public Long getDeleteAgeMinutes() {
        return deleteAgeUnit.toMinutes(deleteAgeValue);
    }

    public Boolean getRecursive() { return recursive; }
    public void setRecursive(Boolean recursive) { this.recursive = recursive; }

    public Boolean getDeleteEmptyDirectories() { return deleteEmptyDirectories; }
    public void setDeleteEmptyDirectories(Boolean deleteEmptyDirectories) { this.deleteEmptyDirectories = deleteEmptyDirectories; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastCleanup() { return lastCleanup; }
    public void setLastCleanup(LocalDateTime lastCleanup) { this.lastCleanup = lastCleanup; }

    public Integer getFilesDeletedLastScan() { return filesDeletedLastScan; }
    public void setFilesDeletedLastScan(Integer filesDeletedLastScan) { this.filesDeletedLastScan = filesDeletedLastScan; }

    public Integer getFoldersDeletedLastScan() { return foldersDeletedLastScan; }
    public void setFoldersDeletedLastScan(Integer foldersDeletedLastScan) { this.foldersDeletedLastScan = foldersDeletedLastScan; }
}
