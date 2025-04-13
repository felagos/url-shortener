package com.shortener.url_shortener.infrastructure.hashing;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Base62 implements IHashing {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();
    private static final int SHORT_URL_LENGTH = 7;

    @Override
    public String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));

            BigInteger number = new BigInteger(1, messageDigest);

            StringBuilder sb = new StringBuilder();
            while (number.compareTo(BigInteger.ZERO) > 0) {
                BigInteger[] divRem = number.divideAndRemainder(BigInteger.valueOf(BASE));
                number = divRem[0];
                int remainder = divRem[1].intValue();
                sb.insert(0, ALPHABET.charAt(remainder));
            }

            while (sb.length() < SHORT_URL_LENGTH) {
                sb.insert(0, ALPHABET.charAt(0));
            }

            return sb.length() > SHORT_URL_LENGTH ?
                    sb.substring(0, SHORT_URL_LENGTH) :
                    sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to create hash", e);
        }
    }
}