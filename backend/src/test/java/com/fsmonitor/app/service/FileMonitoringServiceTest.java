package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.IntegrationCacheService;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.entity.TimeInterval;
import com.fsmonitor.app.repository.IntegrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileMonitoringServiceTest {

    private IntegrationRepository integrationRepository;
    private NotificationService notificationService;
    private IntegrationCacheService cacheService;
    private FileMonitoringService service;

    @TempDir
    Path tempDir;

    private Integration buildIntegration() {
        Integration i = new Integration();
        i.setId(1L);
        i.setName("test-int");
        i.setPath(tempDir.toString());
        i.setMonitoringEnabled(true);
        i.setIsActive(true);
        i.setMonitorAllFiles(true);
        i.setCheckIntervalValue(5);
        i.setCheckIntervalUnit(TimeInterval.MINUTES);
        i.setThresholdValue(15);
        i.setThresholdUnit(TimeInterval.MINUTES);
        i.setScheduleEnabled(false);
        return i;
    }

    @BeforeEach
    void setUp() {
        integrationRepository = mock(IntegrationRepository.class);
        notificationService = mock(NotificationService.class);
        cacheService = new IntegrationCacheService();
        service = new FileMonitoringService(integrationRepository, notificationService, cacheService);
    }

    @Test
    void scanFindsLatestFileAndPopulatesCache() throws IOException {
        Files.writeString(tempDir.resolve("a.log"), "x");
        Integration i = buildIntegration();
        when(integrationRepository.findActiveMonitoringIntegrations()).thenReturn(List.of(i));
        when(integrationRepository.findAll()).thenReturn(List.of(i));

        // lastCheckedAt must be null for the first check to run
        service.monitorIntegrations();

        var cache = cacheService.getCache(1L);
        assertNotNull(cache.getLastCheckedAt(), "lastCheckedAt should be set after check");
        assertNotNull(cache.getLastFileFound(), "lastFileFound should be set when files exist");
        assertTrue(cache.getLastFileName().endsWith("a.log"));
        // and the same values are persisted so they survive a restart
        verify(integrationRepository).updateLastFileFound(
                eq(1L), any(), argThat(n -> n.endsWith("a.log")));
    }

    @Test
    void secondSchedulerPassDoesNotRecheckWithinInterval() throws IOException {
        Files.writeString(tempDir.resolve("a.log"), "x");
        Integration i = buildIntegration();
        when(integrationRepository.findActiveMonitoringIntegrations()).thenReturn(List.of(i));
        when(integrationRepository.findAll()).thenReturn(List.of(i));

        service.monitorIntegrations();
        service.monitorIntegrations();

        // updateLastCheck is called once per actual check - only once if pacing works
        verify(integrationRepository, times(1)).updateLastCheck(anyLong(), any());
    }

    @Test
    void startupSeedsLastFileFromDatabase() {
        // Simulate a restart: DB still holds the file found in the previous run,
        // the in-memory cache starts empty and must be seeded from the entity.
        Integration i = buildIntegration();
        java.time.LocalDateTime found = java.time.LocalDateTime.now().minusMinutes(3);
        i.setLastFileFound(found);
        i.setLastFileName("/data/report.pdf");
        when(integrationRepository.findAll()).thenReturn(List.of(i));

        service.initializeIntegrationMonitoring();

        var cache = cacheService.getCache(1L);
        assertEquals(found, cache.getLastFileFound(),
                "lastFileFound must be restored from DB on startup");
        assertEquals("/data/report.pdf", cache.getLastFileName());
        assertNotNull(cache.getLastCheckedAt(),
                "lastCheckedAt is seeded to startup - intervals count from boot");
    }
}
