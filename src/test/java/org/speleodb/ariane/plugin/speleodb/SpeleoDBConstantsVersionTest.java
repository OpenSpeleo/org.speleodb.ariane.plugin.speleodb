package org.speleodb.ariane.plugin.speleodb;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SpeleoDBConstants version loading functionality.
 * Verifies that ARIANE_VERSION is properly loaded from preferences.
 */
@DisplayName("SpeleoDB Constants Version Loading Tests")
class SpeleoDBConstantsVersionTest {

    @Test
    @DisplayName("Should have ARIANE_VERSION populated")
    void shouldHaveArianeVersionPopulated() {
        // Verify that ARIANE_VERSION is not null
        assertNotNull(SpeleoDBConstants.ARIANE_VERSION, 
            "ARIANE_VERSION should not be null");
        
        // Verify that ARIANE_VERSION is not empty
        assertFalse(SpeleoDBConstants.ARIANE_VERSION.trim().isEmpty(), 
            "ARIANE_VERSION should not be empty");
        
        // Log the version for verification
        System.out.println("ARIANE_VERSION loaded: " + SpeleoDBConstants.ARIANE_VERSION);
    }
    
    @Test
    @DisplayName("Should have valid version format")
    void shouldHaveValidVersionFormat() {
        String version = SpeleoDBConstants.ARIANE_VERSION;
        
        // Version should match the pattern XX.Y.Z where XX is two digits, Y and Z are one or more digits
        assertTrue(version.matches("\\d{2}\\.\\d+\\.\\d+"), 
            "ARIANE_VERSION should match version pattern (e.g., 25.2.1): " + version);
    }
    
    @Test
    @DisplayName("Should have ARIANE_SOFTWARE_NAME constant")
    void shouldHaveArianeSoftwareNameConstant() {
        assertEquals("ARIANE", SpeleoDBConstants.ARIANE_SOFTWARE_NAME, 
            "ARIANE_SOFTWARE_NAME should be 'ARIANE'");
    }
    
    @Test
    @DisplayName("Should be able to use ARIANE_VERSION in version comparison")
    void shouldBeAbleToUseInVersionComparison() {
        // Test that the version can be used in compareVersions method
        String currentVersion = SpeleoDBConstants.ARIANE_VERSION;
        
        // Should be able to compare with itself
        assertEquals(0, SpeleoDBController.compareVersions(currentVersion, currentVersion),
            "Version should be equal to itself");
        
        // Should be able to compare with other versions
        assertTrue(SpeleoDBController.compareVersions(currentVersion, "1.0.0") > 0,
            "Current version should be greater than 1.0.0");
    }
} 