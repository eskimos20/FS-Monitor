package com.fsmonitor.app.dto;

import com.fsmonitor.app.entity.LogConfig;

import java.time.LocalDateTime;

public record LogConfigResponse(
        Long id,
        String name,
        String path,
        String fileTypes,
        String keywords,
        Boolean active,
        Integer checkIntervalMinutes,
        Boolean recursive,
        LocalDateTime lastCheck,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static LogConfigResponse from(LogConfig config) {
        return new LogConfigResponse(
                config.getId(),
                config.getName(),
                config.getPath(),
                config.getFileTypes(),
                config.getKeywords(),
                config.isActive(),
                config.getCheckIntervalMinutes(),
                config.isRecursive(),
                config.getLastCheck(),
                config.getCreatedAt(),
                config.getUpdatedAt());
    }
}
