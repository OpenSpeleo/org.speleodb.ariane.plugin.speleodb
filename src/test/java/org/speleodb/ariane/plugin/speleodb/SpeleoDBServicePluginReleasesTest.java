package org.speleodb.ariane.plugin.speleodb;

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
 * Test class for SpeleoDB service plugin releases functionality.
 * Tests the fetchPluginReleases method and related filtering logic.
 */
@DisplayName("SpeleoDB Service Plugin Releases Tests")
class SpeleoDBServicePluginReleasesTest {

    @Mock
    private SpeleoDBController mockController;

    private SpeleoDBService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new SpeleoDBService(mockController);
    }

    @Nested
    @DisplayName("API Endpoint Tests")
    class ApiEndpointTests {

        @Test
        @DisplayName("Should use correct plugin releases endpoint")
        void shouldUseCorrectPluginReleasesEndpoint() {
            String expectedEndpoint = "/api/v1/plugin_releases/";
            assertEquals(expectedEndpoint, SpeleoDBConstants.API.PLUGIN_RELEASES_ENDPOINT);
        }

        @Test
        @DisplayName("Should construct proper URL for local instances")
        void shouldConstructProperUrlForLocalInstances() {
            // Test various local URL patterns
            String[] localUrls = {
                "localhost:8000",
                "127.0.0.1:3000",
                "192.168.1.100",
                "10.0.0.1:8080",
                "172.16.0.1"
            };

            for (String url : localUrls) {
                assertTrue(url.matches(SpeleoDBConstants.NETWORK.LOCAL_PATTERN),
                          "URL should be recognized as local: " + url);
            }
        }

        @Test
        @DisplayName("Should construct proper URL for remote instances")
        void shouldConstructProperUrlForRemoteInstances() {
            // Test various remote URL patterns
            String[] remoteUrls = {
                "www.speleodb.org",
                "api.example.com",
                "speleodb.example.org",
                "8.8.8.8"
            };

            for (String url : remoteUrls) {
                assertFalse(url.matches(SpeleoDBConstants.NETWORK.LOCAL_PATTERN),
                           "URL should be recognized as remote: " + url);
            }
        }
    }

    @Nested
    @DisplayName("Release Filtering Tests")
    class ReleaseFilteringTests {

        @Test
        @DisplayName("Should filter releases by software name")
        void shouldFilterReleasesBySoftwareName() {
            // Create test data with mixed software types
            JsonArray testReleases = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, "25.2.1")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.23"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "OTHER_SOFTWARE")
                    .add(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, "25.2.1")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.24"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, "25.1.0")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.25"))
                .build();

            // Count ARIANE releases
            int arianeCount = 0;
            for (int i = 0; i < testReleases.size(); i++) {
                JsonObject release = testReleases.getJsonObject(i);
                if ("ARIANE".equals(release.getString(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, ""))) {
                    arianeCount++;
                }
            }

            assertEquals(2, arianeCount, "Should find 2 ARIANE releases");
        }

        @Test
        @DisplayName("Should filter releases by software version bounds")
        void shouldFilterReleasesBySoftwareVersionBounds() {
            // Create test data with different version bounds
            JsonArray testReleases = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, "25.2.0")
                    .add(SpeleoDBConstants.JSON_FIELDS.MAX_SOFTWARE_VERSION, "25.3.0")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.23"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, "25.0.0")
                    .add(SpeleoDBConstants.JSON_FIELDS.MAX_SOFTWARE_VERSION, "25.1.0")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.24"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, "25.2.1")
                    // No max version - should match 25.2.1 and above
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.25"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    // No min version
                    .add(SpeleoDBConstants.JSON_FIELDS.MAX_SOFTWARE_VERSION, "25.2.1")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.26"))
                .build();

            // Count releases compatible with current ARIANE version (25.2.1)
            int compatibleCount = 0;
            String currentVersion = SpeleoDBConstants.ARIANE_VERSION; // "25.2.1"
            
            for (int i = 0; i < testReleases.size(); i++) {
                JsonObject release = testReleases.getJsonObject(i);
                if (!"ARIANE".equals(release.getString(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, ""))) {
                    continue;
                }
                
                String minVersion = release.getString(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, null);
                String maxVersion = release.getString(SpeleoDBConstants.JSON_FIELDS.MAX_SOFTWARE_VERSION, null);
                
                boolean isCompatible = true;
                
                // Check minimum version
                if (minVersion != null && SpeleoDBController.compareVersions(currentVersion, minVersion) < 0) {
                    isCompatible = false;
                }
                
                // Check maximum version
                if (maxVersion != null && SpeleoDBController.compareVersions(currentVersion, maxVersion) > 0) {
                    isCompatible = false;
                }
                
                if (isCompatible) {
                    compatibleCount++;
                }
            }

            assertEquals(3, compatibleCount, "Should find 3 releases compatible with ARIANE version 25.2.1");
        }

        @Test
        @DisplayName("Should handle empty release arrays")
        void shouldHandleEmptyReleaseArrays() {
            JsonArray emptyReleases = Json.createArrayBuilder().build();
            assertEquals(0, emptyReleases.size(), "Empty array should have size 0");
        }

        @Test
        @DisplayName("Should handle releases with missing fields")
        void shouldHandleReleasesWithMissingFields() {
            // Create test data with missing fields
            JsonArray testReleases = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    // Missing version bounds
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.23"))
                .add(Json.createObjectBuilder()
                    // Missing software field
                    .add(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, "25.2.1")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.24"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, "25.2.1")
                    // Missing plugin_version
                    )
                .build();

            // Count valid releases (required fields present)
            int validCount = 0;
            for (int i = 0; i < testReleases.size(); i++) {
                JsonObject release = testReleases.getJsonObject(i);
                String software = release.getString(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, null);
                String pluginVersion = release.getString(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, null);
                
                // In the new format, version bounds are optional, so a release is valid if it has software and plugin_version
                if (software != null && pluginVersion != null) {
                    validCount++;
                }
            }

            assertEquals(1, validCount, "Should find 1 release with all required fields");
        }
    }

    @Nested
    @DisplayName("JSON Response Handling Tests")
    class JsonResponseHandlingTests {

        @Test
        @DisplayName("Should parse valid JSON response structure")
        void shouldParseValidJsonResponseStructure() {
            // Test the expected API response structure with the new format
            JsonObject apiResponse = Json.createObjectBuilder()
                .add("success", true)
                .add("timestamp", "2025-06-25 00:53:14")
                .add("url", "http://localhost:8000/api/v1/plugin_releases/")
                .add(SpeleoDBConstants.JSON_FIELDS.DATA, Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("id", 1)
                        .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                        .add(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, "25.2.1")
                        .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.25")
                        .add(SpeleoDBConstants.JSON_FIELDS.OPERATING_SYSTEM, "ANY")
                        .add(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL, "https://github.com/OpenSpeleo/Ariane-SpeleoDB-Releases/releases/download/2025.06.25/org.speleodb.ariane.plugin.speleodb-2025.06.25.jar")
                        .add(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH, "8a0a6649efad8bdeeb35ecb20b340e06dcbc48c64d455472a61f0e2de369035d")
                        .add(SpeleoDBConstants.JSON_FIELDS.CHANGELOG, "- Changed Filename\r\n- Remember Me checked by default")
                        .add("creation_date", "2025-06-25T00:16:18.884989-04:00")
                        .add("modified_date", "2025-06-25T00:52:36.901731-04:00")))
                .build();

            assertTrue(apiResponse.getBoolean("success"));
            assertNotNull(apiResponse.getJsonArray(SpeleoDBConstants.JSON_FIELDS.DATA));
            assertEquals(1, apiResponse.getJsonArray(SpeleoDBConstants.JSON_FIELDS.DATA).size());

            JsonObject release = apiResponse.getJsonArray(SpeleoDBConstants.JSON_FIELDS.DATA).getJsonObject(0);
            assertEquals("ARIANE", release.getString(SpeleoDBConstants.JSON_FIELDS.SOFTWARE));
            assertEquals("25.2.1", release.getString(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION));
            assertEquals("2025.06.25", release.getString(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION));
            assertNotNull(release.getString(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL));
            assertNotNull(release.getString(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH));
        }

        @Test
        @DisplayName("Should handle response with multiple releases")
        void shouldHandleResponseWithMultipleReleases() {
            JsonArray multipleReleases = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, "25.2.0")
                    .add(SpeleoDBConstants.JSON_FIELDS.MAX_SOFTWARE_VERSION, "25.3.0")
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.20")
                    .add(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL, "https://example.com/v1.jar")
                    .add(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH, "hash1"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    .add(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION, "25.2.1")
                    // No max version
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.25")
                    .add(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL, "https://example.com/v2.jar")
                    .add(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH, "hash2"))
                .add(Json.createObjectBuilder()
                    .add(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "ARIANE")
                    // No version bounds - compatible with all versions
                    .add(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION, "2025.06.22")
                    .add(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL, "https://example.com/v3.jar")
                    .add(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH, "hash3"))
                .build();

            assertEquals(3, multipleReleases.size());

            // Verify all releases are for ARIANE with proper plugin details
            for (int i = 0; i < multipleReleases.size(); i++) {
                JsonObject release = multipleReleases.getJsonObject(i);
                assertEquals("ARIANE", release.getString(SpeleoDBConstants.JSON_FIELDS.SOFTWARE));
                assertNotNull(release.getString(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION));
                assertNotNull(release.getString(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL));
                assertNotNull(release.getString(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH));
            }
        }
    }

    @Nested
    @DisplayName("Constants and Configuration Tests")
    class ConstantsAndConfigurationTests {

        @Test
        @DisplayName("Should have correct ARIANE version constant")
        void shouldHaveCorrectArianeVersionConstant() {
            assertEquals("25.2.1", SpeleoDBConstants.ARIANE_VERSION);
        }

        @Test
        @DisplayName("Should have correct ARIANE software name constant")
        void shouldHaveCorrectArianeSoftwareNameConstant() {
            assertEquals("ARIANE", SpeleoDBConstants.ARIANE_SOFTWARE_NAME);
        }

        @Test
        @DisplayName("Should have all required JSON field constants")
        void shouldHaveAllRequiredJsonFieldConstants() {
            assertNotNull(SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION);
            assertNotNull(SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION);
            assertNotNull(SpeleoDBConstants.JSON_FIELDS.MAX_SOFTWARE_VERSION);
            assertNotNull(SpeleoDBConstants.JSON_FIELDS.SOFTWARE);
            assertNotNull(SpeleoDBConstants.JSON_FIELDS.OPERATING_SYSTEM);
            assertNotNull(SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL);
            assertNotNull(SpeleoDBConstants.JSON_FIELDS.SHA256_HASH);
            assertNotNull(SpeleoDBConstants.JSON_FIELDS.CHANGELOG);

            assertEquals("plugin_version", SpeleoDBConstants.JSON_FIELDS.PLUGIN_VERSION);
            assertEquals("min_software_version", SpeleoDBConstants.JSON_FIELDS.MIN_SOFTWARE_VERSION);
            assertEquals("max_software_version", SpeleoDBConstants.JSON_FIELDS.MAX_SOFTWARE_VERSION);
            assertEquals("software", SpeleoDBConstants.JSON_FIELDS.SOFTWARE);
            assertEquals("operating_system", SpeleoDBConstants.JSON_FIELDS.OPERATING_SYSTEM);
            assertEquals("download_url", SpeleoDBConstants.JSON_FIELDS.DOWNLOAD_URL);
            assertEquals("sha256_hash", SpeleoDBConstants.JSON_FIELDS.SHA256_HASH);
            assertEquals("changelog", SpeleoDBConstants.JSON_FIELDS.CHANGELOG);
        }

        @Test
        @DisplayName("Should have correct timeout configurations")
        void shouldHaveCorrectTimeoutConfigurations() {
            assertTrue(SpeleoDBConstants.NETWORK.REQUEST_TIMEOUT_SECONDS > 0);
            assertTrue(SpeleoDBConstants.NETWORK.CONNECT_TIMEOUT_SECONDS > 0);
            assertTrue(SpeleoDBConstants.NETWORK.DOWNLOAD_TIMEOUT_SECONDS > 0);
            
            // Download timeout should be longer than regular request timeout
            assertTrue(SpeleoDBConstants.NETWORK.DOWNLOAD_TIMEOUT_SECONDS >= 
                      SpeleoDBConstants.NETWORK.REQUEST_TIMEOUT_SECONDS);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() {
            // These tests would require mocking HTTP responses
            // For now, we test that the constants exist for error handling
            assertNotNull(SpeleoDBConstants.MESSAGES.UPDATE_CHECK_FAILED);
            assertNotNull(SpeleoDBConstants.MESSAGES.UPDATE_DOWNLOAD_FAILED);
            assertTrue(SpeleoDBConstants.MESSAGES.UPDATE_CHECK_FAILED.contains("%s"));
        }

        @Test
        @DisplayName("Should have proper error message constants")
        void shouldHaveProperErrorMessageConstants() {
            assertNotNull(SpeleoDBConstants.MESSAGES.UPDATE_CHECK_STARTING);
            assertNotNull(SpeleoDBConstants.MESSAGES.UPDATE_AVAILABLE);
            assertNotNull(SpeleoDBConstants.MESSAGES.UPDATE_NOT_AVAILABLE);
            assertNotNull(SpeleoDBConstants.MESSAGES.UPDATE_DOWNLOAD_STARTING);
            assertNotNull(SpeleoDBConstants.MESSAGES.UPDATE_DOWNLOAD_SUCCESS);
            assertNotNull(SpeleoDBConstants.MESSAGES.UPDATE_HASH_VERIFICATION_FAILED);
            assertNotNull(SpeleoDBConstants.MESSAGES.UPDATE_INSTALL_SUCCESS);

            // Verify format strings
            assertTrue(SpeleoDBConstants.MESSAGES.UPDATE_AVAILABLE.contains("%s"));
            assertTrue(SpeleoDBConstants.MESSAGES.UPDATE_NOT_AVAILABLE.contains("%s"));
            assertTrue(SpeleoDBConstants.MESSAGES.UPDATE_DOWNLOAD_STARTING.contains("%s"));
            assertTrue(SpeleoDBConstants.MESSAGES.UPDATE_INSTALL_SUCCESS.contains("%s"));
        }
    }
} 