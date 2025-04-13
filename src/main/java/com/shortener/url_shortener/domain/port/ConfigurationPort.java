package com.shortener.url_shortener.domain.port;

/**
 * Port (interface) for configuration values.
 * This interface defines what the domain needs from the configuration system.
 */
public interface ConfigurationPort {
    
    /**
     * Get a string configuration value by key
     * 
     * @param key The configuration key
     * @return The string value or null if not found
     */
    String getString(String key);
    
    /**
     * Get a string configuration value with a default fallback
     * 
     * @param key The configuration key
     * @param defaultValue The default value to use if key is not found
     * @return The string value or defaultValue if not found
     */
    String getString(String key, String defaultValue);
    
    /**
     * Get an integer configuration value by key
     * 
     * @param key The configuration key
     * @return The integer value or null if not found
     */
    Integer getInteger(String key);
    
    /**
     * Get a boolean configuration value by key
     * 
     * @param key The configuration key
     * @return The boolean value or null if not found
     */
    Boolean getBoolean(String key);
}