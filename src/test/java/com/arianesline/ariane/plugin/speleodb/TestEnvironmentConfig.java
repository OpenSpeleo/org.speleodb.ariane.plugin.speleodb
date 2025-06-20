package com.arianesline.ariane.plugin.speleodb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration loader for SpeleoDB API tests
 * Loads environment variables from .env file with secure credential handling
 * FAILS IMMEDIATELY if .env file is not found - no silent failures!
 */
public class TestEnvironmentConfig {
    
    // Configuration keys
    public static final String SPELEODB_INSTANCE_URL = "SPELEODB_INSTANCE_URL";
    public static final String SPELEODB_OAUTH_TOKEN = "SPELEODB_OAUTH_TOKEN";
    public static final String SPELEODB_EMAIL = "SPELEODB_EMAIL";
    public static final String SPELEODB_PASSWORD = "SPELEODB_PASSWORD";
    public static final String API_TEST_ENABLED = "API_TEST_ENABLED";
    public static final String API_TIMEOUT_MS = "API_TIMEOUT_MS";
    public static final String API_RETRY_COUNT = "API_RETRY_COUNT";
    
    private static final Map<String, String> config = new HashMap<>();
    private static boolean configLoaded = false;
    private static String envFilePath = null;
    
    static {
        loadConfiguration();
    }
    
