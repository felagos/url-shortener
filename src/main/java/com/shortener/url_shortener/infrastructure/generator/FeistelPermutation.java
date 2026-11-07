package com.shortener.url_shortener.infrastructure.generator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * Keyed bijection on [0, domain): distinct inputs always give distinct outputs, but consecutive
 * inputs give unrelated outputs, so sequential counters cannot be guessed from the codes.
 *
 * A Feistel network with an HMAC-SHA256 round function is a permutation of [0, 2^(2 * halfBits)).
 * Cycle-walking (re-applying it while the result is outside the domain) restricts it to a
 * permutation of [0, domain).
 *
 * The key must be the same on every instance and must never change: a different key is a
 * different permutation and could reproduce codes already issued.
 */
public class FeistelPermutation {

    /** 62^7: every 7-character base62 code. */
    public static final long SHORT_CODE_DOMAIN = 3_521_614_606_208L;
    private static final int SHORT_CODE_HALF_BITS = 21; // 2^42 >= 62^7

    private static final int ROUNDS = 4;
    private static final String ALGORITHM = "HmacSHA256";

    private final long domain;
    private final int halfBits;
    private final long halfMask;
    private final ThreadLocal<Mac> mac;

    public FeistelPermutation(String key) {
        this(key, SHORT_CODE_DOMAIN, SHORT_CODE_HALF_BITS);
    }

    FeistelPermutation(String key, long domain, int halfBits) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Feistel key must not be empty");
        }
        if (halfBits < 1 || halfBits > 31 || domain < 1 || domain > 1L << (2 * halfBits)) {
            throw new IllegalArgumentException("Domain must fit in 2 * halfBits bits");
        }
        this.domain = domain;
        this.halfBits = halfBits;
        this.halfMask = (1L << halfBits) - 1;

        var keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        // Mac is not thread-safe, so each thread gets its own
        this.mac = ThreadLocal.withInitial(() -> {
            try {
                Mac instance = Mac.getInstance(ALGORITHM);
                instance.init(keySpec);
                return instance;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(ALGORITHM + " not available", e);
            }
        });
    }

    public long permute(long value) {
        if (value < 0 || value >= domain) {
            throw new IllegalArgumentException("Value out of domain: " + value);
        }

        long result = value;
        do {
            result = feistel(result);
        } while (result >= domain);

        return result;
    }

    private long feistel(long value) {
        long left = (value >>> halfBits) & halfMask;
        long right = value & halfMask;

        for (int round = 0; round < ROUNDS; round++) {
            long newRight = left ^ roundFunction(round, right);
            left = right;
            right = newRight;
        }

        return (left << halfBits) | right;
    }

    private long roundFunction(int round, long half) {
        byte[] input = {
                (byte) round,
                (byte) (half >>> 24), (byte) (half >>> 16), (byte) (half >>> 8), (byte) half
        };
        byte[] digest = mac.get().doFinal(input);

        long output = ((digest[0] & 0xFFL) << 24) | ((digest[1] & 0xFFL) << 16)
                | ((digest[2] & 0xFFL) << 8) | (digest[3] & 0xFFL);
        return output & halfMask;
    }
}
