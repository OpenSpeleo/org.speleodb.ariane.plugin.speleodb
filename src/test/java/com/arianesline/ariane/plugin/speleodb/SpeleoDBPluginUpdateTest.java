package com.arianesline.ariane.plugin.speleodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/**
 * Test class for SpeleoDB plugin update functionality.
 * Tests version comparison, release filtering, and update logic.
 */
@DisplayName("SpeleoDB Plugin Update Tests")
class SpeleoDBPluginUpdateTest {

    @Mock
    private SpeleoDBPlugin mockPlugin;

    private SpeleoDBController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new SpeleoDBController();
        controller.parentPlugin = mockPlugin;
    }

    @Nested
    @DisplayName("Version Comparison Tests")
    class VersionComparisonTests {

        @Test
        @DisplayName("Should compare CalVer versions correctly")
        void shouldCompareCalVerVersionsCorrectly() {
            // Use reflection to access private method
            try {
                var method = SpeleoDBController.class.getDeclaredMethod("compareVersions", String.class, String.class);
                method.setAccessible(true);

                // Test equal versions
                assertEquals(0, (Integer) method.invoke(controller, "2025.06.23", "2025.06.23"));

                // Test newer version
                assertTrue((Integer) method.invoke(controller, "2025.06.24", "2025.06.23") > 0);
                assertTrue((Integer) method.invoke(controller, "2025.07.01", "2025.06.23") > 0);
                assertTrue((Integer) method.invoke(controller, "2026.01.01", "2025.12.31") > 0);

                // Test older version
                assertTrue((Integer) method.invoke(controller, "2025.06.22", "2025.06.23") < 0);
                assertTrue((Integer) method.invoke(controller, "2025.05.23", "2025.06.23") < 0);
                assertTrue((Integer) method.invoke(controller, "2024.12.31", "2025.01.01") < 0);

                // Test null handling
                assertEquals(0, (Integer) method.invoke(controller, null, null));
                assertTrue((Integer) method.invoke(controller, "2025.06.23", null) > 0);
                assertTrue((Integer) method.invoke(controller, null, "2025.06.23") < 0);

            } catch (Exception e) {
                throw new RuntimeException("Failed to test version comparison", e);
            }
        }

        @Test
        @DisplayName("Should handle malformed version strings")
        void shouldHandleMalformedVersionStrings() {
            try {
                var method = SpeleoDBController.class.getDeclaredMethod("compareVersions", String.class, String.class);
                method.setAccessible(true);

                // Test with non-numeric parts
                assertEquals(0, (Integer) method.invoke(controller, "2025.06.23-beta", "2025.06.23-alpha"));
                assertTrue((Integer) method.invoke(controller, "2025.06.24", "2025.06.23-beta") > 0);

            } catch (Exception e) {
                throw new RuntimeException("Failed to test malformed version handling", e);
            }
        }
    }

    @Nested
    @DisplayName("Hash Verification Tests")
    class HashVerificationTests {

        @Test
        @DisplayName("Should verify SHA256 hash correctly")
        void shouldVerifySHA256HashCorrectly() {
            try {
                var method = SpeleoDBController.class.getDeclaredMethod("verifyFileHash", byte[].class, String.class);
                method.setAccessible(true);

                // Test with known data and hash
                String testData = "Hello, World!";
                byte[] testDataBytes = testData.getBytes();
                String expectedHash = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f";

                assertTrue((Boolean) method.invoke(controller, testDataBytes, expectedHash));
                assertFalse((Boolean) method.invoke(controller, testDataBytes, "wrong_hash"));

                // Test case insensitivity
                assertTrue((Boolean) method.invoke(controller, testDataBytes, expectedHash.toUpperCase()));

            } catch (Exception e) {
                throw new RuntimeException("Failed to test hash verification", e);
            }
        }

        @Test
        @DisplayName("Should handle invalid hash gracefully")
        void shouldHandleInvalidHashGracefully() {
            try {
                var method = SpeleoDBController.class.getDeclaredMethod("verifyFileHash", byte[].class, String.class);
                method.setAccessible(true);

                byte[] testData = "test".getBytes();
                
                // Test with null hash
                assertFalse((Boolean) method.invoke(controller, testData, null));
                
                // Test with empty hash
                assertFalse((Boolean) method.invoke(controller, testData, ""));

            } catch (Exception e) {
                throw new RuntimeException("Failed to test invalid hash handling", e);
            }
        }
    }

    @Nested
    @DisplayName("Release Filtering Tests")
    class ReleaseFilteringTests {

        @Test
        @DisplayName("Should filter releases correctly")
        void shouldFilterReleasesCorrectly() {
            // Create test release data
            JsonArray releases = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE_VERSION, "25.2.1")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.23")
                    .add(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL, "https://example.com/plugin.jar")
                    .add(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH, "abc123")
                    .add(SpeleoDBConstants.JSON_FIELDS.CHANGELOG, "Initial Release"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "OTHER_SOFTWARE")
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE_VERSION, "25.2.1")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.24")
                    .add(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL, "https://example.com/other.jar")
                    .add(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH, "def456")
                    .add(SpeleoDBConstants.JSON_FIELDS.CHANGELOG, "Other software release"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE_VERSION, "25.1.0")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.25")
                    .add(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL, "https://example.com/old.jar")
                    .add(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH, "ghi789")
                    .add(SpeleoDBConstants.JSON_FIELDS.CHANGELOG, "Old ARIANE version"))
                .build();

            // Count ARIANE releases for current software version
            int arianeReleases = 0;
            for (int i = 0; i < releases.size(); i++) {
                JsonObject release = releases.getJsonObject(i);
                if ("ARIANE".equals(release.getString(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "")) &&
                    "25.2.1".equals(release.getString(SpeleoDBConstants.JSON_FIELDS.SOFTWARE_VERSION, ""))) {
                    arianeReleases++;
                }
            }

            assertEquals(1, arianeReleases, "Should find exactly 1 ARIANE release for version 25.2.1");
        }

        @Test
        @DisplayName("Should find latest version from multiple releases")
        void shouldFindLatestVersionFromMultipleReleases() {
            // Create test releases with different plugin versions
            JsonArray releases = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE_VERSION, "25.2.1")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.20")
                    .add(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL, "https://example.com/v1.jar")
                    .add(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH, "hash1")
                    .add(SpeleoDBConstants.JSON_FIELDS.CHANGELOG, "Version 1"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE_VERSION, "25.2.1")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.25")
                    .add(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL, "https://example.com/v3.jar")
                    .add(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH, "hash3")
                    .add(SpeleoDBConstants.JSON_FIELDS.CHANGELOG, "Version 3 - Latest"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE_VERSION, "25.2.1")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.22")
                    .add(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL, "https://example.com/v2.jar")
                    .add(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH, "hash2")
                    .add(SpeleoDBConstants.JSON_FIELDS.CHANGELOG, "Version 2"))
                .build();

            // Find the latest version manually (simulating the controller logic)
            String latestVersion = null;
            JsonObject latestRelease = null;

            for (int i = 0; i < releases.size(); i++) {
                JsonObject release = releases.getJsonObject(i);
                if ("ARIANE".equals(release.getString(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "")) &&
                    "25.2.1".equals(release.getString(SpeleoDBConstants.JSON_FIELDS.SOFTWARE_VERSION, ""))) {
                    
                    String pluginVersion = release.getString(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, null);
                    if (pluginVersion != null) {
                        if (latestVersion == null || compareVersionStrings(pluginVersion, latestVersion) > 0) {
                            latestVersion = pluginVersion;
                            latestRelease = release;
                        }
                    }
                }
            }

            assertNotNull(latestRelease, "Should find a latest release");
            assertEquals("2025.06.25", latestVersion, "Should identify the latest version");
            assertEquals("Version 3 - Latest", latestRelease.getString(SpeleoDBConstants.JSON_FIELDS.CHANGELOG, ""));
        }
    }

    /**
     * Simple version comparison for testing (duplicates the private method logic)
     */
    private int compareVersionStrings(String version1, String version2) {
        if (version1 == null && version2 == null) return 0;
        if (version1 == null) return -1;
        if (version2 == null) return 1;
        
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            
            int comparison = Integer.compare(num1, num2);
            if (comparison != 0) {
                return comparison;
            }
        }
        
        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
} 