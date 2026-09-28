package com.fsmonitor.app.service;

import com.fsmonitor.app.dto.ProcessInfo;
import com.fsmonitor.app.dto.SystemStats;
import com.fsmonitor.app.util.ShellCommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects host CPU, memory and process statistics. procfs files are read
 * directly with java.nio; the only external command is a single "ps" call
 * executed without a shell and with a timeout.
 */
@Service
public class SystemMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(SystemMonitoringService.class);
    private static final Path PROC_MEMINFO = Path.of("/proc/meminfo");
    private static final Path PROC_STAT = Path.of("/proc/stat");
    private static final long PS_TIMEOUT_SECONDS = 10;

    private record MemoryInfo(long total, long available) {}

    public SystemStats getSystemStats() {
        SystemStats stats = new SystemStats();

        try {
            com.sun.management.OperatingSystemMXBean osBean =
                ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);

            int availableProcessors = osBean.getAvailableProcessors();
            stats.setAvailableProcessors(availableProcessors);

            double cpuUsage = osBean.getCpuLoad() * 100;
            stats.setCpuUsage(cpuUsage >= 0 ? cpuUsage : 0);
            stats.setCpuPerCore(availableProcessors > 0 ? cpuUsage / availableProcessors : 0);
            stats.setCpuPerCoreList(getCpuPerCore(availableProcessors));

            MemoryInfo memInfo = getMemoryInfo();
            long totalMemory = memInfo.total;
            long freeMemory = memInfo.available;
            long usedMemory = totalMemory - freeMemory;

            stats.setTotalMemory(totalMemory);
            stats.setFreeMemory(freeMemory);
            stats.setUsedMemory(usedMemory);
            stats.setMemoryUsagePercent(totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0);

            List<ProcessInfo> processes = getProcessList();
            stats.setTopProcesses(processes.stream().limit(20).toList());
            stats.setProcessCount(processes.size());

        } catch (Exception e) {
            logger.error("Error getting system stats", e);
        }

        return stats;
    }

    /**
     * Single "ps" invocation - the result is used both for the top-processes
     * list and the total process count.
     */
    private List<ProcessInfo> getProcessList() {
        List<ProcessInfo> processes = new ArrayList<>();

        try {
            ShellCommandUtil.CommandResult result = ShellCommandUtil.execute(
                    List.of("ps", "aux", "--sort=-pcpu", "--no-headers"), PS_TIMEOUT_SECONDS);

            for (String line : result.output()) {
                String[] parts = line.trim().split("\\s+");
                // ps aux: USER PID %CPU %MEM VSZ RSS TTY STAT START TIME COMMAND
                if (parts.length < 11) {
                    continue;
                }

                ProcessInfo info = new ProcessInfo();
                info.setUser(parts[0]);
                info.setPid(parts[1]);
                info.setCpuUsage(parseDouble(parts[2]));
                info.setMemoryUsage(parseDouble(parts[3]));
                info.setMemoryMB(parseLong(parts[5]) / 1024); // RSS (KB) -> MB

                String command = String.join(" ", java.util.Arrays.asList(parts).subList(10, parts.length));
                info.setCommand(command.length() > 100 ? command.substring(0, 100) + "..." : command);

                String processName = parts[10];
                if (processName.contains("/")) {
                    processName = processName.substring(processName.lastIndexOf("/") + 1);
                }
                info.setName(processName);

                processes.add(info);
            }
        } catch (Exception e) {
            logger.error("Error getting process list", e);
        }

        return processes;
    }

    /**
     * Per-core CPU usage derived from two /proc/stat samples taken 100ms apart.
     */
    private List<Double> getCpuPerCore(int numCores) {
        List<Double> cpuPerCore = new ArrayList<>();

        try {
            List<long[]> firstRead = readProcStat(numCores);
            Thread.sleep(100);
            List<long[]> secondRead = readProcStat(numCores);

            for (int i = 0; i < Math.min(firstRead.size(), secondRead.size()); i++) {
                long[] first = firstRead.get(i);
                long[] second = secondRead.get(i);

                long totalDiff = (second[0] + second[1] + second[2] + second[3])
                               - (first[0] + first[1] + first[2] + first[3]);
                long activeDiff = (second[0] + second[1] + second[2])
                                - (first[0] + first[1] + first[2]);

                double usage = totalDiff > 0 ? (activeDiff * 100.0 / totalDiff) : 0.0;
                cpuPerCore.add(Math.max(0.0, Math.min(100.0, usage)));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.debug("Error reading CPU per core: {}", e.getMessage());
        }

        while (cpuPerCore.size() < numCores) {
            cpuPerCore.add(0.0);
        }
        return cpuPerCore.subList(0, Math.min(cpuPerCore.size(), numCores));
    }

    private List<long[]> readProcStat(int numCores) throws IOException {
        List<long[]> coreStats = new ArrayList<>();

        for (String line : Files.readAllLines(PROC_STAT)) {
            if (coreStats.size() >= numCores) {
                break;
            }
            // "cpuN ..." lines are individual cores (skip the aggregate "cpu " line)
            if (line.startsWith("cpu") && !line.startsWith("cpu ")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 5) {
                    try {
                        // cpuN user nice system idle [iowait irq softirq...]
                        coreStats.add(new long[]{
                            Long.parseLong(parts[1]),
                            Long.parseLong(parts[2]),
                            Long.parseLong(parts[3]),
                            Long.parseLong(parts[4])
                        });
                    } catch (NumberFormatException e) {
                        logger.debug("Error parsing /proc/stat line: {}", line);
                    }
                }
            }
        }
        return coreStats;
    }

    private MemoryInfo getMemoryInfo() {
        try {
            long totalMemory = 0;
            long availableMemory = 0;

            for (String line : Files.readAllLines(PROC_MEMINFO)) {
                if (line.startsWith("MemTotal:")) {
                    totalMemory = parseLong(line.split("\\s+")[1]) * 1024;
                } else if (line.startsWith("MemAvailable:")) {
                    availableMemory = parseLong(line.split("\\s+")[1]) * 1024;
                }
                if (totalMemory > 0 && availableMemory > 0) {
                    break;
                }
            }
            return new MemoryInfo(totalMemory, availableMemory);
        } catch (IOException e) {
            logger.error("Error reading memory info from /proc/meminfo", e);
            return new MemoryInfo(0, 0);
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
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
