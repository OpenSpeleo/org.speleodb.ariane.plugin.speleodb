package org.speleodb.ariane.plugin.speleodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests to verify that the preference isolation system works correctly.
 * This ensures that tests don't affect user's actual preferences.
 */
class SpeleoDBPreferenceIsolationTest {

    private SpeleoDBController controller;
    private String originalTestMode;

    @BeforeEach
    void setUp() {
        // Save original test mode setting
        originalTestMode = System.getProperty("speleodb.test.mode");
        
        // Create controller instance
        controller = new SpeleoDBController();
    }

    @AfterEach
    void tearDown() {
        // Restore original test mode setting
        if (originalTestMode != null) {
            System.setProperty("speleodb.test.mode", originalTestMode);
        } else {
            System.clearProperty("speleodb.test.mode");
        }
    }

    @Test
    void testPreferenceIsolationInTestMode() {
        // Ensure we're in test mode
        System.setProperty("speleodb.test.mode", "true");
        
        // Get preferences node
        Preferences testPrefs = controller.getPreferencesNode();
        
        // Verify we get a test-specific preferences node
        assertNotNull(testPrefs);
        
        // The test preferences should be isolated from production
        assertTrue(testPrefs.absolutePath().contains("test"), 
                  "Test preferences should use test-specific path, got: " + testPrefs.absolutePath());
    }

    @Test
    void testPreferenceIsolationInProductionMode() {
        // Ensure we're in production mode
        System.setProperty("speleodb.test.mode", "false");
        
        // Get preferences node
        Preferences prodPrefs = controller.getPreferencesNode();
        
        // Verify we get a production preferences node
        assertNotNull(prodPrefs);
        
        // The production preferences should use the normal package-based path
        assertFalse(prodPrefs.absolutePath().contains("test"), 
                   "Production preferences should not use test path, got: " + prodPrefs.absolutePath());
    }

    @Test
    void testPreferenceIsolationWithoutProperty() {
        // Clear the test mode property
        System.clearProperty("speleodb.test.mode");
        
        // Get preferences node
        Preferences defaultPrefs = controller.getPreferencesNode();
        
        // Verify we get a production preferences node by default
        assertNotNull(defaultPrefs);
        
        // Without the test mode property, should default to production
        assertFalse(defaultPrefs.absolutePath().contains("test"), 
                   "Default preferences should not use test path, got: " + defaultPrefs.absolutePath());
    }

    @Test
    void testPreferenceIsolationSeparation() {
        // Test that test and production preferences are truly separate
        System.setProperty("speleodb.test.mode", "true");
        Preferences testPrefs = controller.getPreferencesNode();
        
        System.setProperty("speleodb.test.mode", "false");
        Preferences prodPrefs = controller.getPreferencesNode();
        
        // They should be different preference nodes
        assertNotEquals(testPrefs.absolutePath(), prodPrefs.absolutePath(), 
                       "Test and production preferences should have different paths");
        
        // Test that data doesn't leak between them
        String testKey = "test.isolation.key";
        String testValue = "test.isolation.value";
        
        // Set value in test preferences
        System.setProperty("speleodb.test.mode", "true");
        testPrefs = controller.getPreferencesNode();
        testPrefs.put(testKey, testValue);
        
        // Check that production preferences don't see the test value
        System.setProperty("speleodb.test.mode", "false");
        prodPrefs = controller.getPreferencesNode();
        String prodValue = prodPrefs.get(testKey, "not.found");
        
        assertEquals("not.found", prodValue, 
                    "Production preferences should not see test values");
        
        // Clean up test preference
        System.setProperty("speleodb.test.mode", "true");
        testPrefs = controller.getPreferencesNode();
        testPrefs.remove(testKey);
    }
} 