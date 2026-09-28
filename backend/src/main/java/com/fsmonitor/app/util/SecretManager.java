package com.fsmonitor.app.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Manages application secrets. Resolution order for each secret:
 *   1. Explicit configuration property / environment variable
 *   2. A previously generated secret file in the data directory
 *   3. A newly generated random secret persisted to the data directory
 *
 * Secret files are created with owner-only permissions (0600) so that
 * generated secrets survive restarts without being committed to source control.
 */
@Component
public class SecretManager {

    private static final Logger logger = LoggerFactory.getLogger(SecretManager.class);

    private final Path dataDir;
    private final String jwtSecretProperty;
    private final String encryptionKeyProperty;

    private String jwtSecret;
    private String encryptionKey;

    public SecretManager(
            @Value("${jwt.secret:}") String jwtSecretProperty,
            @Value("${fsmonitor.encryption-key:}") String encryptionKeyProperty,
            @Value("${fsmonitor.data-dir:./data}") String dataDir) {
        this.jwtSecretProperty = jwtSecretProperty;
        this.encryptionKeyProperty = encryptionKeyProperty;
        this.dataDir = Paths.get(dataDir);
    }

    @PostConstruct
    public void init() {
        this.jwtSecret = resolveSecret(jwtSecretProperty, "jwt.secret", "JWT_SECRET");
        this.encryptionKey = resolveSecret(encryptionKeyProperty, "encryption.key", "FS_MONITOR_ENCRYPTION_KEY");
        Secrets.initialize(encryptionKey);
    }

    /** Secret used for signing JWT tokens. */
    public String getJwtSecret() {
        return jwtSecret;
    }

    /** Secret used for encrypting credentials at rest. */
    public String getEncryptionKey() {
        return encryptionKey;
    }

    private String resolveSecret(String configured, String fileName, String envName) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        try {
            Path secretFile = dataDir.resolve(fileName);
            if (Files.exists(secretFile)) {
                String stored = Files.readString(secretFile, StandardCharsets.UTF_8).trim();
                if (!stored.isEmpty()) {
                    logger.debug("Loaded {} from {}", fileName, secretFile);
                    return stored;
                }
            }
            String generated = generateSecret();
            persistSecret(secretFile, generated);
            logger.info("Generated new {} and stored it in {} (set {} to override)", fileName, secretFile, envName);
            return generated;
        } catch (IOException e) {
            // Fall back to an ephemeral secret rather than failing startup
            logger.warn("Could not persist {} ({}), using an ephemeral secret - " +
                    "set environment variable {} for a stable secret", fileName, e.getMessage(), envName);
            return generateSecret();
        }
    }

    private void persistSecret(Path secretFile, String secret) throws IOException {
        Files.createDirectories(secretFile.getParent());
        Files.writeString(secretFile, secret, StandardCharsets.UTF_8);
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(secretFile, perms);
        } catch (UnsupportedOperationException e) {
            // Non-POSIX filesystem - permissions not supported
        }
    }

    private static String generateSecret() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
