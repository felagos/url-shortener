package com.shortener.url_shortener.infrastructure.repository;

import com.shortener.url_shortener.domain.exceptions.UrlWriteConflictException;
import com.shortener.url_shortener.domain.model.Url;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CachingUrlRepositoryTest {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String CODE = "21SEd9K";
    private static final String ORIGINAL = "https://www.example.com/page";
    private static final String CODE_KEY = "url:code:" + CODE;
    private static final String ORIGINAL_KEY = "url:orig:" + UrlHashing.sha256(ORIGINAL);

    private UrlRepository dynamo;
    private ValueOperations<String, String> values;
    private MutableClock clock;
    private CachingUrlRepository repository;

    /** Clock the test can move forward, to check the retry window after a Redis failure. */
    private static class MutableClock extends Clock {
        long millis = 1_000_000;

        @Override public long millis() { return millis; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        dynamo = mock(UrlRepository.class);
        values = mock(ValueOperations.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenReturn(values);
        clock = new MutableClock();

        repository = new CachingUrlRepository(dynamo, redis, TTL, clock);
    }

    @Test
    void cacheHitDoesNotReadDynamo() {
        when(values.get(CODE_KEY)).thenReturn(ORIGINAL);
        when(values.get(ORIGINAL_KEY)).thenReturn(CODE);

        assertThat(repository.findByShortUrl(CODE)).get().extracting(Url::getOriginalUrl).isEqualTo(ORIGINAL);
        assertThat(repository.findByOriginalUrl(ORIGINAL)).get().extracting(Url::getShortUrl).isEqualTo(CODE);
        verifyNoInteractions(dynamo);
    }

    @Test
    void cacheMissReadsDynamoAndCachesTheResult() {
        when(dynamo.findByShortUrl(CODE)).thenReturn(Optional.of(new Url(CODE, ORIGINAL, CODE)));
        when(dynamo.findByOriginalUrl(ORIGINAL)).thenReturn(Optional.of(new Url(CODE, ORIGINAL, CODE)));

        assertThat(repository.findByShortUrl(CODE)).isPresent();
        assertThat(repository.findByOriginalUrl(ORIGINAL)).isPresent();

        verify(values).set(CODE_KEY, ORIGINAL, TTL);
        verify(values).set(ORIGINAL_KEY, CODE, TTL);
    }

    @Test
    void absencesAreNotCached() {
        when(dynamo.findByShortUrl(CODE)).thenReturn(Optional.empty());
        when(dynamo.findByOriginalUrl(ORIGINAL)).thenReturn(Optional.empty());

        assertThat(repository.findByShortUrl(CODE)).isEmpty();
        assertThat(repository.findByOriginalUrl(ORIGINAL)).isEmpty();

        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void successfulSaveCachesBothMappings() {
        var url = new Url(ORIGINAL, CODE);
        when(dynamo.save(url)).thenReturn(new Url(CODE, ORIGINAL, CODE));

        repository.save(url);

        verify(values).set(CODE_KEY, ORIGINAL, TTL);
        verify(values).set(ORIGINAL_KEY, CODE, TTL);
    }

    @Test
    void conflictingSaveCachesNothing() {
        var url = new Url(ORIGINAL, CODE);
        when(dynamo.save(url)).thenThrow(new UrlWriteConflictException("conflict"));

        assertThatThrownBy(() -> repository.save(url)).isInstanceOf(UrlWriteConflictException.class);

        verifyNoInteractions(values);
    }

    @Test
    void redisFailureFallsBackToDynamoAndSkipsRedisForAWhile() {
        when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        when(dynamo.findByShortUrl(CODE)).thenReturn(Optional.of(new Url(CODE, ORIGINAL, CODE)));

        // First call hits the failure and still answers from DynamoDB
        assertThat(repository.findByShortUrl(CODE)).get().extracting(Url::getOriginalUrl).isEqualTo(ORIGINAL);
        // Within the retry window Redis is not touched at all
        assertThat(repository.findByShortUrl(CODE)).isPresent();
        verify(values, times(1)).get(anyString());
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));

        // After the window Redis is tried again; once it recovers, it is used
        clock.millis += CachingUrlRepository.RETRY_AFTER.toMillis();
        reset(values);
        when(values.get(CODE_KEY)).thenReturn(ORIGINAL);

        assertThat(repository.findByShortUrl(CODE)).isPresent();
        verify(dynamo, times(2)).findByShortUrl(CODE);
    }
}
