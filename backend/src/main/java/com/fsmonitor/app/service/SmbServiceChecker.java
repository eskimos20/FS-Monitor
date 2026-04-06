package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.Socket;

@Component
public class SmbServiceChecker implements ServiceChecker {
    private static final Logger logger = LoggerFactory.getLogger(SmbServiceChecker.class);
    private static final int TIMEOUT_MS = 5000;

    @Override
    public boolean check(Service service) {
        String host = service.getHost();
        int port = service.getPort() != null ? service.getPort() : 445;
        
        // If credentials are provided, test actual SMB share access
        if (Boolean.TRUE.equals(service.getUseCredentials()) && service.getUsername() != null) {
            return checkSmbShare(service, host);
        }
        
        // Otherwise, just check if SMB port is open
        return checkTcpPort(host, port, service.getName());
    }

    private boolean checkSmbShare(Service service, String host) {
        try {
            logger.debug("Testing SMB share access to {} with credentials", host);
            
            // Parse domain and username (format: DOMAIN\\username or just username)
            String domain = "";
            String username = service.getUsername();
            if (username.contains("\\")) {
                String[] parts = username.split("\\\\");
                domain = parts[0];
                username = parts[1];
            }
            
            String password = service.getPassword() != null ? service.getPassword() : "";
            
            // Create authentication with jcifs-ng
            CIFSContext context = SingletonContext.getInstance();
            NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator(domain, username, password);
            CIFSContext authContext = context.withCredentials(auth);
            
            // Build SMB URL
            String sharePath = service.getSharePath() != null ? service.getSharePath() : "";
            if (!sharePath.startsWith("/")) {
                sharePath = "/" + sharePath;
            }
            String smbUrl = "smb://" + host + sharePath;
            
            logger.debug("Testing SMB URL: {}", smbUrl);
            
            // Test access
            try (SmbFile smbFile = new SmbFile(smbUrl, authContext)) {
                // Try to check if file/share exists
                if (smbFile.exists()) {
                    logger.debug("SMB share access successful");
                    return true;
                } else {
                    logger.debug("SMB share does not exist: {}", smbUrl);
                    return false;
                }
            }
            
        } catch (Exception e) {
            logger.debug("SMB share access failed for {}: {}", service.getName(), e.getMessage());
            return false;
        }
    }

    private boolean checkTcpPort(String host, int port, String serviceName) {
        try {
            logger.debug("Checking SMB port: {}:{}", host, port);
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), TIMEOUT_MS);
                return socket.isConnected();
            }
        } catch (Exception e) {
            logger.debug("SMB port check failed for {}: {}", serviceName, e.getMessage());
            return false;
        }
    }
}
