package com.shortener.url_shortener.infrastructure.controller;

import com.shortener.url_shortener.application.RedirectUrlUseCase;
import com.shortener.url_shortener.application.UrlShortenUseCase;
import com.shortener.url_shortener.domain.exceptions.UrlNotFoundException;
import com.shortener.url_shortener.infrastructure.dto.ShortenUrlDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * REST Controller for URL shortening operations.
 */
@RestController
@RequestMapping("/shortener")
public class UrlShortenerController {

    private final UrlShortenUseCase urlShortenerService;
    private final RedirectUrlUseCase redirectUrlService;

    public UrlShortenerController(UrlShortenUseCase urlShortenerService, RedirectUrlUseCase redirectUrlService) {
        this.urlShortenerService = urlShortenerService;
        this.redirectUrlService = redirectUrlService;
    }

    /**
     * Endpoint to shorten a URL.
     * 
     * @param request The request containing the URL to shorten
     * @return The shortened URL
     */
    @PostMapping
    public String shortenUrl(@RequestBody @Valid ShortenUrlDto request) {
        return this.urlShortenerService.shortenUrl(request.getUrl());
    }

    /**
     * Endpoint to redirect to the original URL with a 301 (Moved Permanently) status code.
     * 
     * @param hash The hash of the shortened URL
     * @return A redirect view that sends the user to the original URL with 301 status code
     * @throws UrlNotFoundException if the URL with the given hash is not found
     */
    @GetMapping("/{hash}")
    public RedirectView getOriginalUrl(@PathVariable String hash) {
        String originalUrl = this.redirectUrlService.getOriginalUrl(hash);
        
        RedirectView redirectView = new RedirectView(originalUrl);
        redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        
        return redirectView;
    }
}