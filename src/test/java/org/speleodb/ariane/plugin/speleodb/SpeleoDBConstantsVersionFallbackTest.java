package org.speleodb.ariane.plugin.speleodb;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SpeleoDBConstants version loading fallback behavior.
 * This test will typically run in environments where Ariane classes are not available,
 * testing the fallback mechanism.
 */
@DisplayName("SpeleoDB Constants Version Fallback Tests")
class SpeleoDBConstantsVersionFallbackTest {

    @Test
    @DisplayName("Should always have a valid ARIANE_VERSION even without Ariane classes")
    void shouldAlwaysHaveValidArianeVersion() {
        // This test verifies that even when Ariane classes are not available,
        // we still have a valid version (the fallback)
        
        String version = SpeleoDBConstants.ARIANE_VERSION;
        
        // Should never be null
        assertNotNull(version, "ARIANE_VERSION should never be null");
        
        // Should never be empty
        assertFalse(version.trim().isEmpty(), "ARIANE_VERSION should never be empty");
        
        // Should be a valid version format
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"), 
            "ARIANE_VERSION should be in valid format: " + version);
        
        // Log which version we got (either from preferences or fallback)
        System.out.println("ARIANE_VERSION in test environment: " + version);
        
        // In a test environment without Ariane, we expect the fallback version
        // But if Ariane is available, we might get a different version
        // Either way, the version should be valid
        assertTrue(version.equals("25.2.1") || version.matches("\\d{2}\\.\\d+\\.\\d+"),
            "Version should be either the fallback (25.2.1) or a valid Ariane version: " + version);
    }
    
    @Test
    @DisplayName("Should be able to use ARIANE_VERSION in API calls")
    void shouldBeAbleToUseVersionInApiCalls() {
        // This test verifies that the version works in real scenarios
        // like filtering plugin releases
        
        String version = SpeleoDBConstants.ARIANE_VERSION;
        
        // Simulate version bounds checking
        String minVersion = "25.0.0";
        String maxVersion = "26.0.0";
        
        // Current version should be within typical bounds
        assertTrue(SpeleoDBController.compareVersions(version, minVersion) >= 0,
            "Version should be >= " + minVersion);
        assertTrue(SpeleoDBController.compareVersions(version, maxVersion) <= 0,
            "Version should be <= " + maxVersion);
    }
    
    @Test
    @DisplayName("Should handle version in logging without errors")
    void shouldHandleVersionInLogging() {
        // This test ensures that logging with the version doesn't cause issues
        assertDoesNotThrow(() -> {
            String logMessage = "Testing with Ariane version: " + SpeleoDBConstants.ARIANE_VERSION;
            System.out.println(logMessage);
        }, "Should be able to use ARIANE_VERSION in logging without errors");
    }
} 