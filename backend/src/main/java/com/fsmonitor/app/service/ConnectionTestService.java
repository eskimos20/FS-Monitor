package com.fsmonitor.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConnectionTestService {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionTestService.class);
    private static final int TIMEOUT_MS = 3000;

    private final PingServiceChecker pingChecker;

    public ConnectionTestService(PingServiceChecker pingChecker) {
        this.pingChecker = pingChecker;
    }

    public Map<String, Object> testConnection(String host, Integer port, String path) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> methods = new ArrayList<>();
        
        logger.info("Testing connection to {}:{}", host, port);

        // Test TCP
        Map<String, Object> tcpResult = testTcp(host, port);
        methods.add(tcpResult);

        // Test HTTP (only if port suggests web service)
        if (port != null && (port == 80 || port == 443 || port == 8080 || port == 8443)) {
            Map<String, Object> httpResult = testHttp(host, port, path);
            methods.add(httpResult);
        }

        // Test PING
        Map<String, Object> pingResult = testPing(host, port);
        methods.add(pingResult);

        // Determine recommended method
        String recommended = determineRecommendedMethod(methods);
        
        result.put("methods", methods);
        result.put("recommended", recommended);
        result.put("host", host);
        result.put("port", port);

        return result;
    }

    private Map<String, Object> testTcp(String host, Integer port) {
        Map<String, Object> result = new HashMap<>();
        result.put("method", "TCP");
        result.put("name", "TCP Connection");
        result.put("description", "Simple TCP socket connection");
        
        if (port == null || port == 0) {
            result.put("success", false);
            result.put("message", "Port required for TCP test");
            result.put("responseTime", 0);
            return result;
        }

        long startTime = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            long responseTime = System.currentTimeMillis() - startTime;
            result.put("success", true);
            result.put("message", "TCP connection successful");
            result.put("responseTime", responseTime);
            logger.debug("TCP test successful: {}ms", responseTime);
        } catch (IOException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("message", "TCP connection failed: " + e.getMessage());
            result.put("responseTime", responseTime);
            logger.debug("TCP test failed: {}", e.getMessage());
        }
        
        return result;
    }

    private Map<String, Object> testHttp(String host, Integer port, String path) {
        Map<String, Object> result = new HashMap<>();
        result.put("method", "HTTP");
        result.put("name", "HTTP Request");
        result.put("description", "HTTP GET request");
        
        if (port == null) {
            port = 80;
        }
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        String protocol = (port == 443 || port == 8443) ? "https" : "http";
        String urlString = String.format("%s://%s:%d%s", protocol, host, port, path);
        
        long startTime = System.currentTimeMillis();
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            
            int responseCode = connection.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;
            
            result.put("success", responseCode >= 200 && responseCode < 400);
            result.put("message", "HTTP " + responseCode + " - " + connection.getResponseMessage());
            result.put("responseTime", responseTime);
            result.put("statusCode", responseCode);
            logger.debug("HTTP test: {} - {}ms", responseCode, responseTime);
            
            connection.disconnect();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("message", "HTTP request failed: " + e.getMessage());
            result.put("responseTime", responseTime);
            logger.debug("HTTP test failed: {}", e.getMessage());
        }
        
        return result;
    }

    private Map<String, Object> testPing(String host, Integer port) {
        Map<String, Object> result = new HashMap<>();
        result.put("method", "PING");
        result.put("name", "Ping");
        result.put("description", "ICMP ping or TCP connection");
        
        long startTime = System.currentTimeMillis();
        try {
            // Create a temporary service object for ping test
            com.fsmonitor.app.entity.Service tempService = new com.fsmonitor.app.entity.Service();
            tempService.setHost(host);
            tempService.setPort(port != null ? port : 0);
            
            boolean success = pingChecker.check(tempService);
            long responseTime = System.currentTimeMillis() - startTime;
            
            result.put("success", success);
            result.put("message", success ? "Host is reachable" : "Host is not reachable");
            result.put("responseTime", responseTime);
            logger.debug("Ping test: {} - {}ms", success, responseTime);
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("message", "Ping failed: " + e.getMessage());
            result.put("responseTime", responseTime);
            logger.debug("Ping test failed: {}", e.getMessage());
        }
        
        return result;
    }

    private String determineRecommendedMethod(List<Map<String, Object>> methods) {
        // Priority: HTTP > TCP > PING
        for (Map<String, Object> method : methods) {
            if ("HTTP".equals(method.get("method")) && Boolean.TRUE.equals(method.get("success"))) {
                return "HTTP";
            }
        }
        
        for (Map<String, Object> method : methods) {
            if ("TCP".equals(method.get("method")) && Boolean.TRUE.equals(method.get("success"))) {
                return "TCP";
            }
        }
        
        for (Map<String, Object> method : methods) {
            if ("PING".equals(method.get("method")) && Boolean.TRUE.equals(method.get("success"))) {
                return "PING";
            }
        }
        
        return "TCP"; // Default fallback
    }
}
