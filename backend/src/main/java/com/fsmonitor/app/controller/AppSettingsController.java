package com.fsmonitor.app.controller;

import com.fsmonitor.app.dto.AppSettingsResponse;
import com.fsmonitor.app.entity.AppSettings;
import com.fsmonitor.app.service.AppSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app-settings")
public class AppSettingsController {
    
    private final AppSettingsService appSettingsService;

    public AppSettingsController(AppSettingsService appSettingsService) {
        this.appSettingsService = appSettingsService;
    }
    
    @GetMapping
    public ResponseEntity<AppSettingsResponse> getCurrentAppSettings() {
        return ResponseEntity.ok(AppSettingsResponse.from(appSettingsService.getCurrentAppSettings()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppSettingsResponse> saveAppSettings(@RequestBody AppSettings appSettings) {
        return ResponseEntity.ok(AppSettingsResponse.from(appSettingsService.saveAppSettings(appSettings)));
    }
}
