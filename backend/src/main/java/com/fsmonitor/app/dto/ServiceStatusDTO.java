package com.fsmonitor.app.dto;

import com.fsmonitor.app.entity.ServiceStatus;
import java.time.LocalDateTime;

public class ServiceStatusDTO {
    private Long id;
    private String name;
    private ServiceStatus status;
    private String lastError;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime lastSuccessfulCheck;
    private Boolean isActive;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public ServiceStatus getStatus() { return status; }
    public void setStatus(ServiceStatus status) { this.status = status; }
    
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    
    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(LocalDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
    
    public LocalDateTime getLastSuccessfulCheck() { return lastSuccessfulCheck; }
    public void setLastSuccessfulCheck(LocalDateTime lastSuccessfulCheck) { this.lastSuccessfulCheck = lastSuccessfulCheck; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
