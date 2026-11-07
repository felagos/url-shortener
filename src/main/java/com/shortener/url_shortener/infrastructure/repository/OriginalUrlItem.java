package com.shortener.url_shortener.infrastructure.repository;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * DynamoDB item of the `original_urls` table: SHA-256 of the original URL -> short URL.
 * Its partition key guarantees that an original URL has at most one short URL.
 */
@DynamoDbBean
public class OriginalUrlItem {

    private String originalUrlHash;
    private String originalUrl;
    private String shortUrl;

    @DynamoDbPartitionKey
    public String getOriginalUrlHash() {
        return originalUrlHash;
    }

    public void setOriginalUrlHash(String originalUrlHash) {
        this.originalUrlHash = originalUrlHash;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }
}
