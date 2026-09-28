package com.fsmonitor.app.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fsmonitor.app.util.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
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

    @Enumerated(EnumType.STRING)
    private CheckMethod checkMethod;

    @Column(nullable = false)
    private String host;

    private Integer port;

    private String path;

    @Column(name = "use_credentials")
    private Boolean useCredentials = false;

    private String username;

    @Convert(converter = EncryptedStringConverter.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "share_path")
    private String sharePath;

    @Convert(converter = EncryptedStringConverter.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "private_key", columnDefinition = "TEXT")
    private String privateKey;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer checkIntervalMinutes = 5;

    @Column(name = "schedule_enabled")
    private Boolean scheduleEnabled = false;

    @Column(name = "active_days")
    private String activeDays;

    @Column(name = "active_start_hour")
    private Integer activeStartHour = 0;

    @Column(name = "active_end_hour")
    private Integer activeEndHour = 24;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @Column(name = "last_check")
    private LocalDateTime lastCheck;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

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
            case HTTPS:
                this.port = 443;
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
            case SSH:
                this.port = 22;
                break;
            case MYSQL:
                this.port = 3306;
                break;
            case POSTGRESQL:
                this.port = 5432;
                break;
            case MONGODB:
                this.port = 27017;
                break;
            case REDIS:
                this.port = 6379;
                break;
            case MSSQL:
                this.port = 1433;
                break;
            case DNS:
                this.port = 53;
                break;
            case LDAP:
                this.port = 389;
                break;
            case RDP:
                this.port = 3389;
                break;
            case PING:
                // No port needed for ping
                break;
            case CUSTOM:
                // User specifies port, default to TCP check
                if (this.checkMethod == null) {
                    this.checkMethod = CheckMethod.TCP;
                }
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

    public CheckMethod getCheckMethod() { return checkMethod; }
    public void setCheckMethod(CheckMethod checkMethod) { this.checkMethod = checkMethod; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Boolean getUseCredentials() { return useCredentials; }
    public void setUseCredentials(Boolean useCredentials) { this.useCredentials = useCredentials; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    /** Exposes whether a password is stored without revealing it. */
    @JsonProperty("passwordSet")
    public boolean isPasswordSet() {
        return password != null && !password.isEmpty();
    }

    /** Exposes whether a private key is stored without revealing it. */
    @JsonProperty("privateKeySet")
    public boolean isPrivateKeySet() {
        return privateKey != null && !privateKey.isEmpty();
    }

    public String getSharePath() { return sharePath; }
    public void setSharePath(String sharePath) { this.sharePath = sharePath; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getCheckIntervalMinutes() { return checkIntervalMinutes; }
    public void setCheckIntervalMinutes(Integer checkIntervalMinutes) { this.checkIntervalMinutes = checkIntervalMinutes; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getScheduleEnabled() { return scheduleEnabled; }
    public void setScheduleEnabled(Boolean scheduleEnabled) { this.scheduleEnabled = scheduleEnabled; }

    public String getActiveDays() { return activeDays; }
    public void setActiveDays(String activeDays) { this.activeDays = activeDays; }

    public Integer getActiveStartHour() { return activeStartHour; }
    public void setActiveStartHour(Integer activeStartHour) { this.activeStartHour = activeStartHour; }

    public Integer getActiveEndHour() { return activeEndHour; }
    public void setActiveEndHour(Integer activeEndHour) { this.activeEndHour = activeEndHour; }

    public Boolean getNotificationSent() { return notificationSent; }
    public void setNotificationSent(Boolean notificationSent) { this.notificationSent = notificationSent; }

    public LocalDateTime getNotificationSentAt() { return notificationSentAt; }
    public void setNotificationSentAt(LocalDateTime notificationSentAt) { this.notificationSentAt = notificationSentAt; }

    public LocalDateTime getLastCheck() { return lastCheck; }
    public void setLastCheck(LocalDateTime lastCheck) { this.lastCheck = lastCheck; }
}
