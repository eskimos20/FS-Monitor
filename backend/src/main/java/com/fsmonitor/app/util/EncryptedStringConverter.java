package com.fsmonitor.app.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter that transparently encrypts/decrypts sensitive string columns
 * (passwords, private keys) using AES-256-GCM. Encrypted values are stored
 * with an "ENC:" prefix; legacy plaintext values are read as-is and will be
 * upgraded to encrypted form the next time the entity is written.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return Secrets.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return Secrets.decrypt(dbData);
    }
}
