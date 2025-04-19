package com.shortener.url_shortener.application;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.shortener.url_shortener.domain.exceptions.UrlNotFoundException;
import com.shortener.url_shortener.domain.model.Url;
import com.shortener.url_shortener.domain.port.UrlRepositoryPort;

@Service
public class GetUrlUseCase {

    private final UrlRepositoryPort urlRepository;

    public GetUrlUseCase(UrlRepositoryPort urlRepository) {
        this.urlRepository = urlRepository;
    }

    /**
     * Retrieves the original URL associated with a given hash.
     *
     * @param hash The shortened URL hash to look up
     * @return The original URL string that corresponds to the hash
     * @throws UrlNotFoundException if no URL is found for the given hash
     */
    public String getOriginalUrl(String hash) {
        Optional<Url> url = urlRepository.findByShortUrl(hash);

        return url.map((el) -> el.getOriginalUrl())
                .orElseThrow(() -> new UrlNotFoundException("URL with hash " + hash + " not found", 404));
    }

}
