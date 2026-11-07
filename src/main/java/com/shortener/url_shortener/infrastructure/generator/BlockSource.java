package com.shortener.url_shortener.infrastructure.generator;

/**
 * Shared coordinator that hands out blocks of numbers. Every call, from any instance,
 * must return a block that no other call has received.
 */
public interface BlockSource {

    /**
     * Reserve the next block of {@code size} numbers.
     *
     * @param size How many numbers to reserve
     * @return The first number of the block; the block is [start, start + size)
     */
    long reserve(int size);
}
