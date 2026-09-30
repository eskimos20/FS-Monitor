package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.util.ShellCommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PingServiceChecker implements ServiceChecker {
    private static final Logger logger = LoggerFactory.getLogger(PingServiceChecker.class);
    private static final int TIMEOUT_SECONDS = 5;

    /** Remember if the ping binary exists - avoids a failed exec per check. */
    private final AtomicBoolean pingBinaryMissing = new AtomicBoolean(false);

    @Override
    public boolean check(Service service) {
        try {
            logger.debug("Pinging host: {}", service.getHost());

            // Prefer the system ping binary: InetAddress.isReachable() cannot
            // send ICMP without CAP_NET_RAW and silently falls back to TCP
            // port 7 (echo) - a service virtually nothing runs, which would
            // report live hosts as OFFLINE. The app runs unprivileged, so the
            // setuid/dgram ping binary is the only reliable ICMP path.
            if (!pingBinaryMissing.get()) {
                try {
                    ShellCommandUtil.CommandResult result = ShellCommandUtil.execute(
                            List.of("ping", "-c", "1", "-w", "3", service.getHost()),
                            TIMEOUT_SECONDS + 3);
                    boolean reachable = result.exitCode() == 0;
                    if (!reachable) {
                        logger.debug("ping exited {} for {}", result.exitCode(), service.getHost());
                    }
                    return reachable;
                } catch (java.io.IOException e) {
                    if (isMissingBinary(e)) {
                        logger.warn("ping binary not found - falling back to InetAddress.isReachable()");
                        pingBinaryMissing.set(true);
                    } else {
                        logger.debug("ping command failed for {}: {}", service.getHost(), e.getMessage());
                        return false;
                    }
                }
            }

            InetAddress address = InetAddress.getByName(service.getHost());
            return address.isReachable(TIMEOUT_SECONDS * 1000);
        } catch (Exception e) {
            logger.debug("Ping check failed for {}: {}", service.getName(), e.getMessage());
            return false;
        }
    }

    private boolean isMissingBinary(java.io.IOException e) {
        String msg = e.getMessage();
        return msg != null && msg.contains("Cannot run program");
    }
}
