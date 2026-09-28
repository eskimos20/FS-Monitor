package com.fsmonitor.app.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShellCommandUtilTest {

    @Test
    void escapeForBashWrapsInSingleQuotes() {
        assertEquals("'hello'", ShellCommandUtil.escapeForBash("hello"));
        assertEquals("''", ShellCommandUtil.escapeForBash(null));
        assertEquals("''", ShellCommandUtil.escapeForBash(""));
    }

    @Test
    void escapeForBashNeutralizesInjection() {
        // Embedded quotes are escaped so ; and $(...) cannot break out of quoting
        assertEquals("'a'\"'\"'b'", ShellCommandUtil.escapeForBash("a'b"));

        String escaped = ShellCommandUtil.escapeForBash("'; rm -rf /; '");
        assertTrue(escaped.startsWith("'") && escaped.endsWith("'"));
        assertFalse(escaped.contains("'rm")); // the ; stays inside quoting
    }

    @Test
    void argvExecutionReturnsExitCodeAndOutput() throws Exception {
        var result = ShellCommandUtil.execute(java.util.List.of("echo", "hello"), 10);
        assertEquals(0, result.exitCode());
        assertEquals(java.util.List.of("hello"), result.output());
    }

    @Test
    void nonzeroExitCodeIsReported() throws Exception {
        var result = ShellCommandUtil.execute(java.util.List.of("false"), 10);
        assertEquals(1, result.exitCode());
    }
}
