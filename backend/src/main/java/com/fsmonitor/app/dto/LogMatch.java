package com.fsmonitor.app.dto;

import java.util.List;

public class LogMatch {
    private String fileName;
    private String keyword;
    private int lineNumber;
    private String matchedLine;
    private List<String> contextBefore;
    private List<String> contextAfter;
    private String timestamp;

    public LogMatch() {}

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public String getMatchedLine() { return matchedLine; }
    public void setMatchedLine(String matchedLine) { this.matchedLine = matchedLine; }

    public List<String> getContextBefore() { return contextBefore; }
    public void setContextBefore(List<String> contextBefore) { this.contextBefore = contextBefore; }

    public List<String> getContextAfter() { return contextAfter; }
    public void setContextAfter(List<String> contextAfter) { this.contextAfter = contextAfter; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
