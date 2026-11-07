package com.shortener.url_shortener.application;

import com.shortener.url_shortener.domain.exceptions.ShortUrlGenerationException;
import com.shortener.url_shortener.domain.exceptions.UrlWriteConflictException;
import com.shortener.url_shortener.domain.model.Url;
import com.shortener.url_shortener.domain.port.ConfigurationPort;
import com.shortener.url_shortener.domain.port.HashingServicePort;
import com.shortener.url_shortener.domain.port.UrlRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UrlShortenUseCaseTest {

    private static final String DOMAIN = "https://short.test";
    private static final String ORIGINAL = "https://www.example.com/page";

    private UrlRepositoryPort repository;
    private HashingServicePort hashing;
    private UrlShortenUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(UrlRepositoryPort.class);
        hashing = mock(HashingServicePort.class);
        ConfigurationPort config = mock(ConfigurationPort.class);
        when(config.getString("domain-shortener")).thenReturn(DOMAIN);
        when(repository.findByOriginalUrl(ORIGINAL)).thenReturn(Optional.empty());

        useCase = new UrlShortenUseCase(repository, hashing, config);
    }

    @Test
    void returnsExistingCodeWithoutSaving() {
        when(repository.findByOriginalUrl(ORIGINAL)).thenReturn(Optional.of(new Url("abc1234", ORIGINAL, "abc1234")));

        assertThat(useCase.shortenUrl(ORIGINAL)).isEqualTo(DOMAIN + "/abc1234");
        verify(repository, never()).save(any());
    }

    @Test
    void retriesWithNewCodeWhenCodeIsTaken() {
        when(hashing.hash(ORIGINAL)).thenReturn("taken01", "free002");
        when(repository.save(any()))
                .thenThrow(new UrlWriteConflictException("taken01"))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThat(useCase.shortenUrl(ORIGINAL)).isEqualTo(DOMAIN + "/free002");
        verify(repository, times(2)).save(any());
    }

    @Test
    void returnsCodeSavedByAnotherInstanceWhenLosingTheRace() {
        // Nothing stored on the first lookup; another instance saves the same URL before us
        when(repository.findByOriginalUrl(ORIGINAL))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new Url("other01", ORIGINAL, "other01")));
        when(hashing.hash(ORIGINAL)).thenReturn("mine001");
        when(repository.save(any())).thenThrow(new UrlWriteConflictException("mine001"));

        assertThat(useCase.shortenUrl(ORIGINAL)).isEqualTo(DOMAIN + "/other01");
        verify(repository, times(1)).save(any());
    }

    @Test
    void failsWith503AfterMaxAttempts() {
        when(hashing.hash(ORIGINAL)).thenReturn("taken01");
        when(repository.save(any())).thenThrow(new UrlWriteConflictException("taken01"));

        assertThatThrownBy(() -> useCase.shortenUrl(ORIGINAL))
                .isInstanceOf(ShortUrlGenerationException.class)
                .extracting(e -> ((ShortUrlGenerationException) e).getCode())
                .isEqualTo(503);
        verify(repository, times(UrlShortenUseCase.MAX_ATTEMPTS)).save(any());
    }
}
