package com.fsmonitor.app.dto;

import com.fsmonitor.app.entity.FileType;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.entity.ScheduleDay;
import com.fsmonitor.app.entity.TimeInterval;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record IntegrationResponse(
        Long id,
        String name,
        String path,
        Boolean monitoringEnabled,
        Integer checkIntervalValue,
        TimeInterval checkIntervalUnit,
        Integer thresholdValue,
        TimeInterval thresholdUnit,
        Long checkIntervalMinutes,
        Long thresholdMinutes,
        Boolean monitorAllFiles,
        Boolean isActive,
        Boolean scheduleEnabled,
        String activeDays,
        Integer activeStartHour,
        Integer activeEndHour,
        Boolean notificationSent,
        LocalDateTime notificationSentAt,
        LocalDateTime lastCheck,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Set<FileTypeResponse> monitoredFileTypes,
        Set<ScheduleDay> scheduleDays) {

    public static IntegrationResponse from(Integration integration) {
        Set<FileType> types = integration.getMonitoredFileTypes();
        return new IntegrationResponse(
                integration.getId(),
                integration.getName(),
                integration.getPath(),
                integration.getMonitoringEnabled(),
                integration.getCheckIntervalValue(),
                integration.getCheckIntervalUnit(),
                integration.getThresholdValue(),
                integration.getThresholdUnit(),
                integration.getCheckIntervalMinutes(),
                integration.getThresholdMinutes(),
                integration.getMonitorAllFiles(),
                integration.getIsActive(),
                integration.getScheduleEnabled(),
                integration.getActiveDays(),
                integration.getActiveStartHour(),
                integration.getActiveEndHour(),
                integration.getNotificationSent(),
                integration.getNotificationSentAt(),
                integration.getLastCheck(),
                integration.getCreatedAt(),
                integration.getUpdatedAt(),
                types == null ? Set.of() : types.stream().map(FileTypeResponse::from).collect(Collectors.toSet()),
                integration.getScheduleDays());
    }
}
