package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PingServiceCheckerTest {

    private final PingServiceChecker checker = new PingServiceChecker();

    static boolean pingBinaryAvailable() {
        try {
            Process p = new ProcessBuilder("ping", "-c", "1", "-w", "2", "127.0.0.1").start();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Test
    @EnabledIf("pingBinaryAvailable")
    void loopbackIsReachable() {
        Service service = new Service();
        service.setName("loopback");
        service.setHost("127.0.0.1");
        assertTrue(checker.check(service));
    }

    @Test
    @EnabledIf("pingBinaryAvailable")
    void unresolvableHostIsOffline() {
        Service service = new Service();
        service.setName("bogus");
        service.setHost("nonexistent.invalid.example");
        assertFalse(checker.check(service));
    }
}
