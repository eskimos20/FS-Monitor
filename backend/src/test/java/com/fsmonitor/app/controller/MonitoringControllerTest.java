package com.fsmonitor.app.controller;

import com.fsmonitor.app.cache.IntegrationCacheService;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.entity.TimeInterval;
import com.fsmonitor.app.service.FileMonitoringService;
import com.fsmonitor.app.service.IntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoringControllerTest {

    private FileMonitoringService fileMonitoringService;
    private IntegrationService integrationService;
    private IntegrationCacheService cacheService;
    private MonitoringController controller;

    private Integration buildIntegration(long id) {
        Integration i = new Integration();
        i.setId(id);
        i.setName("int-" + id);
        i.setPath("/tmp");
        i.setMonitoringEnabled(true);
        i.setIsActive(true);
        i.setMonitorAllFiles(true);
        i.setCheckIntervalValue(5);
        i.setCheckIntervalUnit(TimeInterval.MINUTES);
        return i;
    }

    @BeforeEach
    void setUp() {
        fileMonitoringService = mock(FileMonitoringService.class);
        integrationService = mock(IntegrationService.class);
        cacheService = new IntegrationCacheService();
        controller = new MonitoringController(fileMonitoringService, integrationService, cacheService);
        // Scheduler pass grid anchored 45s ahead - do NOT use "next whole
        // minute": near a minute boundary that is <1s away and Duration
        // truncation yields 0, making the assertions timing-flaky.
        when(fileMonitoringService.getNextSchedulerRun())
                .thenReturn(LocalDateTime.now().plusSeconds(45));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> timersFor(Integration i) {
        when(integrationService.getAllIntegrations()).thenReturn(List.of(i));
        Map<String, Object> body = controller.getMonitoringStatus().getBody();
        assertNotNull(body);
        return (Map<String, Object>) ((Map<String, Object>) body.get("integrationTimers"))
                .get(String.valueOf(i.getId()));
    }

    @Test
    void timerReportsNextSchedulerPassWhenDueAndNoSchedule() {
        Integration i = buildIntegration(1L);
        cacheService.getCache(1L).setLastCheckedAt(
                LocalDateTime.now().minusMinutes(10)); // due long ago

        Map<String, Object> timer = timersFor(i);
        long seconds = (Long) timer.get("secondsUntilNextRun");
        assertTrue(seconds > 0 && seconds <= 65,
                "due + unscheduled => next pass (~60s), got " + seconds);
    }

    @Test
    void timerAlignsToPassAfterIntervalBoundary() {
        Integration i = buildIntegration(1L); // 5 min interval
        cacheService.getCache(1L).setLastCheckedAt(LocalDateTime.now().minusMinutes(2));

        Map<String, Object> timer = timersFor(i);
        long seconds = (Long) timer.get("secondsUntilNextRun");
        // due in ~3min -> first pass at-or-after due is 3-4 min out
        assertTrue(seconds >= 120 && seconds <= 300,
                "expected pass-aligned ~3-4min, got " + seconds);
    }

    @Test
    void dueExactlyOnPassBoundaryDoesNotAddExtraMinute() {
        // Regression: lastCheck and the pass grid share a second-anchor, so a
        // due time landing exactly on a pass must use it, not skip to +60s.
        LocalDateTime t0 = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        when(fileMonitoringService.getNextSchedulerRun()).thenReturn(t0.plusSeconds(60));
        Integration i = buildIntegration(1L); // 5 min interval
        // due = t0+60s+5min is NOT on the boundary; make due == t0+120s (2nd pass)
        cacheService.getCache(1L).setLastCheckedAt(t0.plusSeconds(120).minusMinutes(5));

        Map<String, Object> timer = timersFor(i);
        long seconds = (Long) timer.get("secondsUntilNextRun");
        long expected = Duration.between(LocalDateTime.now(), t0.plusSeconds(120)).getSeconds();
        assertTrue(seconds >= expected - 2 && seconds <= expected + 2,
                "due on a pass boundary => that pass, got " + seconds + " expected ~" + expected);
    }

    @Test
    void scheduleBlockedIntegrationStillCountsDown() {
        // Checks run regardless of schedule - the schedule only gates mail.
        // An overdue, schedule-blocked integration counts down to the next pass.
        String futureDay = LocalDate.now().plusDays(2)
                .getDayOfWeek().name().substring(0, 3); // e.g. "FRI"
        Integration i = buildIntegration(1L);
        i.setScheduleEnabled(true);
        i.setActiveDays(futureDay);
        i.setActiveStartHour(0);
        i.setActiveEndHour(24);
        cacheService.getCache(1L).setLastCheckedAt(
                LocalDateTime.now().minusMinutes(30)); // overdue

        Map<String, Object> timer = timersFor(i);
        long seconds = (Long) timer.get("secondsUntilNextRun");

        assertTrue(seconds > 0 && seconds <= 65,
                "checks run outside schedule too - next pass (~60s), got " + seconds);
        assertEquals(Boolean.TRUE, timer.get("outsideSchedule"),
                "flag stays so the UI can indicate alerts are paused");
    }

    @Test
    void neverCheckedIntegrationOutsideScheduleRunsOnNextPass() {
        String futureDay = LocalDate.now().plusDays(2)
                .getDayOfWeek().name().substring(0, 3);
        Integration i = buildIntegration(1L);
        i.setScheduleEnabled(true);
        i.setActiveDays(futureDay);
        i.setActiveStartHour(0);
        i.setActiveEndHour(24);
        // no lastCheckedAt - never ran

        Map<String, Object> timer = timersFor(i);
        long seconds = (Long) timer.get("secondsUntilNextRun");
        assertTrue(seconds > 0 && seconds <= 65,
                "never-checked integration is due immediately - next pass, got " + seconds);
        assertEquals(Boolean.TRUE, timer.get("outsideSchedule"));
    }

    @Test
    void scheduledIntegrationInsideWindowBehavesNormally() {
        String today = LocalDate.now().getDayOfWeek().name().substring(0, 3);
        Integration i = buildIntegration(1L);
        i.setScheduleEnabled(true);
        i.setActiveDays(today);
        i.setActiveStartHour(0);
        i.setActiveEndHour(24);
        cacheService.getCache(1L).setLastCheckedAt(LocalDateTime.now().minusMinutes(10));

        Map<String, Object> timer = timersFor(i);
        long seconds = (Long) timer.get("secondsUntilNextRun");
        assertTrue(seconds > 0 && seconds <= 65,
                "inside schedule + due => next pass, got " + seconds);
        assertEquals(Boolean.FALSE, timer.get("outsideSchedule"));
    }
}
