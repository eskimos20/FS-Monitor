package com.fsmonitor.app.cache.model;

import java.time.LocalDateTime;

public class LogMatchResultData {
    private Long logConfigId;
    private String fileName;
    private String keyword;
    private int lineNumber;
    private String matchedLine;
    private String contextBefore;
    private String contextAfter;
    private LocalDateTime foundAt;

    public Long getLogConfigId() { return logConfigId; }
    public void setLogConfigId(Long logConfigId) { this.logConfigId = logConfigId; }

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
