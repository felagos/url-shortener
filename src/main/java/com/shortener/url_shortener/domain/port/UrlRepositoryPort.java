package com.shortener.url_shortener.domain.port;

import com.shortener.url_shortener.domain.model.Url;
import java.util.Optional;

/**
 * Port (interface) for URL repository operations.
 * This interface defines what the domain needs from the persistence layer.
 */
public interface UrlRepositoryPort {
    
    /**
     * Save a URL to the repository
     * 
     * @param url The URL entity to save
     * @return The saved URL entity
     */
    Url save(Url url);
    
    /**
     * Find a URL by its original URL
     * 
     * @param originalUrl The original URL to search for
     * @return An Optional containing the URL if found, or empty if not found
     */
    Optional<Url> findByOriginalUrl(String originalUrl);

    Optional<Url> findByShortUrl(String shortUrl);
}