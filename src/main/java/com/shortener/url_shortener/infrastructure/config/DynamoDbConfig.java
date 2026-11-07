package com.shortener.url_shortener.infrastructure.config;

import com.shortener.url_shortener.infrastructure.repository.OriginalUrlItem;
import com.shortener.url_shortener.infrastructure.repository.UrlItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    private static final String URLS_TABLE = "urls";
    private static final String ORIGINAL_URLS_TABLE = "original_urls";

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

        DynamoDbTable<UrlItem> table = enhancedClient.table(URLS_TABLE, TableSchema.fromBean(UrlItem.class));

        if (createTable) {
            createIfMissing(client, table);
        }

        return table;
    }

    @Bean
    public DynamoDbTable<OriginalUrlItem> originalUrlTable(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbClient client,
            @Value("${dynamodb.create-table:false}") boolean createTable) {

        DynamoDbTable<OriginalUrlItem> table =
                enhancedClient.table(ORIGINAL_URLS_TABLE, TableSchema.fromBean(OriginalUrlItem.class));

        if (createTable) {
            createIfMissing(client, table);
        }

        return table;
    }

    /**
     * Safe when several instances start at once: if another one creates the table first,
     * its ResourceInUseException is ignored, and every instance waits until the table is ACTIVE
     * (it may still be CREATING when we see it).
     */
    private void createIfMissing(DynamoDbClient client, DynamoDbTable<?> table) {
        if (!tableExists(client, table.tableName())) {
            try {
                table.createTable();
            } catch (ResourceInUseException e) {
                // Another instance created it between our check and our create
            }
        }
        client.waiter().waitUntilTableExists(r -> r.tableName(table.tableName()));
    }

    private boolean tableExists(DynamoDbClient client, String tableName) {
        try {
            client.describeTable(r -> r.tableName(tableName));
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
}
