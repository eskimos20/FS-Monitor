package com.fsmonitor.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "mail_configs")
public class MailConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String server;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column(nullable = false)
    private String sender;
    
    @Column(nullable = false)
    private String recipient;
    
    private String username;
    
    private String password;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }
    
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
