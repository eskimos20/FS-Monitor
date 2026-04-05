package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
public class PingServiceChecker implements ServiceChecker {
    private static final Logger logger = LoggerFactory.getLogger(PingServiceChecker.class);

    @Override
    public boolean check(Service service) {
        try {
            logger.debug("Pinging host: {}", service.getHost());
            
            InetAddress address = InetAddress.getByName(service.getHost());
            boolean reachable = address.isReachable(5000); // 5 second timeout
            
            if (reachable) {
                logger.debug("Host {} is reachable", service.getHost());
            } else {
                logger.debug("Host {} is not reachable", service.getHost());
            }
            
            return reachable;
        } catch (Exception e) {
            logger.debug("Ping check failed for {}: {}", service.getName(), e.getMessage());
            return false;
        }
    }
}
