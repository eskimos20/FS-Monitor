package com.fsmonitor.app.dto;

import java.time.LocalDateTime;

public class IntegrationStatusDTO {
    private Long id;
    private String name;
    private LocalDateTime lastFileFound;
    private String lastFileName;
    private LocalDateTime lastCheckedAt;
    private Boolean isActive;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public LocalDateTime getLastFileFound() { return lastFileFound; }
    public void setLastFileFound(LocalDateTime lastFileFound) { this.lastFileFound = lastFileFound; }
    
    public String getLastFileName() { return lastFileName; }
    public void setLastFileName(String lastFileName) { this.lastFileName = lastFileName; }
    
    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(LocalDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
