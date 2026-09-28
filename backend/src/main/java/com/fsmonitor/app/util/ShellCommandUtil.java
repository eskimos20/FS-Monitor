package com.fsmonitor.app.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

        List<String> output = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
        }

        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Command timed out after " + timeoutSeconds + " seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Command was interrupted", e);
        }

        return new CommandResult(process.exitValue(), output);
    }
}
