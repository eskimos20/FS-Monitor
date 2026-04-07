package com.fsmonitor.app.service;

import com.fsmonitor.app.dto.ProcessInfo;
import com.fsmonitor.app.dto.SystemStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

@Service
public class SystemMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(SystemMonitoringService.class);
    
    private record MemoryInfo(long total, long available) {}

    public SystemStats getSystemStats() {
        SystemStats stats = new SystemStats();
        
        try {
            // Get CPU and memory usage using modern Java 17+ method
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
            
            int availableProcessors = sunOsBean.getAvailableProcessors();
            stats.setAvailableProcessors(availableProcessors);
            
            // CPU usage
            double cpuUsage = sunOsBean.getCpuLoad() * 100;
            stats.setCpuUsage(cpuUsage >= 0 ? cpuUsage : 0);
            
            // Calculate CPU usage per core
            double cpuPerCore = availableProcessors > 0 ? cpuUsage / availableProcessors : 0;
            stats.setCpuPerCore(cpuPerCore);
            
            // Get CPU usage per individual core
            List<Double> cpuPerCoreList = getCpuPerCore(availableProcessors);
            stats.setCpuPerCoreList(cpuPerCoreList);
            
            // Get memory usage from /proc/meminfo (Linux specific, non-deprecated)
            MemoryInfo memInfo = getMemoryInfo();
            long totalMemory = memInfo.total;
            long freeMemory = memInfo.available;
            long usedMemory = totalMemory - freeMemory;
            
            stats.setTotalMemory(totalMemory);
            stats.setFreeMemory(freeMemory);
            stats.setUsedMemory(usedMemory);
            stats.setMemoryUsagePercent((double) usedMemory / totalMemory * 100);
            
            // Get process count and top processes
            List<ProcessInfo> processes = getTopProcesses();
            stats.setTopProcesses(processes);
            stats.setProcessCount(getTotalProcessCount());
            
        } catch (Exception e) {
            logger.error("Error getting system stats", e);
        }
        
        return stats;
    }

    private List<ProcessInfo> getTopProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();
        
        try {
            // Use ps command to get top processes by CPU usage
            ProcessBuilder pb = new ProcessBuilder(
                "ps", "aux", "--sort=-pcpu", "--no-headers"
            );
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 11) {
                    ProcessInfo info = new ProcessInfo();
                    info.setUser(parts[0]);
                    info.setPid(parts[1]);
                    info.setCpuUsage(parseDouble(parts[2]));
                    
                    double memoryPercent = parseDouble(parts[3]);
                    info.setMemoryUsage(memoryPercent);
                    
                    // Get RSS (Resident Set Size) in KB from column 5 and convert to MB
                    // ps aux format: USER PID %CPU %MEM VSZ RSS TTY STAT START TIME COMMAND
                    long rssKB = parseLong(parts[5]);
                    long memoryMB = rssKB / 1024;
                    info.setMemoryMB(memoryMB);
                    
                    // Get command (join remaining parts)
                    StringBuilder cmd = new StringBuilder();
                    for (int i = 10; i < parts.length; i++) {
                        cmd.append(parts[i]).append(" ");
                    }
                    String command = cmd.toString().trim();
                    info.setCommand(command.length() > 100 ? command.substring(0, 100) + "..." : command);
                    
                    // Extract process name from command
                    String processName = parts[10];
                    if (processName.contains("/")) {
                        processName = processName.substring(processName.lastIndexOf("/") + 1);
                    }
                    info.setName(processName);
                    
                    processes.add(info);
                }
            }
            
            process.waitFor();
            reader.close();
            
        } catch (Exception e) {
            logger.error("Error getting top processes", e);
        }
        
        return processes;
    }

    private int getTotalProcessCount() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ps", "aux", "--no-headers");
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            int count = 0;
            while (reader.readLine() != null) {
                count++;
            }
            
            process.waitFor();
            reader.close();
            
            return count;
        } catch (Exception e) {
            logger.error("Error getting process count", e);
            return 0;
        }
    }

    private List<Double> getCpuPerCore(int numCores) {
        List<Double> cpuPerCore = new ArrayList<>();
        
        try {
            // Read /proc/stat twice with a delay to calculate actual usage
            List<long[]> firstRead = readProcStat(numCores);
            Thread.sleep(100); // 100ms delay
            List<long[]> secondRead = readProcStat(numCores);
            
            // Calculate usage for each core
            for (int i = 0; i < Math.min(firstRead.size(), secondRead.size()); i++) {
                long[] first = firstRead.get(i);
                long[] second = secondRead.get(i);
                
                long totalDiff = (second[0] + second[1] + second[2] + second[3]) - 
                                 (first[0] + first[1] + first[2] + first[3]);
                long activeDiff = (second[0] + second[1] + second[2]) - 
                                  (first[0] + first[1] + first[2]);
                
                double usage = totalDiff > 0 ? (activeDiff * 100.0 / totalDiff) : 0.0;
                cpuPerCore.add(Math.max(0.0, Math.min(100.0, usage)));
            }
            
        } catch (Exception e) {
            logger.debug("Error reading CPU per core: {}", e.getMessage());
        }
        
        // Fallback: if reading failed or returned no data, create list with zeros
        if (cpuPerCore.isEmpty()) {
            for (int i = 0; i < numCores; i++) {
                cpuPerCore.add(0.0);
            }
        }
        
        // Ensure we don't return more cores than expected
        while (cpuPerCore.size() > numCores) {
            cpuPerCore.remove(cpuPerCore.size() - 1);
        }
        
        return cpuPerCore;
    }

    private List<long[]> readProcStat(int numCores) throws Exception {
        List<long[]> coreStats = new ArrayList<>();
        
        ProcessBuilder pb = new ProcessBuilder("cat", "/proc/stat");
        Process process = pb.start();
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );
        
        String line;
        while ((line = reader.readLine()) != null && coreStats.size() < numCores) {
            // Lines starting with "cpu" followed by a number are individual cores
            if (line.startsWith("cpu") && !line.startsWith("cpu ")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 5) {
                    try {
                        // Format: cpuN user nice system idle iowait irq softirq...
                        long user = Long.parseLong(parts[1]);
                        long nice = Long.parseLong(parts[2]);
                        long system = Long.parseLong(parts[3]);
                        long idle = Long.parseLong(parts[4]);
                        
                        coreStats.add(new long[]{user, nice, system, idle});
                    } catch (NumberFormatException e) {
                        logger.debug("Error parsing /proc/stat line: {}", line);
                    }
                }
            }
        }
        
        process.waitFor();
        reader.close();
        
        return coreStats;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private MemoryInfo getMemoryInfo() {
        try {
            ProcessBuilder pb = new ProcessBuilder("cat", "/proc/meminfo");
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            long totalMemory = 0;
            long availableMemory = 0;
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    totalMemory = parseLong(line.split("\\s+")[1]) * 1024; // Convert KB to bytes
                } else if (line.startsWith("MemAvailable:")) {
                    availableMemory = parseLong(line.split("\\s+")[1]) * 1024; // Convert KB to bytes
                }
                
                // Break if we have both values
                if (totalMemory > 0 && availableMemory > 0) {
                    break;
                }
            }
            
            process.waitFor();
            reader.close();
            
            return new MemoryInfo(totalMemory, availableMemory);
            
        } catch (Exception e) {
            logger.error("Error reading memory info from /proc/meminfo", e);
            // Fallback: return zeros
            return new MemoryInfo(0, 0);
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
