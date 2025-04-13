package com.shortener.url_shortener.application;

import com.shortener.url_shortener.domain.model.Url;
import com.shortener.url_shortener.domain.port.ConfigurationPort;
import com.shortener.url_shortener.domain.port.HashingServicePort;
import com.shortener.url_shortener.domain.port.UrlRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Application service that implements the URL shortening use case.
 */
@Service
public class UrlShortenUseCase {

    private final String domainShortener;
    private final UrlRepositoryPort urlRepository;
    private final HashingServicePort hashingService;

    public UrlShortenUseCase(
            UrlRepositoryPort urlRepository,
            HashingServicePort hashingService,
            ConfigurationPort configurationPort) {
        this.urlRepository = urlRepository;
        this.hashingService = hashingService;
        this.domainShortener = configurationPort.getString("domain-shortener");
    }

    /**
     * Shortens a URL by either retrieving an existing short URL or creating a new one.
     *
     * @param originalUrl The URL to be shortened
     * @return The shortened URL with domain prefix
     */
    public String shortenUrl(String originalUrl) {
        Optional<Url> existingUrl = urlRepository.findByOriginalUrl(originalUrl);
        
        if (existingUrl.isPresent()) {
            return domainShortener + "/" + existingUrl.get().getShortUrl();
        }

        String hash = hashingService.hash(originalUrl);
        Url newUrl = new Url(originalUrl, hash);
        urlRepository.save(newUrl);
        
        return domainShortener + "/" + hash;
    }
}