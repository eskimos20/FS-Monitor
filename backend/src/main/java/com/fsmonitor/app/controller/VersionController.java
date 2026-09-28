package com.fsmonitor.app.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    private static final long SERVER_START_TIME = System.currentTimeMillis();

    @Value("${application.version:1.0.0}")
    private String version;

    @GetMapping
    public Map<String, Object> getVersion() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", version);
        response.put("serverStartTime", SERVER_START_TIME);
        return response;
    }
}
