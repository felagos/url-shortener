package com.shortener.url_shortener.infrastructure.generator;

import com.shortener.url_shortener.domain.exceptions.ShortUrlGenerationException;
import com.shortener.url_shortener.domain.port.ShortCodeGeneratorPort;
import org.springframework.stereotype.Service;

/**
 * Short codes from coordinated number ranges: each instance takes numbers from its own block
 * (so instances never repeat a number), scrambles them with a keyed permutation (so codes are
 * not sequential) and encodes them as 7 base62 characters.
 */
@Service
public class RangeShortCodeGenerator implements ShortCodeGeneratorPort {

    private static final int CODE_LENGTH = 7;

    private final RangeAllocator rangeAllocator;
    private final FeistelPermutation permutation;

    public RangeShortCodeGenerator(RangeAllocator rangeAllocator, FeistelPermutation permutation) {
        this.rangeAllocator = rangeAllocator;
        this.permutation = permutation;
    }

    @Override
    public String generate() {
        long number = rangeAllocator.next();

        if (number >= FeistelPermutation.SHORT_CODE_DOMAIN) {
            throw new ShortUrlGenerationException("All " + CODE_LENGTH + "-character short codes are used");
        }

        return Base62.encode(permutation.permute(number), CODE_LENGTH);
    }
}
