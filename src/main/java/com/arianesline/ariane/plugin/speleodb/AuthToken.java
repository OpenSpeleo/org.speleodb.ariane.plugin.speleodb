package com.arianesline.ariane.plugin.speleodb;

import java.util.Objects;

/**
 * Value object representing an authentication token for SpeleoDB API.
 * Provides type safety and validation for authentication tokens.
 */
public final class AuthToken {
    
    private final String token;
    
    private AuthToken(String token) {
        this.token = Objects.requireNonNull(token, "Auth token cannot be null");
    }
    
    /**
     * Creates an AuthToken from a string value.
     * 
     * @param token the token string (must not be null or empty)
     * @return a new AuthToken instance
     * @throws IllegalArgumentException if the token is null or empty
     */
    public static AuthToken of(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Auth token cannot be empty");
        }
        return new AuthToken(token.trim());
    }
    
    /**
     * Creates an empty/invalid AuthToken.
     * 
     * @return an empty AuthToken instance
     */
    public static AuthToken empty() {
        return new AuthToken("");
    }
    
    /**
     * Gets the raw token value.
     * 
     * @return the token string
     */
    public String getValue() {
        return token;
    }
    
    /**
     * Checks if this token is valid (non-empty).
     * 
     * @return true if the token is valid, false otherwise
     */
    public boolean isValid() {
        return !token.isEmpty();
    }
    
    /**
     * Checks if this token is empty/invalid.
     * 
     * @return true if the token is empty, false otherwise
     */
    public boolean isEmpty() {
        return token.isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthToken authToken = (AuthToken) o;
        return Objects.equals(token, authToken.token);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
    
    @Override
    public String toString() {
        // Don't expose the actual token in toString for security
        return "AuthToken{" + (isValid() ? "***" : "empty") + "}";
    }
} 