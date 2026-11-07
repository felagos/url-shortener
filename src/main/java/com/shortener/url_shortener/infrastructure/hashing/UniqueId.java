package com.shortener.url_shortener.infrastructure.hashing;
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import java.security.SecureRandom;

public class UniqueId implements IHashing {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int LENGTH = 7;
    // SecureRandom is thread-safe, so one instance is shared across requests
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String hash(String input) {
        return NanoIdUtils.randomNanoId(RANDOM, ALPHABET, LENGTH);
    }

}
