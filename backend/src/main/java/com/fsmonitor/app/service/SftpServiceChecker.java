package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelSftp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.Socket;
import java.util.Properties;

@Component
public class SftpServiceChecker implements ServiceChecker {
    private static final Logger logger = LoggerFactory.getLogger(SftpServiceChecker.class);
    private static final int TIMEOUT_MS = 5000;

    @Override
    public boolean check(Service service) {
        String host = service.getHost();
        int port = service.getPort() != null ? service.getPort() : 22;
        
        // If credentials are provided, test actual SFTP connection
        if (Boolean.TRUE.equals(service.getUseCredentials()) && service.getUsername() != null) {
            return checkSftpConnection(service, host, port);
        }
        
        // Otherwise, just check if SSH port is open
        return checkTcpPort(host, port, service.getName());
    }

    private boolean checkSftpConnection(Service service, String host, int port) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channel = null;
        
        try {
            logger.debug("Testing SFTP connection to {}:{} with credentials", host, port);
            
            session = jsch.getSession(service.getUsername(), host, port);
            
            // Configure authentication
            if (service.getPrivateKey() != null && !service.getPrivateKey().trim().isEmpty()) {
                // Use SSH private key
                logger.debug("Using SSH private key for authentication");
                byte[] privateKeyBytes = service.getPrivateKey().getBytes();
                jsch.addIdentity("key", privateKeyBytes, null, null);
            } else if (service.getPassword() != null) {
                // Use password
                logger.debug("Using password for authentication");
                session.setPassword(service.getPassword());
            }
            
            // Disable strict host key checking
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(TIMEOUT_MS);
            
            session.connect();
            
            // Open SFTP channel
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(TIMEOUT_MS);
            
            // Test path access if provided
            if (service.getSharePath() != null && !service.getSharePath().trim().isEmpty()) {
                logger.debug("Testing access to path: {}", service.getSharePath());
                channel.ls(service.getSharePath());
            }
            
            logger.debug("SFTP connection successful");
            return true;
            
        } catch (Exception e) {
            logger.debug("SFTP connection failed for {}: {}", service.getName(), e.getMessage());
            return false;
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private boolean checkTcpPort(String host, int port, String serviceName) {
        try {
            logger.debug("Checking SSH/SFTP port: {}:{}", host, port);
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), TIMEOUT_MS);
                return socket.isConnected();
            }
        } catch (Exception e) {
            logger.debug("SSH/SFTP port check failed for {}: {}", serviceName, e.getMessage());
            return false;
        }
    }
}
