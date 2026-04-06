package com.fsmonitor.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_match_results")
public class LogMatchResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "log_config_id", nullable = false)
    private LogConfig logConfig;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String keyword;

    @Column(nullable = false)
    private int lineNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String matchedLine;

    @Column(columnDefinition = "TEXT")
    private String contextBefore;

    @Column(columnDefinition = "TEXT")
    private String contextAfter;

    @Column(name = "found_at")
    private LocalDateTime foundAt;

    @PrePersist
    protected void onCreate() {
        foundAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LogConfig getLogConfig() { return logConfig; }
    public void setLogConfig(LogConfig logConfig) { this.logConfig = logConfig; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public String getMatchedLine() { return matchedLine; }
    public void setMatchedLine(String matchedLine) { this.matchedLine = matchedLine; }

    public String getContextBefore() { return contextBefore; }
    public void setContextBefore(String contextBefore) { this.contextBefore = contextBefore; }

    public String getContextAfter() { return contextAfter; }
    public void setContextAfter(String contextAfter) { this.contextAfter = contextAfter; }

    public LocalDateTime getFoundAt() { return foundAt; }
    public void setFoundAt(LocalDateTime foundAt) { this.foundAt = foundAt; }
}
