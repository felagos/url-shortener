package com.shortener.url_shortener;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "dynamodb.create-table=false")
class UrlShortenerApplicationTests {

	@Test
	void contextLoads() {
	}

}
