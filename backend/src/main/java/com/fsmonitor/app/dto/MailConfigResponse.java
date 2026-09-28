package com.fsmonitor.app.dto;

import com.fsmonitor.app.entity.MailConfig;

import java.time.LocalDateTime;

/** API representation of the mail config - password is never exposed, only the {@code passwordSet} flag. */
public record MailConfigResponse(
        Long id,
        String host,
        Integer port,
        String fromEmail,
        String toEmail,
        String username,
        Boolean passwordSet,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MailConfigResponse from(MailConfig config) {
        return new MailConfigResponse(
                config.getId(),
                config.getHost(),
                config.getPort(),
                config.getFromEmail(),
                config.getToEmail(),
                config.getUsername(),
                config.isPasswordSet(),
                config.getCreatedAt(),
                config.getUpdatedAt());
    }
}
