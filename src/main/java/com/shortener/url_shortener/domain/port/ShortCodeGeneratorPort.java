package com.shortener.url_shortener.domain.port;

/**
 * Port (interface) for generating short codes.
 * Codes do not depend on the URL: each call returns a new code.
 */
public interface ShortCodeGeneratorPort {

    /**
     * Generate a new short code. Implementations must not repeat codes,
     * including across different instances of the application.
     *
     * @return A new short code
     */
    String generate();
}
