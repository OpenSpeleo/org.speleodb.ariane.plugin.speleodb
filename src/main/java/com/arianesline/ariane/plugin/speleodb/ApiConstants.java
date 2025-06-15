package com.arianesline.ariane.plugin.speleodb;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Constants for API endpoints, headers, and URL construction.
 * Centralizes API configuration and provides safe URL building methods.
 */
public final class ApiConstants {
    
    // Prevent instantiation
    private ApiConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // ==================== API STRUCTURE ==================== //
    
    /** Base API version path */
    public static final String API_V1 = "/api/v1";
    
    /** Projects endpoint base */
    public static final String PROJECTS_ENDPOINT = API_V1 + "/projects";
    
    /** Authentication endpoint */
    public static final String AUTH_ENDPOINT = API_V1 + "/user/auth-token/";
    
    /** Upload endpoint suffix */
    public static final String UPLOAD_ENDPOINT = "/upload/ariane_tml/";
    
    /** Download endpoint suffix */
    public static final String DOWNLOAD_ENDPOINT = "/download/ariane_tml/";
    
    /** Mutex acquire endpoint suffix */
    public static final String MUTEX_ACQUIRE_ENDPOINT = "/acquire/";
    
    /** Mutex release endpoint suffix */
    public static final String MUTEX_RELEASE_ENDPOINT = "/release/";
    
    // ==================== HTTP HEADERS ==================== //
    
    /** Authorization header name */
    public static final String AUTHORIZATION_HEADER = "Authorization";
    
    /** Content-Type header name */
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    
    /** Accept header name */
    public static final String ACCEPT_HEADER = "Accept";
    
    /** User-Agent header name */
    public static final String USER_AGENT_HEADER = "User-Agent";
    
    // ==================== CONTENT TYPES ==================== //
    
    /** JSON content type */
    public static final String APPLICATION_JSON = "application/json";
    
    /** Form data content type */
    public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    
    /** Multipart form data content type */
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    
    /** Binary data content type */
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    
    // ==================== AUTHORIZATION SCHEMES ==================== //
    
    /** Token-based authorization prefix */
    public static final String TOKEN_PREFIX = "Token ";
    
    /** Bearer token authorization prefix */
    public static final String BEARER_PREFIX = "Bearer ";
    
    // ==================== USER AGENT ==================== //
    
    /** Default User-Agent string for requests */
    public static final String DEFAULT_USER_AGENT = "SpeleoDBPlugin/1.0 (Ariane Integration)";
    
    // ==================== URL BUILDERS ==================== //
    
    /**
     * Builds the authentication URL for a given SpeleoDB instance.
     * 
     * @param baseUrl the base URL of the SpeleoDB instance
     * @return complete authentication URL
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static URI buildAuthUrl(String baseUrl) {
        return buildUrl(baseUrl, AUTH_ENDPOINT);
    }
    
    /**
     * Builds the projects list URL for a given SpeleoDB instance.
     * 
     * @param baseUrl the base URL of the SpeleoDB instance
     * @return complete projects list URL
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static URI buildProjectsUrl(String baseUrl) {
        return buildUrl(baseUrl, PROJECTS_ENDPOINT + "/");
    }
    
    /**
     * Builds the project upload URL for a specific project.
     * 
     * @param baseUrl the base URL of the SpeleoDB instance
     * @param projectId the project ID
     * @return complete upload URL
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static URI buildUploadUrl(String baseUrl, String projectId) {
        String endpoint = PROJECTS_ENDPOINT + "/" + projectId + UPLOAD_ENDPOINT;
        return buildUrl(baseUrl, endpoint);
    }
    
    /**
     * Builds the project download URL for a specific project.
     * 
     * @param baseUrl the base URL of the SpeleoDB instance
     * @param projectId the project ID
     * @return complete download URL
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static URI buildDownloadUrl(String baseUrl, String projectId) {
        String endpoint = PROJECTS_ENDPOINT + "/" + projectId + DOWNLOAD_ENDPOINT;
        return buildUrl(baseUrl, endpoint);
    }
    
    /**
     * Builds the mutex acquire URL for a specific project.
     * 
     * @param baseUrl the base URL of the SpeleoDB instance
     * @param projectId the project ID
     * @return complete mutex acquire URL
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static URI buildMutexAcquireUrl(String baseUrl, String projectId) {
        String endpoint = PROJECTS_ENDPOINT + "/" + projectId + MUTEX_ACQUIRE_ENDPOINT;
        return buildUrl(baseUrl, endpoint);
    }
    
    /**
     * Builds the mutex release URL for a specific project.
     * 
     * @param baseUrl the base URL of the SpeleoDB instance
     * @param projectId the project ID
     * @return complete mutex release URL
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static URI buildMutexReleaseUrl(String baseUrl, String projectId) {
        String endpoint = PROJECTS_ENDPOINT + "/" + projectId + MUTEX_RELEASE_ENDPOINT;
        return buildUrl(baseUrl, endpoint);
    }
    
    /**
     * Builds a complete URL by combining base URL and endpoint.
     * 
     * @param baseUrl the base URL
     * @param endpoint the API endpoint
     * @return complete URI
     * @throws IllegalArgumentException if the URL is invalid
     */
    private static URI buildUrl(String baseUrl, String endpoint) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint cannot be null");
        }
        
        try {
            // Remove trailing slash from baseUrl if present
            String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            
            // Ensure endpoint starts with slash
            String cleanEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
            
            return new URI(cleanBaseUrl + cleanEndpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL construction: " + baseUrl + endpoint, e);
        }
    }
    
    // ==================== HEADER BUILDERS ==================== //
    
    /**
     * Creates a token-based authorization header value.
     * 
     * @param token the authentication token
     * @return formatted authorization header value
     * @throws IllegalArgumentException if token is null or empty
     */
    public static String buildTokenAuthHeader(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        return TOKEN_PREFIX + token.trim();
    }
    
    /**
     * Creates a bearer token authorization header value.
     * 
     * @param token the bearer token
     * @return formatted authorization header value
     * @throws IllegalArgumentException if token is null or empty
     */
    public static String buildBearerAuthHeader(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        return BEARER_PREFIX + token.trim();
    }
    
    // ==================== VALIDATION HELPERS ==================== //
    
    /**
     * Validates that a project ID is non-null and non-empty.
     * 
     * @param projectId the project ID to validate
     * @throws IllegalArgumentException if the project ID is invalid
     */
    public static void validateProjectId(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
    }
    
    /**
     * Validates that a base URL is properly formatted.
     * 
     * @param baseUrl the base URL to validate
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static void validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }
        
        try {
            new URI(baseUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid base URL format: " + baseUrl, e);
        }
    }
} 