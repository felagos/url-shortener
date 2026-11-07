package com.shortener.url_shortener.infrastructure.config;

import com.shortener.url_shortener.infrastructure.repository.UrlItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    private static final String TABLE_NAME = "urls";

    @Bean
    public DynamoDbClient dynamoDbClient(
            @Value("${dynamodb.region}") String region,
            @Value("${dynamodb.endpoint:}") String endpoint) {

        DynamoDbClientBuilder builder = DynamoDbClient.builder().region(Region.of(region));

        // A custom endpoint means DynamoDB Local, which accepts any credentials
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local", "local")));
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    @Bean
    public DynamoDbTable<UrlItem> urlTable(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbClient client,
            @Value("${dynamodb.create-table:false}") boolean createTable) {

        DynamoDbTable<UrlItem> table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(UrlItem.class));

        if (createTable && !tableExists(client)) {
            table.createTable(CreateTableEnhancedRequest.builder()
                    .globalSecondaryIndices(EnhancedGlobalSecondaryIndex.builder()
                            .indexName(UrlItem.ORIGINAL_URL_HASH_INDEX)
                            .projection(p -> p.projectionType(ProjectionType.ALL))
                            .build())
                    .build());
            client.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME));
        }

        return table;
    }

    private boolean tableExists(DynamoDbClient client) {
        try {
            client.describeTable(r -> r.tableName(TABLE_NAME));
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
}
