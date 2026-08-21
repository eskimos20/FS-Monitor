package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.MonitoringCacheManager;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.entity.NotificationType;
import com.fsmonitor.app.repository.IntegrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private MonitoringCacheManager cacheManager;

    @Autowired
    @Qualifier("mailTaskExecutor")
    private Executor mailTaskExecutor;

    @Autowired
    private MailConfigService mailConfigService;

    private static final int EMAIL_TIMEOUT_SECONDS = 15;

    @Transactional
    public boolean checkAndSendIntegrationNotification(Integration integration, LocalDateTime lastFileFound) {
        // Check if notification already sent from database
        if (Boolean.TRUE.equals(integration.getNotificationSent())) {
            logger.debug("Integration {} notification already sent", integration.getName());
            return false;
        }

        // Send notification
        String subject = "FS-Monitor Alert: Integration " + integration.getName() + " is INACTIVE";
        String body = String.format(
            "Integration %s has become inactive.\n\n" +
            "Last file found: %s\n" +
            "Time of detection: %s\n\n" +
            "Please check the integration and resolve any issues.\n\n" +
            "FS-Monitor System",
            integration.getName(),
            lastFileFound != null ? lastFileFound.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "Never",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        boolean emailSent = false;
        try {
            mailConfigService.getCurrentMailConfig()
                .orElseThrow(() -> new RuntimeException("Mail configuration not found"));

            emailSent = sendEmailWithTimeout(subject, body);
        } catch (Exception e) {
            logger.warn("Failed to send integration {} notification: {}", integration.getName(), e.getMessage());
            return false;
        }
        
        if (emailSent) {
            // Save notification status to database
            integration.setNotificationSent(true);
            integration.setNotificationSentAt(LocalDateTime.now());
            integrationRepository.save(integration);
            
            logger.info("Integration {} inactive notification sent and saved to database", integration.getName());
            return true;
        } else {
            logger.warn("Failed to send integration {} notification: Email send failed", integration.getName());
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

        boolean emailSent = false;
        try {
            mailConfigService.getCurrentMailConfig()
                .orElseThrow(() -> new RuntimeException("Mail configuration not found"));

            emailSent = sendEmailWithTimeout(subject, body);
        } catch (Exception e) {
            logger.warn("Failed to send service {} notification: {}", serviceName, e.getMessage());
            return false;
        }
        
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

    @Transactional
    public void clearIntegrationNotification(Integration integration) {
        integration.setNotificationSent(false);
        integration.setNotificationSentAt(null);
        integrationRepository.save(integration);
        logger.debug("Cleared integration {} notification status in database", integration.getName());
    }

    public void clearServiceNotification(Long serviceId) {
        cacheManager.clearNotification(NotificationType.SERVICE_OFFLINE, serviceId);
        logger.debug("Cleared service {} notification cache", serviceId);
    }

    private boolean sendEmailWithTimeout(String subject, String body) throws Exception {
        return CompletableFuture.supplyAsync(() -> emailService.sendEmail(subject, body), mailTaskExecutor)
                .get(EMAIL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
