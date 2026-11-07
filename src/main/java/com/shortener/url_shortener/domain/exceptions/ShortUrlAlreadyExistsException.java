package com.shortener.url_shortener.domain.exceptions;

/**
 * Thrown by the repository when a short URL is already taken.
 * Signals the caller to retry with a new code; it is not an HTTP error.
 */
public class ShortUrlAlreadyExistsException extends RuntimeException {

    public ShortUrlAlreadyExistsException(String shortUrl) {
        super("Short URL " + shortUrl + " already exists");
    }

}
