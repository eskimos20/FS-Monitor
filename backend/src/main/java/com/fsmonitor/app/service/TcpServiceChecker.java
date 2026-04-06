package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@org.springframework.stereotype.Service
public class TcpServiceChecker implements ServiceChecker {
    private static final Logger logger = LoggerFactory.getLogger(TcpServiceChecker.class);
    private static final int TIMEOUT_MS = 5000;

    @Override
    public boolean check(Service service) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(service.getHost(), service.getPort()), TIMEOUT_MS);
            logger.debug("TCP connection successful to {}:{}", service.getHost(), service.getPort());
            return true;
        } catch (IOException e) {
            logger.debug("TCP connection failed to {}:{} - {}", service.getHost(), service.getPort(), e.getMessage());
            return false;
        }
    }
}
