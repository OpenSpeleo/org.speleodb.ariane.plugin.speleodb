package com.arianesline.ariane.plugin.speleodb;

import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

/**
 * Utility class for string formatting and building operations.
 * Centralizes string manipulation to improve maintainability and reduce manual concatenation.
 */
public final class StringFormatter {
    
    // Prevent instantiation
    private StringFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // ==================== REGEX PATTERNS ==================== //
    
    /** Pattern for detecting local network addresses */
    private static final Pattern LOCAL_NETWORK_PATTERN = Pattern.compile(
        "(^localhost)|(^127\\.)|(^10\\.)|(^172\\.(1[6-9]|2[0-9]|3[0-1])\\.)|(^192\\.168\\.)"
    );
    
    /** Pattern for token extraction from JSON responses */
    private static final String TOKEN_SEARCH_PATTERN = "\"token\":\"";
    
    // ==================== JSON BUILDING ==================== //
    
    /**
     * Creates a JSON object for project creation requests.
     * 
     * @param name the project name
     * @param description the project description  
     * @param countryCode the country code
     * @param latitude the latitude (optional)
     * @param longitude the longitude (optional)
     * @return JSON string for the project creation request
     */
    public static String buildProjectCreationJson(String name, String description, 
                                                 String countryCode, String latitude, String longitude) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
            .add("name", escapeJson(name))
            .add("description", escapeJson(description))
            .add("country", countryCode);
        
        // Add optional coordinates if provided
        if (latitude != null && !latitude.trim().isEmpty()) {
            builder.add("latitude", latitude.trim());
        }
        if (longitude != null && !longitude.trim().isEmpty()) {
            builder.add("longitude", longitude.trim());
        }
        
        return builder.build().toString();
    }
    
    /**
     * Creates a JSON object for authentication requests.
     * 
     * @param email the user email
     * @param password the user password
     * @return JSON string for the authentication request
     */
    public static String buildAuthenticationJson(String email, String password) {
        return Json.createObjectBuilder()
            .add("email", escapeJson(email))
            .add("password", escapeJson(password))
            .build()
            .toString();
    }
    
    /**
     * Escapes special characters in JSON strings.
     * 
     * @param input the input string
     * @return escaped string safe for JSON
     */
    public static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    
    // ==================== DIALOG MESSAGE FORMATTING ==================== //
    
    /**
     * Formats a project switch confirmation dialog message.
     * 
     * @param currentProjectName the name of the currently locked project
     * @param newProjectName the name of the project to switch to
     * @return formatted dialog message
     */
    public static String formatProjectSwitchMessage(String currentProjectName, String newProjectName) {
        return String.format(
            "You currently have an active lock on project \"%s\".\n\n" +
            "To switch to project \"%s\", you need to release your current lock.\n\n" +
            "Do you want to release the lock and switch projects?\n\n" +
            "• Yes: Release lock and switch to \"%s\"\n" +
            "• No: Keep current lock and stay on \"%s\"",
            currentProjectName, newProjectName, newProjectName, currentProjectName
        );
    }
    
    /**
     * Formats an unlock confirmation dialog message.
     * 
     * @param projectName the name of the project to unlock
     * @return formatted dialog message
     */
    public static String formatUnlockMessage(String projectName) {
        return String.format(
            "Are you sure you want to unlock project \"%s\"?\n\n" +
            "This will release your write lock and allow other users to edit the project.\n\n" +
            "• Yes: Unlock project (other users can edit)\n" +
            "• No: Keep project locked",
            projectName
        );
    }
    
    /**
     * Formats a release lock confirmation dialog message.
     * 
     * @param projectName the name of the project to release
     * @return formatted dialog message
     */
    public static String formatReleaseLockMessage(String projectName) {
        return String.format(
            "Are you sure you want to release the lock on project \"%s\"?\n\n" +
            "This will allow other users to edit the project.",
            projectName
        );
    }
    
    /**
     * Formats a project close confirmation dialog message.
     * 
     * @param projectName the name of the project being closed
     * @return formatted dialog message
     */
    public static String formatProjectCloseMessage(String projectName) {
        return String.format(
            "You have an active lock on project \"%s\".\n\n" +
            "Do you want to release the lock before closing the application?\n\n" +
            "• Yes: Release lock (other users can edit)\n" +
            "• No: Keep lock (will be released when connection times out)",
            projectName
        );
    }
    
    // ==================== SETTINGS FORMATTING ==================== //
    
    /**
     * Formats settings information for display.
     * 
     * @param isAuthenticated whether the user is authenticated
     * @param instanceUrl the current instance URL (optional)
     * @param dataDirectory the data directory path
     * @return formatted settings information
     */
    public static String formatSettingsInfo(boolean isAuthenticated, String instanceUrl, String dataDirectory) {
        StringBuilder info = new StringBuilder("Current Settings:\n\n");
        
        if (isAuthenticated) {
            info.append("Status: Connected\n");
            info.append("Instance: ").append(instanceUrl != null ? instanceUrl : "Unknown").append("\n");
        } else {
            info.append("Status: Disconnected\n");
            info.append("Instance: Not connected\n");
        }
        
        info.append("\nData Directory: ").append(dataDirectory).append("\n");
        info.append("\n• Use the main interface to configure connection settings");
        info.append("\n• Preferences are automatically saved when you connect");
        info.append("\n• Project files are stored in your home directory");
        
        return info.toString();
    }
    
    // ==================== NETWORK UTILITIES ==================== //
    
    /**
     * Checks if a URL represents a local network address.
     * 
     * @param url the URL to check
     * @return true if the URL is a local network address
     */
    public static boolean isLocalNetworkAddress(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return LOCAL_NETWORK_PATTERN.matcher(url.trim().toLowerCase()).find();
    }
    
    /**
     * Extracts authentication token from JSON response body.
     * 
     * @param responseBody the JSON response body
     * @return the extracted token, or null if not found
     */
    public static String extractAuthToken(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }
        
        int tokenStart = responseBody.indexOf(TOKEN_SEARCH_PATTERN);
        if (tokenStart == -1) {
            return null;
        }
        
        tokenStart += TOKEN_SEARCH_PATTERN.length();
        int tokenEnd = responseBody.indexOf("\"", tokenStart);
        
        if (tokenEnd == -1) {
            return null;
        }
        
        return responseBody.substring(tokenStart, tokenEnd);
    }
    
    // ==================== ERROR MESSAGE FORMATTING ==================== //
    
    /**
     * Formats error messages with consistent structure.
     * 
     * @param operation the operation that failed
     * @param statusCode the HTTP status code (optional)
     * @param details the error details (optional)
     * @return formatted error message
     */
    public static String formatErrorMessage(String operation, Integer statusCode, String details) {
        StringBuilder message = new StringBuilder(operation).append(" failed");
        
        if (statusCode != null && statusCode > 0) {
            message.append(" with status code: ").append(statusCode);
        }
        
        if (details != null && !details.trim().isEmpty()) {
            message.append(" - ").append(details.trim());
        }
        
        return message.toString();
    }
    
    /**
     * Truncates long strings for display purposes.
     * 
     * @param input the input string
     * @param maxLength the maximum length
     * @return truncated string with ellipsis if needed
     */
    public static String truncateForDisplay(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        
        if (input.length() <= maxLength) {
            return input;
        }
        
        return input.substring(0, maxLength) + "...";
    }
} 