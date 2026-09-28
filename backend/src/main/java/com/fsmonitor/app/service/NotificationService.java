package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.repository.IntegrationRepository;
import com.fsmonitor.app.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Sends e-mail alerts when monitored entities go down/inactive.
 * Notification state is persisted on the entities themselves so a
 * notification is not re-sent after an application restart.
 */
@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final int EMAIL_TIMEOUT_SECONDS = 15;
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EmailService emailService;
    private final IntegrationRepository integrationRepository;
    private final ServiceRepository serviceRepository;
    private final Executor mailTaskExecutor;
    private final MailConfigService mailConfigService;

    public NotificationService(EmailService emailService,
                               IntegrationRepository integrationRepository,
                               ServiceRepository serviceRepository,
                               @Qualifier("mailTaskExecutor") Executor mailTaskExecutor,
                               MailConfigService mailConfigService) {
        this.emailService = emailService;
        this.integrationRepository = integrationRepository;
        this.serviceRepository = serviceRepository;
        this.mailTaskExecutor = mailTaskExecutor;
        this.mailConfigService = mailConfigService;
    }

    @Transactional
    public boolean checkAndSendIntegrationNotification(Integration integration, LocalDateTime lastFileFound) {
        if (Boolean.TRUE.equals(integration.getNotificationSent())) {
            logger.debug("Integration {} notification already sent", integration.getName());
            return false;
        }

        String subject = "FS-Monitor Alert: Integration " + integration.getName() + " is INACTIVE";
        String body = String.format(
            "Integration %s has become inactive.\n\n" +
            "Last file found: %s\n" +
            "Time of detection: %s\n\n" +
            "Please check the integration and resolve any issues.\n\n" +
            "FS-Monitor System",
            integration.getName(),
            lastFileFound != null ? lastFileFound.format(TIMESTAMP_FORMAT) : "Never",
            LocalDateTime.now().format(TIMESTAMP_FORMAT)
        );

        if (!sendNotificationEmail(subject, body, integration.getName())) {
            return false;
        }

        integration.setNotificationSent(true);
        integration.setNotificationSentAt(LocalDateTime.now());
        integrationRepository.save(integration);
        logger.info("Integration {} inactive notification sent and saved to database", integration.getName());
        return true;
    }

    @Transactional
    public boolean checkAndSendServiceNotification(com.fsmonitor.app.entity.Service service) {
        if (Boolean.TRUE.equals(service.getNotificationSent())) {
            logger.debug("Service {} notification already sent", service.getName());
            return false;
        }

        String subject = "FS-Monitor Alert: Service " + service.getName() + " is OFFLINE";
        String body = String.format(
            "Service %s has gone offline.\n\n" +
            "Time of detection: %s\n\n" +
            "Please check the service and resolve any issues.\n\n" +
            "FS-Monitor System",
            service.getName(),
            LocalDateTime.now().format(TIMESTAMP_FORMAT)
        );

        if (!sendNotificationEmail(subject, body, service.getName())) {
            return false;
        }

        service.setNotificationSent(true);
        service.setNotificationSentAt(LocalDateTime.now());
        serviceRepository.save(service);
        logger.info("Service {} offline notification sent", service.getName());
        return true;
    }

    @Transactional
    public void clearIntegrationNotification(Integration integration) {
        if (Boolean.TRUE.equals(integration.getNotificationSent())) {
            integration.setNotificationSent(false);
            integration.setNotificationSentAt(null);
            integrationRepository.save(integration);
            logger.debug("Cleared integration {} notification status in database", integration.getName());
        }
    }

    @Transactional
    public void clearServiceNotification(com.fsmonitor.app.entity.Service service) {
        if (Boolean.TRUE.equals(service.getNotificationSent())) {
            service.setNotificationSent(false);
            service.setNotificationSentAt(null);
            serviceRepository.save(service);
            logger.debug("Cleared service {} notification status in database", service.getName());
        }
    }

    private boolean sendNotificationEmail(String subject, String body, String entityName) {
        try {
            // Fail fast when no mail configuration exists
            mailConfigService.getCurrentMailConfig()
                .orElseThrow(() -> new IllegalStateException("Mail configuration not found"));

            boolean sent = CompletableFuture
                    .supplyAsync(() -> emailService.sendEmail(subject, body), mailTaskExecutor)
                    .get(EMAIL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!sent) {
                logger.warn("Failed to send notification for {}", entityName);
            }
            return sent;
        } catch (Exception e) {
            logger.warn("Failed to send notification for {}: {}", entityName, e.getMessage());
            return false;
        }
    }
}
