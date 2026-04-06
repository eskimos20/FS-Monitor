package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.NotificationLog;
import com.fsmonitor.app.entity.NotificationType;
import com.fsmonitor.app.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private MailConfigService mailConfigService;

    @Transactional
    public boolean checkAndSendIntegrationNotification(Long integrationId, String integrationName, LocalDateTime lastFileFound) {
        // Check if notification already sent
        if (notificationLogRepository.findByTypeAndEntityId(NotificationType.INTEGRATION_INACTIVE, integrationId).isPresent()) {
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
            logger.warn("Could not send email notification for integration {}: {}", integrationName, e.getMessage());
        }
        
        if (emailSent) {
            // Log the notification
            NotificationLog log = new NotificationLog();
            log.setType(NotificationType.INTEGRATION_INACTIVE);
            log.setEntityId(integrationId);
            log.setEntityName(integrationName);
            notificationLogRepository.save(log);
            
            logger.info("Integration {} inactive notification sent", integrationName);
            return true;
        } else {
            logger.warn("Failed to send integration {} notification (no mail config)", integrationName);
            // Don't fail the entire transaction if email fails
            return false;
        }
    }

    @Transactional
    public boolean checkAndSendServiceNotification(Long serviceId, String serviceName) {
        // Check if notification already sent
        if (notificationLogRepository.findByTypeAndEntityId(NotificationType.SERVICE_OFFLINE, serviceId).isPresent()) {
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
            // Log the notification
            NotificationLog log = new NotificationLog();
            log.setType(NotificationType.SERVICE_OFFLINE);
            log.setEntityId(serviceId);
            log.setEntityName(serviceName);
            notificationLogRepository.save(log);
            
            logger.info("Service {} offline notification sent", serviceName);
            return true;
        } else {
            logger.error("Failed to send service {} notification", serviceName);
            return false;
        }
    }

    @Transactional
    public void clearIntegrationNotification(Long integrationId) {
        notificationLogRepository.deleteByTypeAndEntityId(NotificationType.INTEGRATION_INACTIVE, integrationId);
        logger.debug("Cleared integration {} notification log", integrationId);
    }

    @Transactional
    public void clearServiceNotification(Long serviceId) {
        notificationLogRepository.deleteByTypeAndEntityId(NotificationType.SERVICE_OFFLINE, serviceId);
        logger.debug("Cleared service {} notification log", serviceId);
    }
}
