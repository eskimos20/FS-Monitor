package com.fsmonitor.app.dto;

import com.fsmonitor.app.entity.AppSettings;

import java.time.LocalDateTime;

public record AppSettingsResponse(
        Long id,
        Integer refreshIntervalSeconds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AppSettingsResponse from(AppSettings settings) {
        return new AppSettingsResponse(
                settings.getId(),
                settings.getRefreshIntervalSeconds(),
                settings.getCreatedAt(),
                settings.getUpdatedAt());
    }
}
