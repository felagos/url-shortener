package com.shortener.url_shortener.infrastructure.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

/**
 * Data Transfer Object for URL shortening requests.
 */
public class ShortenUrlDto {

    @NotEmpty(message = "URL cannot be empty")
    @Pattern(
            regexp = "^(https?:\\/\\/)([a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?\\.)+" +
                    "([a-zA-Z0-9]{2,})(\\.[a-zA-Z]{2,})+(:\\d+)?(\\/[\\w\\-\\.~!$&'()*+,;=:@\\/]*)*$",
            message = "Invalid URL format"
    )
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}