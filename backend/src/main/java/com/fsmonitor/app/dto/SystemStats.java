package com.fsmonitor.app.dto;

import java.util.List;

public class SystemStats {
    private double cpuUsage;
    private double cpuPerCore;
    private int availableProcessors;
    private List<Double> cpuPerCoreList;
    private long totalMemory;
    private long usedMemory;
    private long freeMemory;
    private double memoryUsagePercent;
    private int processCount;
    private List<ProcessInfo> topProcesses;

    public SystemStats() {}

    // Getters and Setters
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public double getCpuPerCore() { return cpuPerCore; }
    public void setCpuPerCore(double cpuPerCore) { this.cpuPerCore = cpuPerCore; }

    public int getAvailableProcessors() { return availableProcessors; }
    public void setAvailableProcessors(int availableProcessors) { this.availableProcessors = availableProcessors; }

    public List<Double> getCpuPerCoreList() { return cpuPerCoreList; }
    public void setCpuPerCoreList(List<Double> cpuPerCoreList) { this.cpuPerCoreList = cpuPerCoreList; }

    public long getTotalMemory() { return totalMemory; }
    public void setTotalMemory(long totalMemory) { this.totalMemory = totalMemory; }

    public long getUsedMemory() { return usedMemory; }
    public void setUsedMemory(long usedMemory) { this.usedMemory = usedMemory; }

    public long getFreeMemory() { return freeMemory; }
    public void setFreeMemory(long freeMemory) { this.freeMemory = freeMemory; }

    public double getMemoryUsagePercent() { return memoryUsagePercent; }
    public void setMemoryUsagePercent(double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }

    public int getProcessCount() { return processCount; }
    public void setProcessCount(int processCount) { this.processCount = processCount; }

    public List<ProcessInfo> getTopProcesses() { return topProcesses; }
    public void setTopProcesses(List<ProcessInfo> topProcesses) { this.topProcesses = topProcesses; }
}
