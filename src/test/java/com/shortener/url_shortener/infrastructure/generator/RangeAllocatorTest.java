package com.shortener.url_shortener.infrastructure.generator;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RangeAllocatorTest {

    /** In-memory coordinator: an atomic counter, like the DynamoDB `ADD`. */
    private static class FakeBlockSource implements BlockSource {
        final AtomicLong next = new AtomicLong();
        final AtomicInteger reservations = new AtomicInteger();

        @Override
        public long reserve(int size) {
            reservations.incrementAndGet();
            return next.getAndAdd(size);
        }
    }

    @Test
    void servesTheBlockInOrderAndReservesTheNextWhenExhausted() {
        var source = new FakeBlockSource();
        source.next.set(500);
        var allocator = new RangeAllocator(source, 3);

        assertThat(allocator.next()).isEqualTo(500);
        assertThat(allocator.next()).isEqualTo(501);
        assertThat(allocator.next()).isEqualTo(502);
        assertThat(source.reservations).hasValue(1);

        assertThat(allocator.next()).isEqualTo(503);
        assertThat(source.reservations).hasValue(2);
    }

    @Test
    void instancesSharingTheCoordinatorNeverRepeatNumbers() throws Exception {
        var source = new FakeBlockSource();
        // 4 "instances" with their own allocator, each used by 2 threads
        RangeAllocator[] instances = {
                new RangeAllocator(source, 1000), new RangeAllocator(source, 1000),
                new RangeAllocator(source, 1000), new RangeAllocator(source, 1000)
        };
        Set<Long> numbers = ConcurrentHashMap.newKeySet();

        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int t = 0; t < 8; t++) {
            RangeAllocator allocator = instances[t % instances.length];
            pool.submit(() -> {
                for (int i = 0; i < 10_000; i++) {
                    numbers.add(allocator.next());
                }
            });
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(numbers).hasSize(80_000);
    }
}
