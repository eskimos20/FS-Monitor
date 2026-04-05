package com.fsmonitor.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_logs")
public class NotificationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Column(nullable = false)
    private Long entityId;
    
    @Column(nullable = false)
    private String entityName;
    
    @Column(name = "sent_at")
    private java.time.LocalDateTime sentAt;
    
    @PrePersist
    protected void onCreate() {
        sentAt = java.time.LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    
    public java.time.LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(java.time.LocalDateTime sentAt) { this.sentAt = sentAt; }
}
