package com.shortener.url_shortener.domain.exceptions;

/**
 * Exception thrown when no free short URL could be generated after all retries.
 */
public class ShortUrlGenerationException extends BaseException {

    public ShortUrlGenerationException(String message) {
        super(message, 503);
    }

}