    /**
     * Load configuration from .env file
     * FAILS IMMEDIATELY if .env file is not found!
     */
    private static void loadConfiguration() {
        if (configLoaded) {
            return;
        }
        
        // Search for .env file in multiple locations
        String[] searchPaths = {
            ".env",                                                    // Current directory
            "com.arianesline.ariane.plugin.speleoDB/.env",            // From monorepo root
            "../.env",                                                 // Parent directory
            "../../.env"                                               // Grandparent directory
        };
        
        Path envFile = null;
        for (String searchPath : searchPaths) {
            Path candidate = Path.of(searchPath).toAbsolutePath().normalize();
            if (Files.exists(candidate)) {
                envFile = candidate;
                envFilePath = candidate.toString();
                break;
            }
        }
        
        // IMMEDIATE FAILURE if .env file not found
        if (envFile == null) {
            String errorMessage = """
                
                ❌ CRITICAL ERROR: .env file NOT FOUND!
                
                The SpeleoDB API tests require a .env file with your credentials.
                
                📍 Searched locations:
                %s
                
                🔧 TO FIX THIS:
                
                1. Copy the template file:
                   cp env.dist .env
                
                2. Edit .env with your SpeleoDB credentials:
                   SPELEODB_INSTANCE_URL=https://your-speleodb-instance.com
                   SPELEODB_OAUTH_TOKEN=your-token-here
                   # OR
                   SPELEODB_EMAIL=your-email@example.com
                   SPELEODB_PASSWORD=your-password
                   
                   API_TEST_ENABLED=true
                
                3. Ensure .env is in one of these locations:
                   - Project root: .env
                   - SpeleoDB module: com.arianesline.ariane.plugin.speleoDB/.env
                
                ⚠️  IMPORTANT: Never commit your .env file to version control!
                
                """.formatted(String.join("\n                ", 
                    java.util.Arrays.stream(searchPaths)
                        .map(path -> "• " + Path.of(path).toAbsolutePath().normalize())
                        .toArray(String[]::new)));
            
            throw new RuntimeException(errorMessage);
        }
        
        // Load the .env file
        try {
            System.out.println("✓ Loading .env file from: " + envFile);
            
            Files.lines(envFile)
                .filter(line -> !line.trim().isEmpty())
                .filter(line -> !line.trim().startsWith("#"))
                .forEach(line -> {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        
                        // Remove quotes if present
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        
                        // Normalize instance URL for consistency
                        if (SPELEODB_INSTANCE_URL.equals(key)) {
                            value = normalizeInstanceUrl(value);
                        }
                        
                        config.put(key, value);
                    }
                });
            
            configLoaded = true;
            
            // Validate critical configuration immediately
            validateCriticalConfig();
            
        } catch (IOException e) {
            String errorMessage = """
                
                ❌ CRITICAL ERROR: Failed to read .env file!
                
                File found at: %s
                Error: %s
                
                🔧 TO FIX THIS:
                
                1. Check file permissions (should be readable)
                2. Verify file is not corrupted
                3. Ensure file uses proper format:
                   KEY=value
                   # Comments start with #
                
                """.formatted(envFile, e.getMessage());
            
            throw new RuntimeException(errorMessage, e);
        }
    }
    
    /**
     * Validate that critical configuration is present
     * FAILS IMMEDIATELY if required config is missing!
     */
    private static void validateCriticalConfig() {
        StringBuilder errors = new StringBuilder();
        
        // Check instance URL
        String instanceUrl = get(SPELEODB_INSTANCE_URL);
        if (instanceUrl == null || instanceUrl.trim().isEmpty()) {
            errors.append("• SPELEODB_INSTANCE_URL is required\n");
        } else {
            // Validate URL format - can be with or without protocol
            String url = instanceUrl.trim();
            if (!isValidInstanceUrl(url)) {
                errors.append("• SPELEODB_INSTANCE_URL must be a valid hostname or URL (e.g., 'localhost:8000', '127.0.0.1:8000', or 'https://api.speleodb.org')\n");
            }
        }
        
        // Check authentication credentials
        String oauthToken = get(SPELEODB_OAUTH_TOKEN);
        String email = get(SPELEODB_EMAIL);
        String password = get(SPELEODB_PASSWORD);
        
        boolean hasOAuth = oauthToken != null && !oauthToken.trim().isEmpty();
        boolean hasEmailPassword = email != null && !email.trim().isEmpty() && 
                                  password != null && !password.trim().isEmpty();
        
        if (!hasOAuth && !hasEmailPassword) {
            errors.append("• Authentication required: Either SPELEODB_OAUTH_TOKEN OR both SPELEODB_EMAIL and SPELEODB_PASSWORD\n");
        }
        
        // Check if API testing is disabled
        if (!isApiTestEnabled()) {
            errors.append("• API_TEST_ENABLED=false - tests will be skipped\n");
        }
        
        if (errors.length() > 0) {
            String errorMessage = """
                
                ❌ CRITICAL ERROR: Invalid .env configuration!
                
                📍 Configuration file: %s
                
                🔧 CONFIGURATION ERRORS:
                %s
                
                📋 EXAMPLE VALID .env FILE:
                
                # SpeleoDB Instance (protocol is auto-detected)
                SPELEODB_INSTANCE_URL=your-speleodb-instance.com
                # OR for local development:
                # SPELEODB_INSTANCE_URL=127.0.0.1:8000
                # SPELEODB_INSTANCE_URL=localhost:8000
                
                # Authentication (choose ONE method)
                SPELEODB_OAUTH_TOKEN=your-oauth-token-here
                # OR
                SPELEODB_EMAIL=your-email@example.com
                SPELEODB_PASSWORD=your-password
                
                # Enable testing
                API_TEST_ENABLED=true
                
                # Optional settings
                API_TIMEOUT_MS=10000
                API_RETRY_COUNT=3
                
                ⚠️  Fix these errors and try again!
                
                """.formatted(envFilePath, errors.toString());
            
            throw new RuntimeException(errorMessage);
        }
        
        System.out.println("✓ .env configuration validated successfully");
    }
    
    /**
     * Get configuration value
     */
    public static String get(String key) {
        if (!configLoaded) {
            loadConfiguration();
        }
        return config.get(key);
    }
    
    /**
     * Get configuration value with default
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get integer configuration value
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid integer value for " + key + ": " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get boolean configuration value
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim());
    }
    
    /**
     * Get double configuration value
     */
    public static double getDouble(String key, double defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid double value for " + key + ": " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Check if API testing is enabled
     */
    public static boolean isApiTestEnabled() {
        return getBoolean(API_TEST_ENABLED, true);
    }
    
    /**
     * Check if required configuration is present
     */
    public static boolean hasRequiredConfig() {
        try {
            validateCriticalConfig();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
    
    /**
     * Print configuration status (with masked credentials)
     */
    public static void printConfigStatus() {
        if (!configLoaded) {
            loadConfiguration();
        }
        
        System.out.println("=== SpeleoDB API Test Configuration ===");
        System.out.println("Configuration file: " + (envFilePath != null ? envFilePath : "NOT FOUND"));
        System.out.println("Instance URL: " + maskSensitive(get(SPELEODB_INSTANCE_URL)));
        System.out.println("Has OAuth Token: " + (get(SPELEODB_OAUTH_TOKEN) != null && !get(SPELEODB_OAUTH_TOKEN).isEmpty()));
        System.out.println("Has Email/Password: " + (get(SPELEODB_EMAIL) != null && !get(SPELEODB_EMAIL).isEmpty() && 
                                                    get(SPELEODB_PASSWORD) != null && !get(SPELEODB_PASSWORD).isEmpty()));
        System.out.println("API Test Enabled: " + isApiTestEnabled());
        System.out.println("API Timeout: " + getInt(API_TIMEOUT_MS, 10000) + "ms");
        System.out.println("API Retry Count: " + getInt(API_RETRY_COUNT, 3));
        System.out.println("Required Config Present: " + hasRequiredConfig());
        System.out.println("========================================");
    }
    
    /**
     * Normalize instance URL by removing protocol and trailing slashes
     * This ensures consistent format that SpeleoDB service expects
     */
    private static String normalizeInstanceUrl(String url) {
        if (url == null) {
            return null;
        }
        
        String normalized = url.trim();
        
        // Remove http:// or https:// protocol
        if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8);
        } else if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7);
        }
        
        // Remove trailing slash
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        
        return normalized;
    }
    
    /**
     * Validate instance URL format
     * Accepts URLs with or without protocol since SpeleoDB service handles protocol detection
     */
    private static boolean isValidInstanceUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = url.trim();
        
        // Allow URLs with http:// or https:// protocol
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return true;
        }
        
        // Allow hostnames/IPs without protocol (e.g., "localhost:8000", "127.0.0.1:8000", "api.speleodb.org")
        // Basic validation: should contain at least a hostname
        if (trimmed.contains(" ") || trimmed.contains("\t") || trimmed.contains("\n")) {
            return false; // No whitespace allowed
        }
        
        // Should have some basic hostname structure
        return trimmed.length() > 0 && !trimmed.startsWith(".") && !trimmed.endsWith(".");
    }
    
    /**
     * Mask sensitive information for logging
     */
    private static String maskSensitive(String value) {
        if (value == null || value.isEmpty()) {
            return "(not set)";
        }
        if (value.length() <= 8) {
            return "*".repeat(value.length());
        }
        return value.substring(0, 4) + "*".repeat(value.length() - 8) + value.substring(value.length() - 4);
    }
} 