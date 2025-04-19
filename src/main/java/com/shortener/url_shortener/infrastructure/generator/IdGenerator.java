package com.shortener.url_shortener.infrastructure.generator;

import com.shortener.url_shortener.domain.port.IdGeneratorPort;
import org.springframework.stereotype.Service;

@Service
public class IdGenerator implements IdGeneratorPort {
    
    // Snowflake bits allocation
    private static final long EPOCH = 1672531200000L; // Custom epoch (Jan 1, 2023 UTC)
    private static final int TIMESTAMP_BITS = 41;
    private static final int WORKER_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;
    
    private static final int WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final int TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;
    
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    
    private long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    public IdGenerator() {
        // Default constructor, using workerId = 1
        this(1L);
    }
    
    public IdGenerator(long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("Worker ID must be between 0 and %d", MAX_WORKER_ID));
        }
        this.workerId = workerId;
    }
    
    @Override
    public String generateId(String input) {
        var id = nextId();
        return String.valueOf(id);
    }

    /**
     * Generate the next unique ID using Snowflake algorithm
     */
    private synchronized long nextId() {
        long currentTimestamp = getCurrentTimestamp();
        
        // Handle clock moving backwards
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards. Refusing to generate ID for %d milliseconds",
                            lastTimestamp - currentTimestamp));
        }
        
        // If same timestamp, increment sequence
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // Sequence exhausted, wait for next millisecond
            if (sequence == 0) {
                currentTimestamp = waitForNextMillis(lastTimestamp);
            }
        } else {
            // Different timestamp, reset sequence
            sequence = 0L;
        }
        
        lastTimestamp = currentTimestamp;
        
        // Compose the 64-bit ID
        return ((currentTimestamp - EPOCH) << TIMESTAMP_SHIFT) |
               (workerId << WORKER_ID_SHIFT) |
               sequence;
    }
    
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    private long waitForNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }
}
