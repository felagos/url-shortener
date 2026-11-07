package com.shortener.url_shortener.infrastructure.repository;

import com.shortener.url_shortener.infrastructure.generator.BlockSource;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

import java.util.Map;

/**
 * Range coordinator backed by a counter item in DynamoDB.
 * `ADD next :size` is atomic, so concurrent reservations from any instance get disjoint blocks.
 * If the item does not exist yet, ADD creates it starting from 0.
 */
public class DynamoDbBlockSource implements BlockSource {

    private final DynamoDbClient client;
    private final String tableName;
    private final String counterName;

    public DynamoDbBlockSource(DynamoDbClient client, String tableName, String counterName) {
        this.client = client;
        this.tableName = tableName;
        this.counterName = counterName;
    }

    @Override
    public long reserve(int size) {
        var response = client.updateItem(r -> r
                .tableName(tableName)
                .key(Map.of("name", AttributeValue.fromS(counterName)))
                .updateExpression("ADD #next :size")
                .expressionAttributeNames(Map.of("#next", "next"))
                .expressionAttributeValues(Map.of(":size", AttributeValue.fromN(Integer.toString(size))))
                .returnValues(ReturnValue.UPDATED_NEW));

        long newNext = Long.parseLong(response.attributes().get("next").n());
        return newNext - size;
    }
}
