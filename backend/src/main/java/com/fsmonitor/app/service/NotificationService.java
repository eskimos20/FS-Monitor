package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.MonitoringCacheManager;
import com.fsmonitor.app.entity.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private MonitoringCacheManager cacheManager;

    @Autowired
    private MailConfigService mailConfigService;

    public boolean checkAndSendIntegrationNotification(Long integrationId, String integrationName, LocalDateTime lastFileFound) {
        // Check if notification already sent
        if (cacheManager.getNotification(NotificationType.INTEGRATION_INACTIVE, integrationId).isPresent()) {
            logger.debug("Integration {} notification already sent", integrationName);
            return false;
        }

        // Send notification
        String subject = "FS-Monitor Alert: Integration " + integrationName + " is INACTIVE";
        String body = String.format(
            "Integration %s has become inactive.\n\n" +
            "Last file found: %s\n" +
            "Time of detection: %s\n\n" +
            "Please check the integration and resolve any issues.\n\n" +
            "FS-Monitor System",
            integrationName,
            lastFileFound != null ? lastFileFound.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "Never",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        boolean emailSent = false;
        try {
            mailConfigService.getCurrentMailConfig()
                .orElseThrow(() -> new RuntimeException("Mail configuration not found"));

            emailSent = emailService.sendEmail(subject, body);
        } catch (Exception e) {
            logger.warn("Failed to send integration {} notification: {}", integrationName, e.getMessage());
            return false;
        }
        
        if (emailSent) {
            // Cache the notification
            cacheManager.saveNotification(NotificationType.INTEGRATION_INACTIVE, integrationId, integrationName);
            
            logger.info("Integration {} inactive notification sent", integrationName);
            return true;
        } else {
            logger.warn("Failed to send integration {} notification: Email send failed", integrationName);
            return false;
        }
    }

    public boolean checkAndSendServiceNotification(Long serviceId, String serviceName) {
        // Check if notification already sent
        if (cacheManager.getNotification(NotificationType.SERVICE_OFFLINE, serviceId).isPresent()) {
            logger.debug("Service {} notification already sent", serviceName);
            return false;
        }

        // Send notification
        String subject = "FS-Monitor Alert: Service " + serviceName + " is OFFLINE";
        String body = String.format(
            "Service %s has gone offline.\n\n" +
            "Time of detection: %s\n\n" +
            "Please check the service and resolve any issues.\n\n" +
            "FS-Monitor System",
            serviceName,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        mailConfigService.getCurrentMailConfig()
            .orElseThrow(() -> new RuntimeException("Mail configuration not found"));

        boolean emailSent = emailService.sendEmail(subject, body);
        
        if (emailSent) {
            // Cache the notification
            cacheManager.saveNotification(NotificationType.SERVICE_OFFLINE, serviceId, serviceName);
            
            logger.info("Service {} offline notification sent", serviceName);
            return true;
        } else {
            logger.error("Failed to send service {} notification", serviceName);
            return false;
        }
    }

    public void clearIntegrationNotification(Long integrationId) {
        cacheManager.clearNotification(NotificationType.INTEGRATION_INACTIVE, integrationId);
        logger.debug("Cleared integration {} notification cache", integrationId);
    }

    public void clearServiceNotification(Long serviceId) {
        cacheManager.clearNotification(NotificationType.SERVICE_OFFLINE, serviceId);
        logger.debug("Cleared service {} notification cache", serviceId);
    }
}
