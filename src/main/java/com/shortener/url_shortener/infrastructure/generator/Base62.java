package com.shortener.url_shortener.infrastructure.generator;

/**
 * Encodes a non-negative number in base62, left-padded with '0' to a fixed length.
 */
public final class Base62 {

    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = CHARSET.length();

    private Base62() {
    }

    public static String encode(long value, int length) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must not be negative: " + value);
        }

        char[] chars = new char[length];
        long remaining = value;
        for (int i = length - 1; i >= 0; i--) {
            chars[i] = CHARSET.charAt((int) (remaining % BASE));
            remaining /= BASE;
        }

        if (remaining != 0) {
            throw new IllegalArgumentException(value + " does not fit in " + length + " base62 characters");
        }
        return new String(chars);
    }
}
