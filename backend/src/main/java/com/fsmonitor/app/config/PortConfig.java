package com.fsmonitor.app.config;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class PortConfig {
    private int frontendPort;
    private int backendPort;
    private String frontendHost = "localhost";
    private String backendHost = "localhost";

    public PortConfig() {
        loadConfig();
    }

    private void loadConfig() {
        // Set default values
        this.frontendPort = 3000;
        this.backendPort = 8080;
        
        try {
            // Look for config.json in project root
            Path configPath = Paths.get("config.json");
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);
                
                // Simple JSON parsing (without external libraries)
                String profile = System.getProperty("spring.profiles.active", "development");
                
                // Extract backend port from JSON
                String backendPortPattern = "\"" + profile + "\".*\"backend\".*\"port\"\\s*:\\s*(\\d+)";
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(backendPortPattern, java.util.regex.Pattern.DOTALL);
                java.util.regex.Matcher matcher = pattern.matcher(content);
                
                if (matcher.find()) {
                    this.backendPort = Integer.parseInt(matcher.group(1));
                }
                
                // Extract frontend port
                String frontendPortPattern = "\"" + profile + "\".*\"frontend\".*\"port\"\\s*:\\s*(\\d+)";
                pattern = java.util.regex.Pattern.compile(frontendPortPattern, java.util.regex.Pattern.DOTALL);
                matcher = pattern.matcher(content);
                
                if (matcher.find()) {
                    this.frontendPort = Integer.parseInt(matcher.group(1));
                }
                
                System.out.println("Loaded ports from config.json - Backend: " + backendPort + ", Frontend: " + frontendPort);
            }
        } catch (IOException e) {
            System.out.println("Could not load config.json, using default ports - Backend: " + backendPort + ", Frontend: " + frontendPort);
        }
    }

    public int getFrontendPort() {
        return frontendPort;
    }

    public int getBackendPort() {
        return backendPort;
    }

    public String getFrontendHost() {
        return frontendHost;
    }

    public String getBackendHost() {
        return backendHost;
    }
}
