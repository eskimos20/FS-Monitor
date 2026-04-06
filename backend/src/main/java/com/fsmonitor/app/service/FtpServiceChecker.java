package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.Socket;

@Component
public class FtpServiceChecker implements ServiceChecker {
    private static final Logger logger = LoggerFactory.getLogger(FtpServiceChecker.class);
    private static final int TIMEOUT_MS = 5000;

    @Override
    public boolean check(Service service) {
        String host = service.getHost();
        int port = service.getPort() != null ? service.getPort() : 21;
        
        // If credentials are provided, test actual FTP connection
        if (Boolean.TRUE.equals(service.getUseCredentials()) && service.getUsername() != null) {
            return checkFtpConnection(service, host, port);
        }
        
        // Otherwise, just check if FTP port is open
        return checkTcpPort(host, port, service.getName());
    }

    private boolean checkFtpConnection(Service service, String host, int port) {
        FTPClient ftpClient = new FTPClient();
        
        try {
            logger.debug("Testing FTP connection to {}:{} with credentials", host, port);
            
            ftpClient.setConnectTimeout(TIMEOUT_MS);
            ftpClient.setDefaultTimeout(TIMEOUT_MS);
            ftpClient.connect(host, port);
            
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                logger.debug("FTP server refused connection");
                return false;
            }
            
            // Login
            String username = service.getUsername();
            String password = service.getPassword() != null ? service.getPassword() : "";
            boolean loginSuccess = ftpClient.login(username, password);
            
            if (!loginSuccess) {
                logger.debug("FTP login failed for user: {}", username);
                return false;
            }
            
            // Test path access if provided
            if (service.getSharePath() != null && !service.getSharePath().trim().isEmpty()) {
                logger.debug("Testing access to path: {}", service.getSharePath());
                boolean pathExists = ftpClient.changeWorkingDirectory(service.getSharePath());
                if (!pathExists) {
                    logger.debug("FTP path does not exist or is not accessible: {}", service.getSharePath());
                    return false;
                }
            }
            
            logger.debug("FTP connection successful");
            return true;
            
        } catch (Exception e) {
            logger.debug("FTP connection failed for {}: {}", service.getName(), e.getMessage());
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (Exception e) {
                logger.debug("Error closing FTP connection: {}", e.getMessage());
            }
        }
    }

    private boolean checkTcpPort(String host, int port, String serviceName) {
        try {
            logger.debug("Checking FTP port: {}:{}", host, port);
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), TIMEOUT_MS);
                return socket.isConnected();
            }
        } catch (Exception e) {
            logger.debug("FTP port check failed for {}: {}", serviceName, e.getMessage());
            return false;
        }
    }
}
