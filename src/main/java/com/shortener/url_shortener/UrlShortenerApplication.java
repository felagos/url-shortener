package com.shortener.url_shortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application class for the URL Shortener service.
 * Uses hexagonal architecture with domain, application, and infrastructure layers.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.shortener.url_shortener"})
public class UrlShortenerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortenerApplication.class, args);
	}

}
