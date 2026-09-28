package com.fsmonitor.app.util;

import java.util.regex.Pattern;

/**
 * Central password policy: minimum 8 characters, at least one letter and one digit.
 * Used by bean validation (sign-up / password change) and re-checked in AuthService
 * so the rule cannot be bypassed through a different code path.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final String DESCRIPTION =
            "Password must be at least " + MIN_LENGTH + " characters and contain both letters and numbers";

    private static final Pattern PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{" + MIN_LENGTH + ",}$");

    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        return password != null && PATTERN.matcher(password).matches();
    }

    public static void validate(String password) {
        if (!isValid(password)) {
            throw new IllegalArgumentException(DESCRIPTION);
        }
    }
}
