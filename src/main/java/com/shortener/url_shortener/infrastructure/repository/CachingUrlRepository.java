package com.shortener.url_shortener.infrastructure.repository;

import com.shortener.url_shortener.domain.model.Url;
import com.shortener.url_shortener.domain.port.UrlRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Redis read cache in front of the DynamoDB {@link UrlRepository}.
 *
 * Both mappings (code -> URL and URL -> code) never change once saved, so cached entries are never
 * stale and need no invalidation. Absences are NOT cached: a missing code or URL may be created an
 * instant later by another instance. Writes and their uniqueness guarantees stay in DynamoDB; the
 * cache is only filled after a successful save.
 *
 * Fail-open: if Redis is unavailable, reads go to DynamoDB. After a failure Redis is skipped for
 * {@link #RETRY_AFTER} so an outage does not add a timeout to every request.
 */
@Primary
@Repository
public class CachingUrlRepository implements UrlRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(CachingUrlRepository.class);

    static final Duration RETRY_AFTER = Duration.ofSeconds(5);
    private static final String CODE_KEY = "url:code:";
    private static final String ORIGINAL_KEY = "url:orig:";

    private final UrlRepository delegate;
    private final StringRedisTemplate redis;
    private final Duration ttl;
    private final Clock clock;

    private volatile long unavailableUntil;
    private volatile boolean unavailable;

    @Autowired
    public CachingUrlRepository(
            UrlRepository delegate,
            StringRedisTemplate redis,
            @Value("${cache.ttl:24h}") Duration ttl) {
        this(delegate, redis, ttl, Clock.systemUTC());
    }

    CachingUrlRepository(UrlRepository delegate, StringRedisTemplate redis, Duration ttl, Clock clock) {
        this.delegate = delegate;
        this.redis = redis;
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public Url save(Url url) {
        Url saved = delegate.save(url);

        cache(CODE_KEY + saved.getShortUrl(), saved.getOriginalUrl());
        cache(ORIGINAL_KEY + UrlHashing.sha256(saved.getOriginalUrl()), saved.getShortUrl());
        return saved;
    }

    @Override
    public Optional<Url> findByShortUrl(String shortUrl) {
        String key = CODE_KEY + shortUrl;
        String cachedOriginalUrl = read(key);

        if (cachedOriginalUrl != null) {
            return Optional.of(new Url(shortUrl, cachedOriginalUrl, shortUrl));
        }

        Optional<Url> url = delegate.findByShortUrl(shortUrl);
        url.ifPresent(found -> cache(key, found.getOriginalUrl()));
        return url;
    }

    @Override
    public Optional<Url> findByOriginalUrl(String originalUrl) {
        String key = ORIGINAL_KEY + UrlHashing.sha256(originalUrl);
        String cachedShortUrl = read(key);

        if (cachedShortUrl != null) {
            return Optional.of(new Url(cachedShortUrl, originalUrl, cachedShortUrl));
        }

        Optional<Url> url = delegate.findByOriginalUrl(originalUrl);
        url.ifPresent(found -> cache(key, found.getShortUrl()));
        return url;
    }

    private String read(String key) {
        return withRedis(() -> redis.opsForValue().get(key));
    }

    private void cache(String key, String value) {
        withRedis(() -> {
            redis.opsForValue().set(key, value, ttl);
            return null;
        });
    }

    /** Runs a Redis operation, or skips it and returns null while Redis is considered down. */
    private <T> T withRedis(Supplier<T> operation) {
        if (clock.millis() < unavailableUntil) {
            return null;
        }

        try {
            T result = operation.get();
            if (unavailable) {
                unavailable = false;
                log.info("Redis cache available again");
            }
            return result;
        } catch (DataAccessException e) {
            unavailableUntil = clock.millis() + RETRY_AFTER.toMillis();
            if (!unavailable) {
                unavailable = true;
                log.warn("Redis cache unavailable, reading from DynamoDB (retrying every {}s): {}",
                        RETRY_AFTER.toSeconds(), e.getMessage());
            }
            return null;
        }
    }
}
