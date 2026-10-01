package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.entity.MailConfig;
import com.fsmonitor.app.repository.IntegrationRepository;
import com.fsmonitor.app.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private EmailService emailService;
    private IntegrationRepository integrationRepository;
    private ServiceRepository serviceRepository;
    private MailConfigService mailConfigService;
    private NotificationService notificationService;

    private com.fsmonitor.app.entity.Service serviceWithId(long id) {
        com.fsmonitor.app.entity.Service s = new com.fsmonitor.app.entity.Service();
        s.setId(id);
        s.setName("svc-" + id);
        s.setIsActive(true);
        return s;
    }

    private Integration integrationWithId(long id) {
        Integration i = new Integration();
        i.setId(id);
        i.setName("int-" + id);
        return i;
    }

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        integrationRepository = mock(IntegrationRepository.class);
        serviceRepository = mock(ServiceRepository.class);
        mailConfigService = mock(MailConfigService.class);

        MailConfig mailConfig = new MailConfig();
        mailConfig.setHost("smtp.test");
        mailConfig.setPort(25);
        mailConfig.setFromEmail("from@test");
        mailConfig.setToEmail("to@test");
        when(mailConfigService.getCurrentMailConfig()).thenReturn(Optional.of(mailConfig));
        when(emailService.sendEmail(anyString(), anyString())).thenReturn(true);

        Executor direct = Runnable::run;
        notificationService = new NotificationService(
                emailService, integrationRepository, serviceRepository, direct, mailConfigService, 3);
    }

    @Test
    void doesNotAlertBeforeThreshold() {
        com.fsmonitor.app.entity.Service s = serviceWithId(1);

        assertFalse(notificationService.checkAndSendServiceNotification(s));
        assertFalse(notificationService.checkAndSendServiceNotification(s));

        verify(emailService, never()).sendEmail(anyString(), anyString());
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void alertsOnThirdConsecutiveFailure() {
        com.fsmonitor.app.entity.Service s = serviceWithId(1);

        notificationService.checkAndSendServiceNotification(s);
        notificationService.checkAndSendServiceNotification(s);
        assertTrue(notificationService.checkAndSendServiceNotification(s));

        verify(emailService, times(1)).sendEmail(
                contains("OFFLINE"), contains("svc-1"));
        assertTrue(s.getNotificationSent());
        assertNotNull(s.getNotificationSentAt());
    }

    @Test
    void doesNotRealertWhileNotificationSent() {
        com.fsmonitor.app.entity.Service s = serviceWithId(1);
        s.setNotificationSent(true);
        s.setNotificationSentAt(LocalDateTime.now().minusMinutes(5));

        assertFalse(notificationService.checkAndSendServiceNotification(s));
        verify(emailService, never()).sendEmail(anyString(), anyString());
    }

    @Test
    void recoveryResetsCounterAndSendsRecoveryEmail() {
        com.fsmonitor.app.entity.Service s = serviceWithId(1);

        // Three failures -> alert
        for (int i = 0; i < 3; i++) {
            notificationService.checkAndSendServiceNotification(s);
        }
        s.setNotificationSentAt(LocalDateTime.now().minusMinutes(14));

        // Recovery -> clears flag + sends recovery email
        notificationService.clearServiceNotification(s);

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(2)).sendEmail(subject.capture(), body.capture());

        assertTrue(subject.getValue().contains("back ONLINE"));
        assertTrue(body.getValue().contains("14 minutes"));
        assertFalse(s.getNotificationSent());
        assertNull(s.getNotificationSentAt());
        verify(serviceRepository, times(2)).save(s);
    }

    @Test
    void flapBetweenChecksResetsStreak() {
        com.fsmonitor.app.entity.Service s = serviceWithId(1);

        notificationService.checkAndSendServiceNotification(s); // fail 1
        notificationService.checkAndSendServiceNotification(s); // fail 2
        notificationService.clearServiceNotification(s);        // recovers -> counter reset
        notificationService.checkAndSendServiceNotification(s); // fail 1 again
        notificationService.checkAndSendServiceNotification(s); // fail 2 again

        verify(emailService, never()).sendEmail(anyString(), anyString());
    }

    @Test
    void recoveryWithoutAlertSendsNoEmail() {
        com.fsmonitor.app.entity.Service s = serviceWithId(1);

        notificationService.checkAndSendServiceNotification(s); // fail 1 (below threshold)
        notificationService.clearServiceNotification(s);        // recovers before alerting

        verify(emailService, never()).sendEmail(anyString(), anyString());
    }

    @Test
    void integrationAlertAlsoHonoursThreshold() {
        Integration i = integrationWithId(7);

        notificationService.checkAndSendIntegrationNotification(i, null);
        notificationService.checkAndSendIntegrationNotification(i, null);
        verify(emailService, never()).sendEmail(anyString(), anyString());

        assertTrue(notificationService.checkAndSendIntegrationNotification(i, null));
        verify(emailService, times(1)).sendEmail(contains("INACTIVE"), anyString());
    }

    @Test
    void integrationRecoverySendsEmail() {
        Integration i = integrationWithId(7);
        i.setNotificationSent(true);
        i.setNotificationSentAt(LocalDateTime.now().minusHours(2).minusMinutes(5));

        notificationService.clearIntegrationNotification(i);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmail(contains("ACTIVE again"), body.capture());
        assertTrue(body.getValue().contains("2 hours 5 minutes"));
        verify(integrationRepository).save(i);
    }

    @Test
    void alertSuppressedOutsideScheduleButFiresInsideWindow() {
        // Schedule only allows mail on the day after tomorrow -> all suppressed
        String futureDay = java.time.LocalDate.now().plusDays(2)
                .getDayOfWeek().name().substring(0, 3);
        com.fsmonitor.app.entity.Service s = serviceWithId(1);
        s.setScheduleEnabled(true);
        s.setActiveDays(futureDay);
        s.setActiveStartHour(0);
        s.setActiveEndHour(24);

        for (int i = 0; i < 5; i++) {
            assertFalse(notificationService.checkAndSendServiceNotification(s));
        }
        verify(emailService, never()).sendEmail(anyString(), anyString());
        assertNotEquals(Boolean.TRUE, s.getNotificationSent());

        // Same service inside an all-day today window -> fires on first check
        // (failure streak already exceeds the threshold)
        s.setActiveDays(java.time.LocalDate.now()
                .getDayOfWeek().name().substring(0, 3));
        assertTrue(notificationService.checkAndSendServiceNotification(s));
        verify(emailService).sendEmail(contains("OFFLINE"), anyString());
    }

    @Test
    void recoveryOutsideScheduleClearsFlagWithoutEmail() {
        String futureDay = java.time.LocalDate.now().plusDays(2)
                .getDayOfWeek().name().substring(0, 3);
        com.fsmonitor.app.entity.Service s = serviceWithId(1);
        s.setScheduleEnabled(true);
        s.setActiveDays(futureDay);
        s.setActiveStartHour(0);
        s.setActiveEndHour(24);
        s.setNotificationSent(true);
        s.setNotificationSentAt(LocalDateTime.now().minusHours(1));

        notificationService.clearServiceNotification(s);

        verify(emailService, never()).sendEmail(anyString(), anyString());
        assertFalse(s.getNotificationSent());
        assertNull(s.getNotificationSentAt());
        verify(serviceRepository).save(s);
    }

    @Test
    void pruneRemovesCountersForDeletedEntities() {
        com.fsmonitor.app.entity.Service s = serviceWithId(1);
        notificationService.checkAndSendServiceNotification(s);
        notificationService.checkAndSendServiceNotification(s);

        notificationService.pruneServiceFailures(java.util.Set.of());

        // Counter was dropped - needs 3 fresh failures to alert
        notificationService.checkAndSendServiceNotification(s);
        notificationService.checkAndSendServiceNotification(s);
        verify(emailService, never()).sendEmail(anyString(), anyString());
        assertTrue(notificationService.checkAndSendServiceNotification(s));
    }
}
