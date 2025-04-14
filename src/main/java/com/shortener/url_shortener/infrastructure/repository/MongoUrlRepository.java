package com.shortener.url_shortener.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository interface for URL documents.
 */
public interface MongoUrlRepository extends MongoRepository<UrlDocument, String> {
    
    /**
     * Find a URL document by its original URL
     * 
     * @param originalUrl The original URL to search for
     * @return An Optional containing the URL document if found, or empty if not found
     */
    Optional<UrlDocument> findByOriginalUrl(String originalUrl);

    Optional<UrlDocument> findByShortUrl(String shortUrl);
}