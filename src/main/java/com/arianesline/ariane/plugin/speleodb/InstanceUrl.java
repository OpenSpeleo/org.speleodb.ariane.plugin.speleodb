package com.arianesline.ariane.plugin.speleodb;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a SpeleoDB instance URL.
 * Provides type safety, validation, and proper protocol handling for instance URLs.
 */
public final class InstanceUrl {
    
    private final URI uri;
    private final String originalInput;
    
    private InstanceUrl(URI uri, String originalInput) {
        this.uri = Objects.requireNonNull(uri, "Instance URI cannot be null");
        this.originalInput = originalInput;
    }
    
    /**
     * Creates an InstanceUrl from a string value with automatic protocol detection.
     * 
     * @param url the instance URL string
     * @return a new InstanceUrl instance
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static InstanceUrl of(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Instance URL cannot be empty");
        }
        
        String cleanUrl = url.trim();
        String processedUrl = processUrlWithProtocol(cleanUrl);
        
        try {
            URI uri = new URI(processedUrl);
            return new InstanceUrl(uri, cleanUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + cleanUrl, e);
        }
    }
    
    /**
     * Creates an InstanceUrl for the default SpeleoDB instance.
     * 
     * @return InstanceUrl for www.speleoDB.org
     */
    public static InstanceUrl defaultInstance() {
        return of("www.speleoDB.org");
    }
    
    /**
     * Processes the URL to add appropriate protocol (http/https) based on the address.
     * Uses the same logic as the existing SpeleoDBService.setSDBInstance() method.
     */
    private static String processUrlWithProtocol(String instanceUrl) {
        // Regex to match localhost, private IP ranges, or loopback addresses
        String localPattern = "(^localhost)|(^127\\.)|(^10\\.)|(^172\\.(1[6-9]|2[0-9]|3[0-1])\\.)|(^192\\.168\\.)";
        
        // If URL already has a protocol, return as-is
        if (instanceUrl.startsWith("http://") || instanceUrl.startsWith("https://")) {
            return instanceUrl;
        }
        
        if (Pattern.compile(localPattern).matcher(instanceUrl).find()) {
            // For local addresses and IPs, use http://
            return "http://" + instanceUrl;
        } else {
            // For non-local addresses, use https://
            return "https://" + instanceUrl;
        }
    }
    
    /**
     * Gets the complete URI including protocol.
     * 
     * @return the URI object
     */
    public URI getUri() {
        return uri;
    }
    
    /**
     * Gets the full URL string including protocol.
     * 
     * @return the complete URL string
     */
    public String getUrl() {
        return uri.toString();
    }
    
    /**
     * Gets the host part of the URL (without protocol).
     * 
     * @return the host string
     */
    public String getHost() {
        return uri.getHost();
    }
    
    /**
     * Gets the original input string that was used to create this InstanceUrl.
     * 
     * @return the original input
     */
    public String getOriginalInput() {
        return originalInput;
    }
    
    /**
     * Checks if this instance URL represents a local development environment.
     * 
     * @return true if this is a local instance (localhost, private IPs), false otherwise
     */
    public boolean isLocal() {
        String host = getHost();
        if (host == null) return false;
        
        String localPattern = "(^localhost)|(^127\\.)|(^10\\.)|(^172\\.(1[6-9]|2[0-9]|3[0-1])\\.)|(^192\\.168\\.)";
        return Pattern.compile(localPattern).matcher(host).find();
    }
    
    /**
     * Checks if this instance URL uses HTTPS protocol.
     * 
     * @return true if using HTTPS, false otherwise
     */
    public boolean isSecure() {
        return "https".equals(uri.getScheme());
    }
    
    /**
     * Checks if this instance URL is empty/default.
     * 
     * @return true if this is the default instance, false otherwise
     */
    public boolean isEmpty() {
        return false; // InstanceUrl objects are never empty - they always have a valid URL
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstanceUrl that = (InstanceUrl) o;
        return Objects.equals(uri, that.uri);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }
    
    @Override
    public String toString() {
        return uri.toString();
    }
} 