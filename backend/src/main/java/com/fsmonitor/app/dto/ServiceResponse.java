package com.fsmonitor.app.dto;

import com.fsmonitor.app.entity.CheckMethod;
import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.entity.ServiceType;

import java.time.LocalDateTime;

/**
 * API representation of a monitored service. Password and private key material are
 * deliberately absent - only the {@code passwordSet}/{@code privateKeySet} flags are exposed.
 */
public record ServiceResponse(
        Long id,
        String name,
        ServiceType type,
        CheckMethod checkMethod,
        String host,
        Integer port,
        String path,
        Boolean useCredentials,
        String username,
        String sharePath,
        Boolean isActive,
        Integer checkIntervalMinutes,
        Boolean scheduleEnabled,
        String activeDays,
        Integer activeStartHour,
        Integer activeEndHour,
        String lastError,
        Boolean notificationSent,
        LocalDateTime notificationSentAt,
        LocalDateTime lastCheck,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean passwordSet,
        Boolean privateKeySet) {

    public static ServiceResponse from(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getType(),
                service.getCheckMethod(),
                service.getHost(),
                service.getPort(),
                service.getPath(),
                service.getUseCredentials(),
                service.getUsername(),
                service.getSharePath(),
                service.getIsActive(),
                service.getCheckIntervalMinutes(),
                service.getScheduleEnabled(),
                service.getActiveDays(),
                service.getActiveStartHour(),
                service.getActiveEndHour(),
                service.getLastError(),
                service.getNotificationSent(),
                service.getNotificationSentAt(),
                service.getLastCheck(),
                service.getCreatedAt(),
                service.getUpdatedAt(),
                service.isPasswordSet(),
                service.isPrivateKeySet());
    }
}
