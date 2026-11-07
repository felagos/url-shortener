package com.shortener.url_shortener.infrastructure.generator;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeistelPermutationTest {

    private final FeistelPermutation permutation = new FeistelPermutation("test-key");

    @Test
    void isDeterministic() {
        assertThat(permutation.permute(12345)).isEqualTo(new FeistelPermutation("test-key").permute(12345));
    }

    @Test
    void consecutiveInputsGiveDistinctInDomainOutputs() {
        Set<Long> outputs = new HashSet<>();
        for (long n = 0; n < 100_000; n++) {
            long output = permutation.permute(n);
            assertThat(output).isBetween(0L, FeistelPermutation.SHORT_CODE_DOMAIN - 1);
            outputs.add(output);
        }

        assertThat(outputs).hasSize(100_000);
    }

    @Test
    void isAFullPermutationOfASmallDomain() {
        // 1000 values inside a 2^10 Feistel domain: cycle-walking must map [0, 1000) onto itself
        var small = new FeistelPermutation("test-key", 1000, 5);
        Set<Long> outputs = new HashSet<>();
        for (long n = 0; n < 1000; n++) {
            outputs.add(small.permute(n));
        }

        assertThat(outputs).hasSize(1000).allMatch(v -> v >= 0 && v < 1000);
    }

    @Test
    void consecutiveInputsAreNotConsecutiveOutputs() {
        int adjacent = 0;
        for (long n = 0; n < 10_000; n++) {
            if (Math.abs(permutation.permute(n + 1) - permutation.permute(n)) == 1) {
                adjacent++;
            }
        }

        assertThat(adjacent).isZero();
    }

    @Test
    void differentKeysGiveDifferentPermutations() {
        var other = new FeistelPermutation("other-key");
        int same = 0;
        for (long n = 0; n < 1000; n++) {
            if (permutation.permute(n) == other.permute(n)) {
                same++;
            }
        }

        assertThat(same).isZero();
    }

    @Test
    void rejectsEmptyKeyAndOutOfDomainValues() {
        assertThatThrownBy(() -> new FeistelPermutation(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> permutation.permute(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> permutation.permute(FeistelPermutation.SHORT_CODE_DOMAIN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
