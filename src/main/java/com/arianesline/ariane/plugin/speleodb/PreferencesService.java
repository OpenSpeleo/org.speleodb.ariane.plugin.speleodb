package com.arianesline.ariane.plugin.speleodb;

import java.util.prefs.Preferences;

/**
 * Service responsible for handling user preferences storage and retrieval.
 * Extracted from SpeleoDBController to improve separation of concerns.
 */
public class PreferencesService {
    
    // Constants for Preferences keys and default values
    private static final String PREF_EMAIL = "SDB_EMAIL";
    private static final String PREF_PASSWORD = "SDB_PASSWORD";
    private static final String PREF_OAUTH_TOKEN = "SDB_OAUTH_TOKEN";
    private static final String PREF_INSTANCE = "SDB_INSTANCE";
    private static final String PREF_SAVE_CREDS = "SDB_SAVECREDS";
    private static final String DEFAULT_INSTANCE = "www.speleoDB.org";
    
    private final Preferences preferences;
    
    /**
     * Data class to hold user preferences.
     */
    public static class UserPreferences {
        private final String email;
        private final String password;
        private final String oAuthToken;
        private final String instance;
        private final boolean saveCredentials;
        
        public UserPreferences(String email, String password, String oAuthToken, 
                             String instance, boolean saveCredentials) {
            this.email = email;
            this.password = password;
            this.oAuthToken = oAuthToken;
            this.instance = instance;
            this.saveCredentials = saveCredentials;
        }
        
        public String getEmail() { return email; }
        public String getPassword() { return password; }
        public String getOAuthToken() { return oAuthToken; }
        public String getInstance() { return instance; }
        public boolean isSaveCredentials() { return saveCredentials; }
        
        /**
         * Builder for UserPreferences to make creation more flexible.
         */
        public static class Builder {
            private String email = "";
            private String password = "";
            private String oAuthToken = "";
            private String instance = DEFAULT_INSTANCE;
            private boolean saveCredentials = false;
            
            public Builder withEmail(String email) {
                this.email = email != null ? email : "";
                return this;
            }
            
            public Builder withPassword(String password) {
                this.password = password != null ? password : "";
                return this;
            }
            
            public Builder withOAuthToken(String oAuthToken) {
                this.oAuthToken = oAuthToken != null ? oAuthToken : "";
                return this;
            }
            
            public Builder withInstance(String instance) {
                this.instance = (instance != null && !instance.trim().isEmpty()) ? instance : DEFAULT_INSTANCE;
                return this;
            }
            
            public Builder withSaveCredentials(boolean saveCredentials) {
                this.saveCredentials = saveCredentials;
                return this;
            }
            
            public UserPreferences build() {
                return new UserPreferences(email, password, oAuthToken, instance, saveCredentials);
            }
        }
    }
    
    public PreferencesService() {
        this.preferences = Preferences.userNodeForPackage(SpeleoDBController.class);
    }
    
    /**
     * Constructor for testing with custom preferences node.
     * 
     * @param preferences custom preferences node
     */
    PreferencesService(Preferences preferences) {
        this.preferences = preferences;
    }
    
    /**
     * Loads user preferences from the system.
     * 
     * @return UserPreferences object containing all saved preferences
     */
    public UserPreferences loadPreferences() {
        boolean saveCredentials = preferences.getBoolean(PREF_SAVE_CREDS, false);
        
        return new UserPreferences.Builder()
            .withEmail(preferences.get(PREF_EMAIL, ""))
            .withPassword(saveCredentials ? preferences.get(PREF_PASSWORD, "") : "")
            .withOAuthToken(saveCredentials ? preferences.get(PREF_OAUTH_TOKEN, "") : "")
            .withInstance(preferences.get(PREF_INSTANCE, DEFAULT_INSTANCE))
            .withSaveCredentials(saveCredentials)
            .build();
    }
    
    /**
     * Saves user preferences to the system.
     * 
     * @param userPrefs the preferences to save
     */
    public void savePreferences(UserPreferences userPrefs) {
        if (userPrefs == null) {
            throw new IllegalArgumentException("UserPreferences cannot be null");
        }
        
        // Always save email and instance
        preferences.put(PREF_EMAIL, userPrefs.getEmail());
        preferences.put(PREF_INSTANCE, userPrefs.getInstance());
        preferences.putBoolean(PREF_SAVE_CREDS, userPrefs.isSaveCredentials());
        
        if (userPrefs.isSaveCredentials()) {
            // Save credentials if user opted in
            preferences.put(PREF_PASSWORD, userPrefs.getPassword());
            preferences.put(PREF_OAUTH_TOKEN, userPrefs.getOAuthToken());
        } else {
            // Remove credentials if user opted out
            preferences.remove(PREF_PASSWORD);
            preferences.remove(PREF_OAUTH_TOKEN);
        }
    }
    
    /**
     * Clears all saved preferences.
     */
    public void clearAllPreferences() {
        try {
            preferences.clear();
        } catch (Exception e) {
            // If clear fails, remove individual keys
            preferences.remove(PREF_EMAIL);
            preferences.remove(PREF_PASSWORD);
            preferences.remove(PREF_OAUTH_TOKEN);
            preferences.remove(PREF_INSTANCE);
            preferences.remove(PREF_SAVE_CREDS);
        }
    }
    
    /**
     * Clears only credential-related preferences (password and OAuth token).
     */
    public void clearCredentials() {
        preferences.remove(PREF_PASSWORD);
        preferences.remove(PREF_OAUTH_TOKEN);
        preferences.putBoolean(PREF_SAVE_CREDS, false);
    }
    
    /**
     * Gets the default instance URL.
     * 
     * @return the default instance URL
     */
    public static String getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }
    
    /**
     * Checks if credentials are currently saved.
     * 
     * @return true if save credentials is enabled and credentials exist
     */
    public boolean hasStoredCredentials() {
        boolean saveCredentials = preferences.getBoolean(PREF_SAVE_CREDS, false);
        if (!saveCredentials) {
            return false;
        }
        
        String password = preferences.get(PREF_PASSWORD, "");
        String oAuthToken = preferences.get(PREF_OAUTH_TOKEN, "");
        
        return !password.isEmpty() || !oAuthToken.isEmpty();
    }
    
    /**
     * Validates that an instance URL is not empty.
     * 
     * @param instance the instance URL to validate
     * @return the validated instance URL or default if invalid
     */
    public static String validateInstance(String instance) {
        return (instance != null && !instance.trim().isEmpty()) ? instance.trim() : DEFAULT_INSTANCE;
    }
} 