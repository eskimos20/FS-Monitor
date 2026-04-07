package com.fsmonitor.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "integrations")
public class Integration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 500)
    private String path;

    @NotNull
    @Column(name = "monitoring_enabled")
    private Boolean monitoringEnabled = true;

    @NotNull
    @Column(name = "cleanup_enabled")
    private Boolean cleanupEnabled = false;

    @NotNull
    @Column(name = "check_interval_value")
    private Integer checkIntervalValue = 5;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "check_interval_unit")
    private TimeInterval checkIntervalUnit = TimeInterval.MINUTES;

    @NotNull
    @Column(name = "threshold_value")
    private Integer thresholdValue = 15;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_unit")
    private TimeInterval thresholdUnit = TimeInterval.MINUTES;

    @NotNull
    @Column(name = "cleanup_age_value")
    private Integer cleanupAgeValue = 6;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "cleanup_age_unit")
    private TimeInterval cleanupAgeUnit = TimeInterval.MONTHS;

    @Column(name = "last_file_found")
    private LocalDateTime lastFileFound;

    @Column(name = "last_file_name")
    private String lastFileName;

    @NotNull
    @Column(name = "monitor_all_files")
    private Boolean monitorAllFiles = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "schedule_enabled")
    private Boolean scheduleEnabled = false;

    @Column(name = "active_days")
    private String activeDays;

    @Column(name = "active_start_hour")
    private Integer activeStartHour = 0;

    @Column(name = "active_end_hour")
    private Integer activeEndHour = 24;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @ManyToMany
    @JoinTable(
        name = "integration_file_types",
        joinColumns = @JoinColumn(name = "integration_id"),
        inverseJoinColumns = @JoinColumn(name = "file_type_id")
    )
    private Set<FileType> monitoredFileTypes;

    @ElementCollection
    @CollectionTable(name = "integration_schedule")
    private Set<ScheduleDay> scheduleDays;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Boolean getMonitoringEnabled() { return monitoringEnabled; }
    public void setMonitoringEnabled(Boolean monitoringEnabled) { this.monitoringEnabled = monitoringEnabled; }

    public Boolean getCleanupEnabled() { return cleanupEnabled; }
    public void setCleanupEnabled(Boolean cleanupEnabled) { this.cleanupEnabled = cleanupEnabled; }

    public Integer getCheckIntervalValue() { return checkIntervalValue; }
    public void setCheckIntervalValue(Integer checkIntervalValue) { this.checkIntervalValue = checkIntervalValue; }

    public TimeInterval getCheckIntervalUnit() { return checkIntervalUnit; }
    public void setCheckIntervalUnit(TimeInterval checkIntervalUnit) { this.checkIntervalUnit = checkIntervalUnit; }

    // Helper method for backward compatibility and calculations
    public Long getCheckIntervalMinutes() {
        return checkIntervalUnit.toMinutes(checkIntervalValue);
    }

    public Integer getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Integer thresholdValue) { this.thresholdValue = thresholdValue; }

    public TimeInterval getThresholdUnit() { return thresholdUnit; }
    public void setThresholdUnit(TimeInterval thresholdUnit) { this.thresholdUnit = thresholdUnit; }

    // Computed for backward compatibility
    public Long getThresholdMinutes() { return thresholdUnit.toMinutes(thresholdValue); }

    public Integer getCleanupAgeValue() { return cleanupAgeValue; }
    public void setCleanupAgeValue(Integer cleanupAgeValue) { this.cleanupAgeValue = cleanupAgeValue; }

    public TimeInterval getCleanupAgeUnit() { return cleanupAgeUnit; }
    public void setCleanupAgeUnit(TimeInterval cleanupAgeUnit) { this.cleanupAgeUnit = cleanupAgeUnit; }

    // Computed for backward compatibility
    public Long getCleanupAgeMinutes() { return cleanupAgeUnit.toMinutes(cleanupAgeValue); }

    public LocalDateTime getLastFileFound() { return lastFileFound; }
    public void setLastFileFound(LocalDateTime lastFileFound) { this.lastFileFound = lastFileFound; }

    public String getLastFileName() { return lastFileName; }
    public void setLastFileName(String lastFileName) { this.lastFileName = lastFileName; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(LocalDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public Boolean getMonitorAllFiles() { return monitorAllFiles; }
    public void setMonitorAllFiles(Boolean monitorAllFiles) { this.monitorAllFiles = monitorAllFiles; }

    public Set<FileType> getMonitoredFileTypes() { return monitoredFileTypes; }
    public void setMonitoredFileTypes(Set<FileType> monitoredFileTypes) { this.monitoredFileTypes = monitoredFileTypes; }

    public Set<ScheduleDay> getScheduleDays() { return scheduleDays; }
    public void setScheduleDays(Set<ScheduleDay> scheduleDays) { this.scheduleDays = scheduleDays; }

    public Boolean getScheduleEnabled() { return scheduleEnabled; }
    public void setScheduleEnabled(Boolean scheduleEnabled) { this.scheduleEnabled = scheduleEnabled; }

    public String getActiveDays() { return activeDays; }
    public void setActiveDays(String activeDays) { this.activeDays = activeDays; }

    public Integer getActiveStartHour() { return activeStartHour; }
    public void setActiveStartHour(Integer activeStartHour) { this.activeStartHour = activeStartHour; }

    public Integer getActiveEndHour() { return activeEndHour; }
    public void setActiveEndHour(Integer activeEndHour) { this.activeEndHour = activeEndHour; }
}
