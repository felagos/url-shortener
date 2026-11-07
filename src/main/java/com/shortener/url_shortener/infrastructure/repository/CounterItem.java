package com.shortener.url_shortener.infrastructure.repository;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * DynamoDB item of the `counters` table: the next free number of a named counter.
 * Only used to create the table; updates go through {@link DynamoDbBlockSource}.
 */
@DynamoDbBean
public class CounterItem {

    private String name;
    private Long next;

    @DynamoDbPartitionKey
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getNext() {
        return next;
    }

    public void setNext(Long next) {
        this.next = next;
    }
}
