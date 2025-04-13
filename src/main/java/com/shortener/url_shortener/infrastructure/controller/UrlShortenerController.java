package com.shortener.url_shortener.infrastructure.controller;

import com.shortener.url_shortener.application.UrlShortenUseCase;
import com.shortener.url_shortener.infrastructure.dto.ShortenUrlDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for URL shortening operations.
 */
@RestController
@RequestMapping("/shortener")
public class UrlShortenerController {

    private final UrlShortenUseCase urlShortenerService;

    public UrlShortenerController(UrlShortenUseCase urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    /**
     * Endpoint to shorten a URL.
     * 
     * @param request The request containing the URL to shorten
     * @return The shortened URL
     */
    @PostMapping
    public String shortenUrl(@RequestBody @Valid ShortenUrlDto request) {
        return urlShortenerService.shortenUrl(request.getUrl());
    }
}