package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.LogCacheService;
import com.fsmonitor.app.dto.LogMatch;
import com.fsmonitor.app.entity.LogConfig;
import com.fsmonitor.app.repository.LogConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogMonitoringServiceTest {

    @TempDir
    Path tempDir;

    private LogConfigRepository logConfigRepository;
    private LogMonitoringService service;

    private Path logFile;

    @BeforeEach
    void setUp() throws IOException {
        logConfigRepository = mock(LogConfigRepository.class);
        service = new LogMonitoringService(logConfigRepository, mock(LogCacheService.class));
        ReflectionTestUtils.setField(service, "commandTimeout", 30);

        logFile = tempDir.resolve("app.log");
        Files.writeString(logFile, String.join("\n",
                "call failed with code 4001 - retrying",   // word: spaces around 4001
                "embedded id x4001x must not word-match",   // embedded in a word
                "4001 at line start",                       // word boundary at start
                "id 1245354001345 digits stay embedded"     // embedded in digits
        ) + "\n");
    }

    private LogConfig config(String keywords) {
        LogConfig c = new LogConfig();
        c.setName("test");
        c.setPath(tempDir.toString());
        c.setKeywords(keywords);
        c.setActive(true);
        return c;
    }

    private List<LogMatch> search(String keywords) {
        LogConfig c = config(keywords);
        when(logConfigRepository.findById(1L)).thenReturn(Optional.of(c));
        when(logConfigRepository.save(any())).thenReturn(c);
        return service.searchLogs(1L);
    }

    @Test
    void literalKeywordMatchesSubstringAnywhere() {
        List<LogMatch> matches = search("4001");
        assertEquals(4, matches.size(),
                "literal 4001 matches everywhere, including inside other tokens");
    }

    @Test
    void wordKeywordRequiresWordBoundaries() {
        List<LogMatch> matches = search("=4001");
        assertEquals(2, matches.size(),
                "=4001 must match ' 4001 ' and line-start 4001 only");
        for (LogMatch m : matches) {
            assertTrue(m.getMatchedLine().contains("4001"));
            assertFalse(m.getMatchedLine().contains("x4001x"));
            assertFalse(m.getMatchedLine().contains("1245354001345"));
        }
    }

    @Test
    void mixedKeywordsMatchIndependently() {
        // "=4001" word-matches two lines; "embedded" literal-matches two more
        List<LogMatch> matches = search("=4001,embedded");
        assertEquals(4, matches.size());
    }

    @Test
    void loneEqualsSignStaysLiteral() {
        assertFalse(LogMonitoringService.isWordKeyword("="));
        assertTrue(LogMonitoringService.isWordKeyword("=4001"));
        assertFalse(LogMonitoringService.isWordKeyword("4001"));
    }
}
