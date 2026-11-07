package com.shortener.url_shortener.infrastructure.repository;

import com.shortener.url_shortener.domain.exceptions.UrlWriteConflictException;
import com.shortener.url_shortener.domain.model.Url;
import com.shortener.url_shortener.domain.port.UrlRepositoryPort;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

/**
 * DynamoDB adapter for the URL repository.
 * This class implements the repository port defined in the domain layer.
 */
@Repository
public class UrlRepository implements UrlRepositoryPort {

    // Cancellation reasons meaning "someone else wrote this data first"
    private static final Set<String> CONFLICT_REASONS = Set.of("ConditionalCheckFailed", "TransactionConflict");

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<UrlItem> urlTable;
    private final DynamoDbTable<OriginalUrlItem> originalUrlTable;

    public UrlRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbTable<UrlItem> urlTable,
            DynamoDbTable<OriginalUrlItem> originalUrlTable) {
        this.enhancedClient = enhancedClient;
        this.urlTable = urlTable;
        this.originalUrlTable = originalUrlTable;
    }

    @Override
    public Url save(Url url) {
        UrlItem urlItem = new UrlItem();
        urlItem.setShortUrl(url.getShortUrl());
        urlItem.setOriginalUrl(url.getOriginalUrl());

        OriginalUrlItem originalUrlItem = new OriginalUrlItem();
        originalUrlItem.setOriginalUrlHash(sha256(url.getOriginalUrl()));
        originalUrlItem.setOriginalUrl(url.getOriginalUrl());
        originalUrlItem.setShortUrl(url.getShortUrl());

        // Both items or neither: a short URL is never reused and an original URL never gets two
        try {
            enhancedClient.transactWriteItems(TransactWriteItemsEnhancedRequest.builder()
                    .addPutItem(urlTable, TransactPutItemEnhancedRequest.builder(UrlItem.class)
                            .item(urlItem)
                            .conditionExpression(notExists("shortUrl"))
                            .build())
                    .addPutItem(originalUrlTable, TransactPutItemEnhancedRequest.builder(OriginalUrlItem.class)
                            .item(originalUrlItem)
                            .conditionExpression(notExists("originalUrlHash"))
                            .build())
                    .build());
        } catch (TransactionCanceledException e) {
            boolean conflict = e.cancellationReasons().stream()
                    .map(CancellationReason::code)
                    .anyMatch(CONFLICT_REASONS::contains);

            if (conflict) {
                throw new UrlWriteConflictException("Conflict saving short URL " + url.getShortUrl());
            }
            throw e;
        }

        return toDomain(urlItem);
    }

    @Override
    public Optional<Url> findByOriginalUrl(String originalUrl) {
        OriginalUrlItem item = originalUrlTable.getItem(GetItemEnhancedRequest.builder()
                .key(Key.builder().partitionValue(sha256(originalUrl)).build())
                .consistentRead(true)
                .build());

        return Optional.ofNullable(item)
                // The key is a hash, so confirm the real URL matches
                .filter(found -> originalUrl.equals(found.getOriginalUrl()))
                .map(found -> new Url(found.getShortUrl(), found.getOriginalUrl(), found.getShortUrl()));
    }

    @Override
    public Optional<Url> findByShortUrl(String shortUrl) {
        // Strongly consistent so a code is found right after another instance created it
        UrlItem item = urlTable.getItem(GetItemEnhancedRequest.builder()
                .key(Key.builder().partitionValue(shortUrl).build())
                .consistentRead(true)
                .build());

        return Optional.ofNullable(item).map(this::toDomain);
    }

    private Url toDomain(UrlItem item) {
        return new Url(item.getShortUrl(), item.getOriginalUrl(), item.getShortUrl());
    }

    private static Expression notExists(String attribute) {
        return Expression.builder().expression("attribute_not_exists(" + attribute + ")").build();
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
