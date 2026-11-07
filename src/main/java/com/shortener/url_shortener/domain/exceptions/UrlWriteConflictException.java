package com.shortener.url_shortener.domain.exceptions;

/**
 * Thrown by the repository when a write conflicts with stored data (the short URL is taken
 * or the original URL already has one) or with a concurrent write on the same items.
 * Signals the caller to look up again and retry; it is not an HTTP error.
 */
public class UrlWriteConflictException extends RuntimeException {

    public UrlWriteConflictException(String message) {
        super(message);
    }

}
