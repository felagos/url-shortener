package com.shortener.url_shortener.application;

import com.shortener.url_shortener.domain.exceptions.ShortUrlGenerationException;
import com.shortener.url_shortener.domain.exceptions.UrlWriteConflictException;
import com.shortener.url_shortener.domain.model.Url;
import com.shortener.url_shortener.domain.port.ConfigurationPort;
import com.shortener.url_shortener.domain.port.ShortCodeGeneratorPort;
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
    private final ShortCodeGeneratorPort shortCodeGenerator;

    public UrlShortenUseCase(
            UrlRepositoryPort urlRepository,
            ShortCodeGeneratorPort shortCodeGenerator,
            ConfigurationPort configurationPort) {
        this.urlRepository = urlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.domainShortener = configurationPort.getString("domain-shortener");
    }

    /**
     * Shortens a URL by either retrieving an existing short URL or creating a new one.
     * On a write conflict (code taken, or another instance shortened the same URL first)
     * it looks up again and retries, up to {@link #MAX_ATTEMPTS} times.
     *
     * @param originalUrl The URL to be shortened
     * @return The shortened URL with domain prefix
     * @throws ShortUrlGenerationException if every attempt hit a conflict
     */
    public String shortenUrl(String originalUrl) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Optional<Url> existingUrl = urlRepository.findByOriginalUrl(originalUrl);

            if (existingUrl.isPresent()) {
               return domainShortener + "/" + existingUrl.get().getShortUrl();
            }

            var code = shortCodeGenerator.generate();

            try {
                urlRepository.save(new Url(originalUrl, code));
                return domainShortener + "/" + code;
            } catch (UrlWriteConflictException e) {
                // Someone wrote first: the next lookup returns their code, or we try a new one
            }
        }

        throw new ShortUrlGenerationException(
                "Could not generate a unique short URL after " + MAX_ATTEMPTS + " attempts");
    }

}
