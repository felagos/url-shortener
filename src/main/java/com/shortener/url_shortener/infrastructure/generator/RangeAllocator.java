package com.shortener.url_shortener.infrastructure.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hands out unique numbers from a block reserved to the shared {@link BlockSource}.
 * Numbers are served from memory; the coordinator is only called once per block.
 * Numbers left in the block when the instance stops are never used (gaps, not duplicates).
 */
public class RangeAllocator {

    private static final Logger log = LoggerFactory.getLogger(RangeAllocator.class);

    private final BlockSource blockSource;
    private final int blockSize;

    private long next;
    private long end;

    public RangeAllocator(BlockSource blockSource, int blockSize) {
        if (blockSize <= 0) {
            throw new IllegalArgumentException("Block size must be positive");
        }
        this.blockSource = blockSource;
        this.blockSize = blockSize;
    }

    public synchronized long next() {
        // The first block is reserved lazily, so an instance that never shortens wastes none
        if (next >= end) {
            next = blockSource.reserve(blockSize);
            end = next + blockSize;
            log.info("Reserved short code block [{}, {})", next, end);
        }
        return next++;
    }
}
