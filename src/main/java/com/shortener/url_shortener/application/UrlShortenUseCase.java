package com.shortener.url_shortener.application;

import com.shortener.url_shortener.domain.exceptions.ShortUrlAlreadyExistsException;
import com.shortener.url_shortener.domain.exceptions.ShortUrlGenerationException;
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

    static final int MAX_ATTEMPTS = 5;

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
     * Codes are random, so on a collision a new code is tried up to {@link #MAX_ATTEMPTS} times.
     *
     * @param originalUrl The URL to be shortened
     * @return The shortened URL with domain prefix
     * @throws ShortUrlGenerationException if every attempt collided with an existing code
     */
    public String shortenUrl(String originalUrl) {
        Optional<Url> existingUrl = urlRepository.findByOriginalUrl(originalUrl);

        if (existingUrl.isPresent()) {
           return domainShortener + "/" + existingUrl.get().getShortUrl();
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            var hash = hashingService.hash(originalUrl);

            try {
                urlRepository.save(new Url(originalUrl, hash));
                return domainShortener + "/" + hash;
            } catch (ShortUrlAlreadyExistsException e) {
                // Code already taken, try a new one
            }
        }

        throw new ShortUrlGenerationException(
                "Could not generate a unique short URL after " + MAX_ATTEMPTS + " attempts");
    }

}
