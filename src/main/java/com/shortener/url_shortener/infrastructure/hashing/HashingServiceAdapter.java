package com.shortener.url_shortener.infrastructure.hashing;

import com.shortener.url_shortener.domain.port.HashingServicePort;
import org.springframework.stereotype.Service;

/**
 * Adapter implementation of the HashingServicePort that uses the existing hashing implementation.
 */
@Service
public class HashingServiceAdapter implements HashingServicePort {

    private final HashingFactory hashingFactory;

    public HashingServiceAdapter() {
        this.hashingFactory = HashingFactory.getInstance();
    }

    @Override
    public String hash(String input) {
        return hashingFactory.getHasher(HashingType.BASE62).hash(input);
    }
}