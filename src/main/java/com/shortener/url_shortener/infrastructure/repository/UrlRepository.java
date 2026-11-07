package com.shortener.url_shortener.infrastructure.repository;

import com.shortener.url_shortener.domain.exceptions.ShortUrlAlreadyExistsException;
import com.shortener.url_shortener.domain.model.Url;
import com.shortener.url_shortener.domain.port.UrlRepositoryPort;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * DynamoDB adapter for the URL repository.
 * This class implements the repository port defined in the domain layer.
 */
@Repository
public class UrlRepository implements UrlRepositoryPort {

    private final DynamoDbTable<UrlItem> table;

    public UrlRepository(DynamoDbTable<UrlItem> table) {
        this.table = table;
    }

    @Override
    public Url save(Url url) {
        UrlItem item = new UrlItem();
        item.setShortUrl(url.getShortUrl());
        item.setOriginalUrl(url.getOriginalUrl());
        item.setOriginalUrlHash(sha256(url.getOriginalUrl()));

        // Never overwrite an existing short URL
        try {
            table.putItem(PutItemEnhancedRequest.builder(UrlItem.class)
                    .item(item)
                    .conditionExpression(Expression.builder()
                            .expression("attribute_not_exists(shortUrl)")
                            .build())
                    .build());
        } catch (ConditionalCheckFailedException e) {
            throw new ShortUrlAlreadyExistsException(url.getShortUrl());
        }

        return toDomain(item);
    }

    @Override
    public Optional<Url> findByOriginalUrl(String originalUrl) {
        var key = Key.builder().partitionValue(sha256(originalUrl)).build();

        return table.index(UrlItem.ORIGINAL_URL_HASH_INDEX)
                .query(QueryConditional.keyEqualTo(key))
                .stream()
                .flatMap(page -> page.items().stream())
                // The index is keyed by hash, so confirm the real URL matches
                .filter(item -> originalUrl.equals(item.getOriginalUrl()))
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public Optional<Url> findByShortUrl(String shortUrl) {
        UrlItem item = table.getItem(Key.builder().partitionValue(shortUrl).build());

        return Optional.ofNullable(item).map(this::toDomain);
    }

    private Url toDomain(UrlItem item) {
        return new Url(item.getShortUrl(), item.getOriginalUrl(), item.getShortUrl());
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
