package com.shortener.url_shortener.infrastructure.hashing;
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import java.security.SecureRandom;

public class UniqueId implements IHashing {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    @Override
    public String hash(String input) {
        var random = new SecureRandom();
        return NanoIdUtils.randomNanoId(random, ALPHABET.toCharArray(), 7);
    }

}
