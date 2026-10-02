package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.LogCacheService;
import com.fsmonitor.app.dto.LogMatch;
import com.fsmonitor.app.entity.LogConfig;
import com.fsmonitor.app.repository.LogConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
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

    private Path gzip(String name, String... contentLines) throws IOException {
        Path gz = tempDir.resolve(name);
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) {
            out.write((String.join("\n", contentLines) + "\n").getBytes(StandardCharsets.UTF_8));
        }
        return gz;
    }

    private static boolean zgrepAvailable() {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(File.pathSeparator)) {
            if (Files.isExecutable(Path.of(dir, "zgrep"))) {
                return true;
            }
        }
        return false;
    }

    @Test
    void gzipFileIsScannedViaZgrep() throws IOException {
        assumeTrue(zgrepAvailable(), "zgrep is required to scan .gz archives");
        Path gz = gzip("old.log.gz",
                "archived failure 4001 first",   // word-matches =4001
                "compressed x4001x embedded",    // must not word-match
                "benign trailer line");

        List<LogMatch> matches = search("=4001");

        String gzPath = gz.toAbsolutePath().toString();
        List<LogMatch> gzMatches = matches.stream()
                .filter(m -> m.getFileName().equals(gzPath))
                .toList();
        assertEquals(1, gzMatches.size(),
                "only the whole-word line inside the .gz archive may match");
        LogMatch m = gzMatches.get(0);
        assertEquals(1, m.getLineNumber());
        assertTrue(m.getMatchedLine().contains("4001 first"));
        // context lines are fetched from the decompressed stream via zgrep -n
        assertTrue(m.getContextAfter().contains("compressed x4001x embedded"));
        assertTrue(m.getContextAfter().contains("benign trailer line"));
    }

    @Test
    void gzipAndPlainFilesAreSearchedTogether() throws IOException {
        assumeTrue(zgrepAvailable(), "zgrep is required to scan .gz archives");
        gzip("old.log.gz", "archive hit 4001");

        List<LogMatch> matches = search("=4001");
        assertEquals(3, matches.size(),
                "2 plain-file word matches + 1 inside the .gz archive");
    }

    @Test
    void gzipOnlyDirectoryNeverInvokesGrepOnStdin() throws IOException {
        assumeTrue(zgrepAvailable(), "zgrep is required to scan .gz archives");
        // A directory with ONLY .gz files: the plain-grep batch is empty and
        // must return immediately - "grep <patterns> --" with no files would
        // block reading stdin until the 30s command timeout.
        Path gzDir = Files.createDirectory(tempDir.resolve("gzonly"));
        Path gz = gzDir.resolve("only.gz");
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(gz))) {
            out.write("hit ZVF123 in archive\n".getBytes(StandardCharsets.UTF_8));
        }

        LogConfig c = config("ZVF123");
        c.setPath(gzDir.toString());
        when(logConfigRepository.findById(1L)).thenReturn(Optional.of(c));
        when(logConfigRepository.save(any())).thenReturn(c);

        long start = System.nanoTime();
        List<LogMatch> matches = service.searchLogs(1L);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertEquals(1, matches.size());
        assertEquals(gz.toAbsolutePath().toString(), matches.get(0).getFileName());
        assertTrue(elapsedMs < 20_000,
                "search must not block on grep stdin; took " + elapsedMs + "ms");
    }

    @Test
    void gzSuffixStripsForFileTypeMatching() {
        assertTrue(LogMonitoringService.matchesFileType("a.log.gz", List.of(".log")));
        assertTrue(LogMonitoringService.matchesFileType("a.log.gz", List.of("gz")));
        assertTrue(LogMonitoringService.matchesFileType("a.gz", List.of("gz")));
        assertFalse(LogMonitoringService.matchesFileType("a.log.gz", List.of(".txt")));
        assertTrue(LogMonitoringService.matchesFileType("a.log.gz", List.of()));
    }
}
