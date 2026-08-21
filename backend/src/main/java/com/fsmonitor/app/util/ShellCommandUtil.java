package com.fsmonitor.app.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility for safely building and executing Linux shell commands.
 */
public final class ShellCommandUtil {

    private ShellCommandUtil() {
        // Utility class
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

    public static class CommandResult {
        private final int exitCode;
        private final List<String> output;

        public CommandResult(int exitCode, List<String> output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public List<String> getOutput() {
            return output;
        }
    }

    /**
     * Execute a bash command with a timeout. Returns exit code and stdout lines.
     * If the process does not finish in time it is forcibly destroyed.
     */
    public static CommandResult execute(String command, long timeoutSeconds) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        List<String> output = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
        }

        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Shell command timed out after " + timeoutSeconds + " seconds: " + command);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Shell command was interrupted: " + command, e);
        }

        return new CommandResult(process.exitValue(), output);
    }
}
