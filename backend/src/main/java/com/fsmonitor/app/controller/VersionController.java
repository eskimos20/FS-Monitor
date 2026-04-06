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

    @Value("${application.version:1.0.0}")
    private String version;

    @GetMapping
    public Map<String, String> getVersion() {
        Map<String, String> response = new HashMap<>();
        response.put("version", version);
        return response;
    }
}
