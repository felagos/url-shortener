package com.shortener.url_shortener.domain.exceptions;

/**
 * Exception thrown when a requested URL is not found in the repository.
 */
public class UrlNotFoundException extends BaseException {

    public UrlNotFoundException(String message, int code) {
        super(message, code);
    }

}