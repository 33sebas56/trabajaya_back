package com.trabajaya.storage;

import java.util.UUID;
import java.util.regex.Pattern;

/** Application-generated keys only: no arbitrary URLs or traversal components. */
public final class ObjectKey {
    private static final Pattern KEY = Pattern.compile("[1-9][0-9]*/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png");
    private ObjectKey() { }

    public static String create(long owner, String id) {
        if (owner < 1) throw new IllegalArgumentException("Invalid owner");
        String key = owner + "/" + UUID.fromString(id) + ".png";
        return require(key);
    }

    public static String require(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return key;
    }
}
