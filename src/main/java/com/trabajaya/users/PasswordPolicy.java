package com.trabajaya.users;

import java.nio.charset.StandardCharsets;

/** BCrypt has a 72-byte input limit; character count alone is insufficient. */
public final class PasswordPolicy {
    private PasswordPolicy() { }

    public static boolean valid(String value) {
        return value != null && value.length() >= 8
                && value.getBytes(StandardCharsets.UTF_8).length <= 72
                && value.matches("(?s).*[A-Za-z].*")
                && value.matches("(?s).*[0-9].*");
    }

    public static boolean validAdministrator(String value) {
        return valid(value) && value.length() >= 12;
    }
}
