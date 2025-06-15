package com.arianesline.ariane.plugin.speleodb;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/**
 * Standardized result wrapper for SpeleoDB service operations.
 * Provides consistent error handling and response management across all service methods.
 * 
 * @param <T> the type of data returned on success
 */
public class ServiceResult<T> {
    private final T data;
    private final boolean success;
    private final String errorMessage;
    private final int statusCode;
    private final ServiceErrorType errorType;
    
    /**
     * Enumeration of possible error types for better error categorization.
     */
    public enum ServiceErrorType {
        AUTHENTICATION_ERROR,
        NETWORK_ERROR,
        SERVER_ERROR,
        VALIDATION_ERROR,
        NOT_FOUND_ERROR,
        PERMISSION_ERROR,
        UNKNOWN_ERROR
    }
    
    private ServiceResult(T data, boolean success, String errorMessage, int statusCode, ServiceErrorType errorType) {
        this.data = data;
        this.success = success;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
        this.errorType = errorType;
    }
    
    // Factory methods for success cases
    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(data, true, null, 200, null);
    }
    
    public static <T> ServiceResult<T> success(T data, int statusCode) {
        return new ServiceResult<>(data, true, null, statusCode, null);
    }
    
    // Factory methods for failure cases
    public static <T> ServiceResult<T> failure(String message, int statusCode) {
        return failure(message, statusCode, categorizeError(statusCode));
    }
    
    public static <T> ServiceResult<T> failure(String message, int statusCode, ServiceErrorType errorType) {
        return new ServiceResult<>(null, false, message, statusCode, errorType);
    }
    
    public static <T> ServiceResult<T> authenticationFailure(String message) {
        return new ServiceResult<>(null, false, message, 401, ServiceErrorType.AUTHENTICATION_ERROR);
    }
    
    public static <T> ServiceResult<T> networkFailure(String message) {
        return new ServiceResult<>(null, false, message, 0, ServiceErrorType.NETWORK_ERROR);
    }
    
    public static <T> ServiceResult<T> validationFailure(String message) {
        return new ServiceResult<>(null, false, message, 400, ServiceErrorType.VALIDATION_ERROR);
    }
    
    /**
     * Categorizes error types based on HTTP status codes.
     */
    private static ServiceErrorType categorizeError(int statusCode) {
        if (statusCode >= 400 && statusCode < 500) {
            switch (statusCode) {
                case 401:
                case 403:
                    return ServiceErrorType.AUTHENTICATION_ERROR;
                case 404:
                    return ServiceErrorType.NOT_FOUND_ERROR;
                case 422:
                    return ServiceErrorType.VALIDATION_ERROR;
                default:
                    return ServiceErrorType.VALIDATION_ERROR;
            }
        } else if (statusCode >= 500) {
            return ServiceErrorType.SERVER_ERROR;
        } else if (statusCode == 0) {
            return ServiceErrorType.NETWORK_ERROR;
        }
        return ServiceErrorType.UNKNOWN_ERROR;
    }
    
    // Getters
    public T getData() { return data; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public int getStatusCode() { return statusCode; }
    public ServiceErrorType getErrorType() { return errorType; }
    
    /**
     * Gets data or throws an exception if the operation failed.
     * 
     * @return the data if successful
     * @throws ServiceException if the operation failed
     */
    public T getDataOrThrow() throws ServiceException {
        if (!success) {
            throw new ServiceException(errorMessage, statusCode, errorType);
        }
        return data;
    }
    
    /**
     * Returns data if successful, or a default value if failed.
     * 
     * @param defaultValue the value to return if operation failed
     * @return the data or default value
     */
    public T getDataOrDefault(T defaultValue) {
        return success ? data : defaultValue;
    }
    
    /**
     * Checks if the error is retryable based on the error type.
     * 
     * @return true if the operation can be retried
     */
    public boolean isRetryable() {
        return errorType == ServiceErrorType.NETWORK_ERROR || 
               errorType == ServiceErrorType.SERVER_ERROR ||
               (statusCode >= 500 && statusCode < 600);
    }
    
    /**
     * Gets a user-friendly error message based on the error type.
     * 
     * @return a user-friendly error description
     */
    public String getUserFriendlyMessage() {
        if (success) {
            return "Operation completed successfully";
        }
        
        switch (errorType) {
            case AUTHENTICATION_ERROR:
                return "Authentication failed. Please check your credentials and try again.";
            case NETWORK_ERROR:
                return "Network connection failed. Please check your internet connection.";
            case SERVER_ERROR:
                return "Server error occurred. Please try again later.";
            case VALIDATION_ERROR:
                return "Invalid data provided. Please check your input.";
            case NOT_FOUND_ERROR:
                return "The requested resource was not found.";
            case PERMISSION_ERROR:
                return "You don't have permission to perform this operation.";
            default:
                return errorMessage != null ? errorMessage : "An unknown error occurred.";
        }
    }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("ServiceResult{success=true, data=%s, statusCode=%d}", data, statusCode);
        } else {
            return String.format("ServiceResult{success=false, error='%s', statusCode=%d, errorType=%s}", 
                               errorMessage, statusCode, errorType);
        }
    }
    
    /**
     * Custom exception for service operations.
     */
    public static class ServiceException extends Exception {
        private static final long serialVersionUID = 1L;
        private final int statusCode;
        private final ServiceErrorType errorType;
        
        public ServiceException(String message, int statusCode, ServiceErrorType errorType) {
            super(message);
            this.statusCode = statusCode;
            this.errorType = errorType;
        }
        
        public int getStatusCode() { return statusCode; }
        public ServiceErrorType getErrorType() { return errorType; }
    }
    
    // Convenience type aliases for common return types
    public static class JsonObjectResult extends ServiceResult<JsonObject> {
        private JsonObjectResult(JsonObject data, boolean success, String errorMessage, int statusCode, ServiceErrorType errorType) {
            super(data, success, errorMessage, statusCode, errorType);
        }
    }
    
    public static class JsonArrayResult extends ServiceResult<JsonArray> {
        private JsonArrayResult(JsonArray data, boolean success, String errorMessage, int statusCode, ServiceErrorType errorType) {
            super(data, success, errorMessage, statusCode, errorType);
        }
    }
    
    public static class BooleanResult extends ServiceResult<Boolean> {
        private BooleanResult(Boolean data, boolean success, String errorMessage, int statusCode, ServiceErrorType errorType) {
            super(data, success, errorMessage, statusCode, errorType);
        }
    }
} 