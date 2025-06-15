package com.arianesline.ariane.plugin.speleodb;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for handling all authentication-related operations.
 * Extracted from SpeleoDBController to improve separation of concerns.
 */
public class AuthenticationService {
    
    private final SpeleoDBService speleoDBService;
    private final ExecutorService executorService;
    private final AuthenticationCallback callback;
    
    /**
     * Callback interface for authentication events.
     */
    public interface AuthenticationCallback {
        void onAuthenticationStarted();
        void onAuthenticationSuccess();
        void onAuthenticationFailed(String message);
        void onDisconnected();
        void logMessage(String message);
    }
    
    /**
     * Request object for authentication operations.
     */
    public static class AuthenticationRequest {
        private final String email;
        private final String password;
        private final String oAuthToken;
        private final String instance;
        
        public AuthenticationRequest(String email, String password, String oAuthToken, String instance) {
            this.email = email;
            this.password = password;
            this.oAuthToken = oAuthToken;
            this.instance = instance;
        }
        
        public String getEmail() { return email; }
        public String getPassword() { return password; }
        public String getOAuthToken() { return oAuthToken; }
        public String getInstance() { return instance; }
    }
    
    /**
     * Result object for authentication operations.
     */
    public static class AuthenticationResult {
        private final boolean success;
        private final String message;
        private final String instance;
        
        private AuthenticationResult(boolean success, String message, String instance) {
            this.success = success;
            this.message = message;
            this.instance = instance;
        }
        
        public static AuthenticationResult success(String instance) {
            return new AuthenticationResult(true, "Connected successfully", instance);
        }
        
        public static AuthenticationResult failure(String message) {
            return new AuthenticationResult(false, message, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getInstance() { return instance; }
    }
    
    public AuthenticationService(SpeleoDBService speleoDBService, ExecutorService executorService, AuthenticationCallback callback) {
        this.speleoDBService = speleoDBService;
        this.executorService = executorService;
        this.callback = callback;
    }
    
    /**
     * Performs authentication asynchronously.
     * 
     * @param request the authentication request containing credentials and instance
     * @return CompletableFuture that completes with authentication result
     */
    public CompletableFuture<AuthenticationResult> authenticateAsync(AuthenticationRequest request) {
        callback.onAuthenticationStarted();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String instance = request.getInstance();
                if (instance == null || instance.trim().isEmpty()) {
                    instance = "www.speleoDB.org"; // DEFAULT_INSTANCE
                }
                
                callback.logMessage("Connecting to " + instance);
                
                speleoDBService.authenticate(
                    request.getEmail(),
                    request.getPassword(),
                    request.getOAuthToken(),
                    instance
                );
                
                callback.logMessage("Connected successfully.");
                return AuthenticationResult.success(instance);
                
            } catch (Exception e) {
                String errorMessage = "Connection failed: " + e.getMessage();
                callback.logMessage(errorMessage);
                return AuthenticationResult.failure(errorMessage);
            }
        }, executorService);
    }
    
    /**
     * Synchronous authentication method for backward compatibility.
     * 
     * @param request the authentication request
     * @return the authentication result
     */
    public AuthenticationResult authenticate(AuthenticationRequest request) {
        try {
            return authenticateAsync(request).get();
        } catch (Exception e) {
            return AuthenticationResult.failure("Authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * Disconnects from the current SpeleoDB instance.
     * 
     * @return CompletableFuture that completes when disconnection is done
     */
    public CompletableFuture<Void> disconnectAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (speleoDBService.isAuthenticated()) {
                    String instanceUrl = speleoDBService.getSDBInstance();
                    speleoDBService.logout();
                    callback.logMessage("Disconnected from " + instanceUrl);
                }
                callback.onDisconnected();
            } catch (Exception e) {
                callback.logMessage("Error during disconnection: " + e.getMessage());
            }
        }, executorService);
    }
    
    /**
     * Synchronous disconnect method for backward compatibility.
     */
    public void disconnect() {
        try {
            disconnectAsync().get();
        } catch (Exception e) {
            callback.logMessage("Error during disconnection: " + e.getMessage());
        }
    }
    
    /**
     * Checks if currently authenticated.
     * 
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return speleoDBService.isAuthenticated();
    }
    
    /**
     * Gets the current instance URL if authenticated.
     * 
     * @return the instance URL or null if not authenticated
     */
    public String getCurrentInstance() {
        try {
            return speleoDBService.isAuthenticated() ? speleoDBService.getSDBInstance() : null;
        } catch (IllegalStateException e) {
            return null;
        }
    }
} 