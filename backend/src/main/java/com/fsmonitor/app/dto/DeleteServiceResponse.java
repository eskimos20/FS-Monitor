package com.fsmonitor.app.dto;

import com.fsmonitor.app.entity.DeleteService;
import com.fsmonitor.app.entity.TimeInterval;

import java.time.LocalDateTime;

public record DeleteServiceResponse(
        Long id,
        String name,
        String path,
        String fileTypes,
        Boolean cleanupEnabled,
        Integer cleanupIntervalValue,
        TimeInterval cleanupIntervalUnit,
        Long cleanupIntervalMinutes,
        Integer deleteAgeValue,
        TimeInterval deleteAgeUnit,
        Long deleteAgeMinutes,
        Boolean recursive,
        Boolean deleteEmptyDirectories,
        Boolean isActive,
        LocalDateTime lastCleanup,
        Integer filesDeletedLastScan,
        Integer foldersDeletedLastScan,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static DeleteServiceResponse from(DeleteService service) {
        return new DeleteServiceResponse(
                service.getId(),
                service.getName(),
                service.getPath(),
                service.getFileTypes(),
                service.getCleanupEnabled(),
                service.getCleanupIntervalValue(),
                service.getCleanupIntervalUnit(),
                service.getCleanupIntervalMinutes(),
                service.getDeleteAgeValue(),
                service.getDeleteAgeUnit(),
                service.getDeleteAgeMinutes(),
                service.getRecursive(),
                service.getDeleteEmptyDirectories(),
                service.getIsActive(),
                service.getLastCleanup(),
                service.getFilesDeletedLastScan(),
                service.getFoldersDeletedLastScan(),
                service.getCreatedAt(),
                service.getUpdatedAt());
    }
}
