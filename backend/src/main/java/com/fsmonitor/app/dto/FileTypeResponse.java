package com.fsmonitor.app.dto;

import com.fsmonitor.app.entity.FileType;

public record FileTypeResponse(Long id, String extension, String description, Boolean isActive) {

    public static FileTypeResponse from(FileType fileType) {
        return new FileTypeResponse(
                fileType.getId(),
                fileType.getExtension(),
                fileType.getDescription(),
                fileType.getIsActive());
    }
}
