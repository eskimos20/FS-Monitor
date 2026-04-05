package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.Socket;

@Component
public class SmbServiceChecker implements ServiceChecker {
    private static final Logger logger = LoggerFactory.getLogger(SmbServiceChecker.class);

    @Override
    public boolean check(Service service) {
        try {
            String host = service.getHost();
            int port = service.getPort() != null ? service.getPort() : 445;
            
            logger.debug("Checking SMB service: {}:{}", host, port);
            
            // Basic TCP connection test to SMB port
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 5000);
                return socket.isConnected();
            }
        } catch (Exception e) {
            logger.debug("SMB check failed for {}: {}", service.getName(), e.getMessage());
            return false;
        }
    }
}
