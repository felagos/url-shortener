package com.shortener.url_shortener.domain.model;

/**
 * Domain entity representing a shortened URL.
 */
public class Url {
    private String id;
    private String originalUrl;
    private String shortUrl;

    public Url(String originalUrl, String shortUrl) {
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
    }

    public Url(String id, String originalUrl, String shortUrl) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
    }

    public String getId() {
        return id;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }
}