package com.fsmonitor.app.controller;

import com.fsmonitor.app.entity.AppSettings;
import com.fsmonitor.app.service.AppSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app-settings")
public class AppSettingsController {
    
    @Autowired
    private AppSettingsService appSettingsService;
    
    @GetMapping
    public ResponseEntity<AppSettings> getCurrentAppSettings() {
        AppSettings appSettings = appSettingsService.getCurrentAppSettings();
        return ResponseEntity.ok(appSettings);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppSettings> saveAppSettings(@RequestBody AppSettings appSettings) {
        AppSettings savedSettings = appSettingsService.saveAppSettings(appSettings);
        return ResponseEntity.ok(savedSettings);
    }
}
