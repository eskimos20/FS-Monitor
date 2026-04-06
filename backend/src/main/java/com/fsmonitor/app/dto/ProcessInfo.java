package com.fsmonitor.app.dto;

public class ProcessInfo {
    private String pid;
    private String name;
    private double cpuUsage;
    private double memoryUsage;
    private long memoryMB;
    private String user;
    private String command;

    public ProcessInfo() {}

    public ProcessInfo(String pid, String name, double cpuUsage, double memoryUsage, long memoryMB, String user, String command) {
        this.pid = pid;
        this.name = name;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.memoryMB = memoryMB;
        this.user = user;
        this.command = command;
    }

    // Getters and Setters
    public String getPid() { return pid; }
    public void setPid(String pid) { this.pid = pid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }

    public long getMemoryMB() { return memoryMB; }
    public void setMemoryMB(long memoryMB) { this.memoryMB = memoryMB; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
}
