package com.shortener.url_shortener.infrastructure.hashing;

public class Base62 implements IHashing {
    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = CHARSET.length();
    private static final int SHORT_URL_LENGTH = 7;

    @Override
    public String hash(String input) {
        byte[] bytes = input.getBytes();
        long value = 0;
        for (byte b : bytes) {
            value = (value << 8) | (b & 0xFF);
            if (value < 0) {
                value = Math.abs(value);
            }
        }

        StringBuilder sb = new StringBuilder();
        do {
            sb.insert(0, CHARSET.charAt((int)(value % BASE)));
            value /= BASE;
        } while (value > 0 && sb.length() < SHORT_URL_LENGTH);

        return sb.toString();
    }

}