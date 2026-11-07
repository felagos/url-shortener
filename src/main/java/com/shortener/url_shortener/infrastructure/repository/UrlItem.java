package com.shortener.url_shortener.infrastructure.repository;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/**
 * DynamoDB item model for URL entities.
 */
@DynamoDbBean
public class UrlItem {

    public static final String ORIGINAL_URL_HASH_INDEX = "originalUrlHash-index";

    private String shortUrl;
    private String originalUrl;
    private String originalUrlHash;

    @DynamoDbPartitionKey
    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = ORIGINAL_URL_HASH_INDEX)
    public String getOriginalUrlHash() {
        return originalUrlHash;
    }

    public void setOriginalUrlHash(String originalUrlHash) {
        this.originalUrlHash = originalUrlHash;
    }
}
