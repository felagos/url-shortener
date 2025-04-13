package com.shortener.url_shortener.infrastructure.config;

import com.shortener.url_shortener.domain.port.ConfigurationPort;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Spring-based adapter for the configuration port.
 * This class implements the configuration port using Spring's Environment.
 */
@Component
public class SpringConfigurationAdapter implements ConfigurationPort {

    private final Environment environment;

    public SpringConfigurationAdapter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String getString(String key) {
        return environment.getProperty(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

    @Override
    public Integer getInteger(String key) {
        return environment.getProperty(key, Integer.class);
    }

    @Override
    public Boolean getBoolean(String key) {
        return environment.getProperty(key, Boolean.class);
    }
}