package com.shortener.url_shortener.domain.port;

/**
 * Port (interface) for the hashing service.
 * This interface defines what the domain needs from the hashing service.
 */
public interface HashingServicePort {
    
    /**
     * Hash an input string to produce a unique shortened URL hash
     * 
     * @param input The input string to hash
     * @return The hashed output string
     */
    String hash(String input);
}