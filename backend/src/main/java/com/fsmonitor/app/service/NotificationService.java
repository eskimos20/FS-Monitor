package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.repository.IntegrationRepository;
import com.fsmonitor.app.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sends e-mail alerts when monitored entities go down/inactive and a
 * recovery e-mail when they come back.
 *
 * Flap protection: a notification is only sent after
 * {@code fsmonitor.alert.failure-threshold} consecutive failed checks, so a
 * single transient blip never pages anyone. Counters are in-memory - a
 * restart resets the count but not the persisted notificationSent flag.
 *
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
    private final int failureThreshold;

    private final Map<Long, AtomicInteger> serviceFailures = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> integrationFailures = new ConcurrentHashMap<>();

    public NotificationService(EmailService emailService,
                               IntegrationRepository integrationRepository,
                               ServiceRepository serviceRepository,
                               @Qualifier("mailTaskExecutor") Executor mailTaskExecutor,
                               MailConfigService mailConfigService,
                               @Value("${fsmonitor.alert.failure-threshold:3}") int failureThreshold) {
        this.emailService = emailService;
        this.integrationRepository = integrationRepository;
        this.serviceRepository = serviceRepository;
        this.mailTaskExecutor = mailTaskExecutor;
        this.mailConfigService = mailConfigService;
        this.failureThreshold = Math.max(1, failureThreshold);
    }

    @Transactional
    public boolean checkAndSendIntegrationNotification(Integration integration, LocalDateTime lastFileFound) {
        if (Boolean.TRUE.equals(integration.getNotificationSent())) {
            logger.debug("Integration {} notification already sent", integration.getName());
            return false;
        }

        int failures = integrationFailures
                .computeIfAbsent(integration.getId(), k -> new AtomicInteger())
                .incrementAndGet();
        if (failures < failureThreshold) {
            logger.info("Integration {} inactive (check {}/{} consecutive) - not alerting yet",
                    integration.getName(), failures, failureThreshold);
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

        int failures = serviceFailures
                .computeIfAbsent(service.getId(), k -> new AtomicInteger())
                .incrementAndGet();
        if (failures < failureThreshold) {
            logger.info("Service {} offline (check {}/{} consecutive) - not alerting yet",
                    service.getName(), failures, failureThreshold);
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

    /**
     * Reset the failure counter. When an alert was previously sent, also send
     * a recovery e-mail reporting how long the entity was down/inactive.
     */
    @Transactional
    public void clearIntegrationNotification(Integration integration) {
        integrationFailures.remove(integration.getId());
        if (Boolean.TRUE.equals(integration.getNotificationSent())) {
            sendRecoveryEmail(
                    "Integration " + integration.getName() + " is ACTIVE again",
                    integration.getName(),
                    "active",
                    integration.getNotificationSentAt());
            integration.setNotificationSent(false);
            integration.setNotificationSentAt(null);
            integrationRepository.save(integration);
            logger.info("Cleared integration {} notification - recovery e-mail sent", integration.getName());
        }
    }

    /**
     * Reset the failure counter. When an alert was previously sent, also send
     * a recovery e-mail reporting how long the service was offline.
     */
    @Transactional
    public void clearServiceNotification(com.fsmonitor.app.entity.Service service) {
        serviceFailures.remove(service.getId());
        if (Boolean.TRUE.equals(service.getNotificationSent())) {
            sendRecoveryEmail(
                    "Service " + service.getName() + " is back ONLINE",
                    service.getName(),
                    "online",
                    service.getNotificationSentAt());
            service.setNotificationSent(false);
            service.setNotificationSentAt(null);
            serviceRepository.save(service);
            logger.info("Cleared service {} notification - recovery e-mail sent", service.getName());
        }
    }

    /** Drop failure counters for entities that no longer exist. */
    public void pruneServiceFailures(Set<Long> activeIds) {
        serviceFailures.keySet().retainAll(activeIds);
    }

    public void pruneIntegrationFailures(Set<Long> activeIds) {
        integrationFailures.keySet().retainAll(activeIds);
    }

    private void sendRecoveryEmail(String subject, String entityName,
                                   String stateWord, LocalDateTime alertedAt) {
        String body = String.format(
            "%s has recovered and is now %s.\n\n" +
            "Alert was raised: %s\n" +
            "Duration: %s\n" +
            "Recovery detected: %s\n\n" +
            "FS-Monitor System",
            entityName,
            stateWord,
            alertedAt != null ? alertedAt.format(TIMESTAMP_FORMAT) : "unknown",
            formatDuration(alertedAt),
            LocalDateTime.now().format(TIMESTAMP_FORMAT)
        );
        sendNotificationEmail(subject, body, entityName);
    }

    /** Human-readable downtime, e.g. "14 minutes" or "2 hours 5 minutes". */
    private String formatDuration(LocalDateTime since) {
        if (since == null) {
            return "unknown";
        }
        Duration d = Duration.between(since, LocalDateTime.now());
        if (d.isNegative()) {
            return "unknown";
        }
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        if (days == 0 && hours == 0) {
            return minutes <= 1 ? "about a minute" : minutes + " minutes";
        }
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (days == 0 && minutes > 0) {
            sb.append(" ").append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        return sb.toString();
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
