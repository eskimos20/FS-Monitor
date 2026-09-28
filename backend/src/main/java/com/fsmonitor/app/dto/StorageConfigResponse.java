package com.fsmonitor.app.dto;

import com.fsmonitor.app.entity.StorageConfig;

import java.time.LocalDateTime;

public record StorageConfigResponse(
        Long id,
        String name,
        String path,
        Boolean recursive,
        Integer checkIntervalMinutes,
        String intervalUnit,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static StorageConfigResponse from(StorageConfig config) {
        return new StorageConfigResponse(
                config.getId(),
                config.getName(),
                config.getPath(),
                config.getRecursive(),
                config.getCheckIntervalMinutes(),
                config.getIntervalUnit(),
                config.getActive(),
                config.getCreatedAt(),
                config.getUpdatedAt());
    }
}
