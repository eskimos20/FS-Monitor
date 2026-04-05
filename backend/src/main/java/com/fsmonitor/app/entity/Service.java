package com.fsmonitor.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "services")
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType type;

    @Column(nullable = false)
    private String host;

    private Integer port;

    private String path;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer checkIntervalMinutes = 5;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_successful_check")
    private LocalDateTime lastSuccessfulCheck;

    @Enumerated(EnumType.STRING)
    private ServiceStatus status = ServiceStatus.UNKNOWN;

    private String lastError;

    // Constructors
    public Service() {}

    public Service(String name, ServiceType type, String host) {
        this.name = name;
        this.type = type;
        this.host = host;
        setDefaultPortAndPath();
    }

    private void setDefaultPortAndPath() {
        switch (type) {
            case WEB:
                this.port = 80;
                this.path = "/";
                break;
            case FTP:
                this.port = 21;
                break;
            case SFTP:
                this.port = 22;
                break;
            case SMB:
                this.port = 445;
                break;
            case PING:
                // No port needed for ping
                break;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ServiceType getType() { return type; }
    public void setType(ServiceType type) { 
        this.type = type;
        setDefaultPortAndPath();
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getCheckIntervalMinutes() { return checkIntervalMinutes; }
    public void setCheckIntervalMinutes(Integer checkIntervalMinutes) { this.checkIntervalMinutes = checkIntervalMinutes; }

    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(LocalDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public LocalDateTime getLastSuccessfulCheck() { return lastSuccessfulCheck; }
    public void setLastSuccessfulCheck(LocalDateTime lastSuccessfulCheck) { this.lastSuccessfulCheck = lastSuccessfulCheck; }

    public ServiceStatus getStatus() { return status; }
    public void setStatus(ServiceStatus status) { this.status = status; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
