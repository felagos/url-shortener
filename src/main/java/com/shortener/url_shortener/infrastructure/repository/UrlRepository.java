package com.shortener.url_shortener.infrastructure.repository;

import com.shortener.url_shortener.domain.model.Url;
import com.shortener.url_shortener.domain.port.UrlRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB adapter for the URL repository.
 * This class implements the repository port defined in the domain layer.
 */
@Repository
public class UrlRepository implements UrlRepositoryPort {

    private final MongoUrlRepository repository;

    public UrlRepository(MongoUrlRepository repository) {
        this.repository = repository;
    }

    @Override
    public Url save(Url url) {
        UrlDocument document = new UrlDocument();
        document.setOriginalUrl(url.getOriginalUrl());
        document.setShortUrl(url.getShortUrl());
        
        UrlDocument savedDocument = repository.save(document);
        return new Url(
            savedDocument.getId(),
            savedDocument.getOriginalUrl(),
            savedDocument.getShortUrl()
        );
    }

    @Override
    public Optional<Url> findByOriginalUrl(String originalUrl) {
        return repository.findByOriginalUrl(originalUrl)
                .map(doc -> new Url(doc.getId(), doc.getOriginalUrl(), doc.getShortUrl()));
    }
}