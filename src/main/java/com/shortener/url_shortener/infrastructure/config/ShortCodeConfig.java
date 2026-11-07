package com.shortener.url_shortener.infrastructure.config;

import com.shortener.url_shortener.infrastructure.generator.BlockSource;
import com.shortener.url_shortener.infrastructure.generator.FeistelPermutation;
import com.shortener.url_shortener.infrastructure.generator.RangeAllocator;
import com.shortener.url_shortener.infrastructure.repository.CounterItem;
import com.shortener.url_shortener.infrastructure.repository.DynamoDbBlockSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class ShortCodeConfig {

    private static final String SHORT_URL_COUNTER = "shortUrl";

    @Bean
    public BlockSource blockSource(DynamoDbClient client, DynamoDbTable<CounterItem> counterTable) {
        return new DynamoDbBlockSource(client, counterTable.tableName(), SHORT_URL_COUNTER);
    }

    @Bean
    public RangeAllocator rangeAllocator(
            BlockSource blockSource,
            @Value("${shortener.block-size:1000}") int blockSize) {
        return new RangeAllocator(blockSource, blockSize);
    }

    @Bean
    public FeistelPermutation feistelPermutation(@Value("${shortener.code-secret:}") String codeSecret) {
        return new FeistelPermutation(codeSecret);
    }
}
