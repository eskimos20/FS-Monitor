package com.fsmonitor.app.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeleteMonitoringServiceTest {

    @Test
    void protectedRootsAreBlocked() {
        for (String p : new String[]{"/", "/etc", "/usr", "/var/lib", "/var/log", "/bin", "/sbin", "/boot", "/proc", "/sys", "/dev", "/root"}) {
            assertTrue(DeleteMonitoringService.isProtectedPath(Path.of(p)), "expected " + p + " to be protected");
        }
    }

    @Test
    void parentsOfProtectedPathsAreBlocked() {
        // "/var" is a parent of /var/lib and /var/log - cleaning it would wipe system dirs
        assertTrue(DeleteMonitoringService.isProtectedPath(Path.of("/var")));
    }

    @Test
    void cleaningInsideProtectedTreesIsAllowed() {
        assertFalse(DeleteMonitoringService.isProtectedPath(Path.of("/var/log/myapp")));
        assertFalse(DeleteMonitoringService.isProtectedPath(Path.of("/var/lib/myapp/data")));
        assertFalse(DeleteMonitoringService.isProtectedPath(Path.of("/home/user/tmp")));
        assertFalse(DeleteMonitoringService.isProtectedPath(Path.of("/opt/data/incoming")));
    }

    @Test
    void fileTypeParsing() {
        assertEquals(Set.of(), DeleteMonitoringService.parseFileTypes(null));
        assertEquals(Set.of(), DeleteMonitoringService.parseFileTypes("  "));
        assertEquals(Set.of(".log"), DeleteMonitoringService.parseFileTypes("log"));
        assertEquals(Set.of(".log", ".txt"), DeleteMonitoringService.parseFileTypes(".log, TXT"));
        assertEquals(Set.of(".log", ".txt"), DeleteMonitoringService.parseFileTypes(" .log , txt ,,"));
    }
}
