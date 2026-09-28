package com.fsmonitor.app.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretsTest {

    @BeforeAll
    static void init() {
        Secrets.initialize("test-secret-key-for-unit-tests");
    }

    @Test
    void encryptDecryptRoundTrip() {
        String secret = "s3cr3t-password";
        String encrypted = Secrets.encrypt(secret);

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC:"));
        assertNotEquals(secret, encrypted);
        assertEquals(secret, Secrets.decrypt(encrypted));
    }

    @Test
    void encryptionIsNotDeterministic() {
        // GCM uses a random IV - same plaintext must produce different ciphertext
        assertNotEquals(Secrets.encrypt("same"), Secrets.encrypt("same"));
    }

    @Test
    void nullAndEmptyPassThrough() {
        assertNull(Secrets.encrypt(null));
        assertEquals("", Secrets.encrypt(""));
        assertNull(Secrets.decrypt(null));
        assertEquals("", Secrets.decrypt(""));
    }

    @Test
    void legacyPlaintextDecryptsAsIs() {
        // Values written before encryption was introduced have no ENC: prefix
        assertEquals("old-plaintext", Secrets.decrypt("old-plaintext"));
    }

    @Test
    void unicodeRoundTrip() {
        String s = "lösenord-åäö-密码";
        assertEquals(s, Secrets.decrypt(Secrets.encrypt(s)));
    }
}
