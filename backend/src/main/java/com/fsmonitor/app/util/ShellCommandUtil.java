package com.fsmonitor.app.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility for safely building and executing Linux commands.
 * All executions enforce a timeout so monitoring threads can never
 * block indefinitely on a hung subprocess.
 */
public final class ShellCommandUtil {

    private ShellCommandUtil() {
    }

    /**
     * Escapes a value for use inside a single-quoted bash argument.
     * Wraps the value in single quotes and escapes any embedded single quotes.
     */
    public static String escapeForBash(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    public record CommandResult(int exitCode, List<String> output) {
    }

    /**
     * Execute a bash command with a timeout. Returns exit code and stdout lines.
     * If the process does not finish in time it is forcibly destroyed.
     */
    public static CommandResult execute(String command, long timeoutSeconds) throws IOException {
        return run(new ProcessBuilder("bash", "-c", command), timeoutSeconds);
    }

    /**
     * Execute a command without a shell (no interpolation, no injection risk).
     */
    public static CommandResult execute(List<String> argv, long timeoutSeconds) throws IOException {
        return run(new ProcessBuilder(argv), timeoutSeconds);
    }

    private static CommandResult run(ProcessBuilder pb, long timeoutSeconds) throws IOException {
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Drain stdout/stderr on a separate thread. Reading synchronously here
        // would block until the child closes the stream - a hung process would
        // suspend the caller forever and the timeout below would never apply.
        List<String> output = Collections.synchronizedList(new ArrayList<>());
        Thread drainThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            } catch (IOException ignored) {
                // Expected when the process is forcibly destroyed on timeout
            }
        });
        drainThread.setDaemon(true);
        drainThread.start();

        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                throw new IOException("Command timed out after " + timeoutSeconds + " seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Command was interrupted", e);
        }

        // Collect the tail of the output the drain thread may still be reading
        try {
            drainThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return new CommandResult(process.exitValue(), List.copyOf(output));
    }
}
