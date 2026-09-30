package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.IntegrationCacheService;
import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.entity.TimeInterval;
import com.fsmonitor.app.repository.IntegrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FileMonitoringIntegrationTest {

    @Autowired
    FileMonitoringService fileMonitoringService;

    @Autowired
    IntegrationRepository integrationRepository;

    @Autowired
    IntegrationCacheService integrationCacheService;

    @TempDir
    static Path tempDir;

    @Test
    void realJpaPathPopulatesCacheAndRespectsInterval() throws IOException {
        Files.writeString(tempDir.resolve("found.log"), "data");

        Integration i = new Integration();
        i.setName("repro-int");
        i.setPath(tempDir.toString());
        i.setMonitoringEnabled(true);
        i.setIsActive(true);
        i.setMonitorAllFiles(true);
        i.setCheckIntervalValue(5);
        i.setCheckIntervalUnit(TimeInterval.MINUTES);
        i.setThresholdValue(15);
        i.setThresholdUnit(TimeInterval.MINUTES);
        i.setScheduleEnabled(false);
        // Mimic the frontend payload: an empty scheduleDays set is submitted
        i.setScheduleDays(new HashSet<>());
        i.setMonitoredFileTypes(new HashSet<>());
        Integration saved = integrationRepository.save(i);

        // First pass - integration has no lastCheckedAt in cache yet?
        // (PostConstruct seeding happened before this entity existed)
        List<Integration> active = integrationRepository.findActiveMonitoringIntegrations();
        assertTrue(active.stream().anyMatch(x -> x.getId().equals(saved.getId())),
                "new integration must appear in active list");

        fileMonitoringService.monitorIntegrations();

        var cache = integrationCacheService.getCache(saved.getId());
        assertNotNull(cache.getLastCheckedAt(), "cache.lastCheckedAt after first check");
        assertNotNull(cache.getLastFileFound(),
                "cache.lastFileFound should be set - this is what the UI displays");

        // The found file must be persisted so a restart can restore it
        Integration reloaded = integrationRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(reloaded.getLastFileFound(), "lastFileFound must be persisted to DB");
        assertTrue(reloaded.getLastFileName().endsWith("found.log"));
    }
}
