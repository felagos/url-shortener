package com.shortener.url_shortener.domain.port;

import com.shortener.url_shortener.domain.exceptions.UrlWriteConflictException;
import com.shortener.url_shortener.domain.model.Url;
import java.util.Optional;

/**
 * Port (interface) for URL repository operations.
 * This interface defines what the domain needs from the persistence layer.
 */
public interface UrlRepositoryPort {

    /**
     * Atomically save a URL: either both the short URL and the original URL are stored, or neither is.
     *
     * @param url The URL entity to save
     * @return The saved URL entity
     * @throws UrlWriteConflictException if the short URL is taken, the original URL already has
     *         a short URL, or a concurrent write touched the same data
     */
    Url save(Url url);

    /**
     * Find a URL by its original URL. The read is strongly consistent, so it sees every
     * completed {@link #save}, including those made by other instances.
     *
     * @param originalUrl The original URL to search for
     * @return An Optional containing the URL if found, or empty if not found
     */
    Optional<Url> findByOriginalUrl(String originalUrl);

    Optional<Url> findByShortUrl(String shortUrl);
}
