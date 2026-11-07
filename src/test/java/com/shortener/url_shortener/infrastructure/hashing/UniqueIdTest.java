package com.shortener.url_shortener.infrastructure.hashing;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UniqueIdTest {

    private final UniqueId uniqueId = new UniqueId();

    @Test
    void generatesSevenBase62Characters() {
        assertThat(uniqueId.hash("https://www.example.com")).matches("[0-9A-Za-z]{7}");
    }

    @Test
    void doesNotRepeatAcrossManyCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            codes.add(uniqueId.hash("https://www.example.com"));
        }

        assertThat(codes).hasSize(10_000);
    }
}
