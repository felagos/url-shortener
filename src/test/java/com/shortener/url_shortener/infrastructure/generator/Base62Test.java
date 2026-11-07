package com.shortener.url_shortener.infrastructure.generator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62Test {

    @Test
    void encodesWithLeftPadding() {
        assertThat(Base62.encode(0, 7)).isEqualTo("0000000");
        assertThat(Base62.encode(61, 7)).isEqualTo("000000z");
        assertThat(Base62.encode(62, 7)).isEqualTo("0000010");
        assertThat(Base62.encode(FeistelPermutation.SHORT_CODE_DOMAIN - 1, 7)).isEqualTo("zzzzzzz");
    }

    @Test
    void rejectsValuesThatDoNotFit() {
        assertThatThrownBy(() -> Base62.encode(FeistelPermutation.SHORT_CODE_DOMAIN, 7))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Base62.encode(-1, 7)).isInstanceOf(IllegalArgumentException.class);
    }
}
