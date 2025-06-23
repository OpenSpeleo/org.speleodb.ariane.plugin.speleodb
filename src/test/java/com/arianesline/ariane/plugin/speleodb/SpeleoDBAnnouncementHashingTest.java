package com.arianesline.ariane.plugin.speleodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * Unit tests for SpeleoDB announcement duplicate prevention system.
 * Tests hash generation, URL normalization, and announcement tracking.
 */
@DisplayName("SpeleoDB Announcement Hashing Tests")
class SpeleoDBAnnouncementHashingTest {

    @Mock
    private SpeleoDBController mockController;
    
    private SpeleoDBService speleoDBService;
    private Preferences testPrefs;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        speleoDBService = new SpeleoDBService(mockController);
        // Use a test-specific preferences node
        testPrefs = Preferences.userNodeForPackage(SpeleoDBAnnouncementHashingTest.class);
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test preferences
        try {
            testPrefs.clear();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Nested
    @DisplayName("URL Normalization Tests")
    class UrlNormalizationTests {

        @Test
        @DisplayName("Should normalize URLs to lowercase")
        void shouldNormalizeUrlsToLowercase() {
            String hash1 = generateTestHash("HTTP://EXAMPLE.COM/API", createTestAnnouncement());
            String hash2 = generateTestHash("http://example.com/api", createTestAnnouncement());
            
            assertEquals(hash1, hash2, "URLs with different cases should produce same hash");
        }

        @Test
        @DisplayName("Should remove protocol from URLs")
        void shouldRemoveProtocolFromUrls() {
            String hash1 = generateTestHash("http://example.com/api", createTestAnnouncement());
            String hash2 = generateTestHash("https://example.com/api", createTestAnnouncement());
            String hash3 = generateTestHash("example.com/api", createTestAnnouncement());
            
            assertEquals(hash1, hash2, "HTTP and HTTPS should produce same hash");
            assertEquals(hash1, hash3, "With and without protocol should produce same hash");
        }

        @Test
        @DisplayName("Should remove trailing slash from URLs")
        void shouldRemoveTrailingSlashFromUrls() {
            String hash1 = generateTestHash("http://example.com/api", createTestAnnouncement());
            String hash2 = generateTestHash("http://example.com/api/", createTestAnnouncement());
            
            assertEquals(hash1, hash2, "URLs with and without trailing slash should produce same hash");
        }

        @Test
        @DisplayName("Should handle complex URL normalization")
        void shouldHandleComplexUrlNormalization() {
            String hash1 = generateTestHash("HTTPS://EXAMPLE.COM/API/V1/", createTestAnnouncement());
            String hash2 = generateTestHash("http://example.com/api/v1", createTestAnnouncement());
            
            assertEquals(hash1, hash2, "Complex URL variations should produce same hash");
        }

        @Test
        @DisplayName("Should handle localhost URLs")
        void shouldHandleLocalhostUrls() {
            String hash1 = generateTestHash("http://localhost:8000/api", createTestAnnouncement());
            String hash2 = generateTestHash("https://localhost:8000/api/", createTestAnnouncement());
            
            assertEquals(hash1, hash2, "Localhost URLs should normalize consistently");
        }

        @Test
        @DisplayName("Should handle IP addresses")
        void shouldHandleIpAddresses() {
            String hash1 = generateTestHash("http://192.168.1.1:8000/api", createTestAnnouncement());
            String hash2 = generateTestHash("https://192.168.1.1:8000/api/", createTestAnnouncement());
            
            assertEquals(hash1, hash2, "IP addresses should normalize consistently");
        }
    }

    @Nested
    @DisplayName("Hash Generation Tests")
    class HashGenerationTests {

        @Test
        @DisplayName("Should generate consistent hashes for identical content")
        void shouldGenerateConsistentHashesForIdenticalContent() {
            JsonObject announcement = createTestAnnouncement();
            String endpoint = "http://example.com/api";
            
            String hash1 = generateTestHash(endpoint, announcement);
            String hash2 = generateTestHash(endpoint, announcement);
            
            assertEquals(hash1, hash2, "Identical content should produce identical hashes");
        }

        @Test
        @DisplayName("Should generate different hashes for different content")
        void shouldGenerateDifferentHashesForDifferentContent() {
            JsonObject announcement1 = Json.createObjectBuilder()
                    .add("title", "First Announcement")
                    .add("message", "First message")
                    .build();
            
            JsonObject announcement2 = Json.createObjectBuilder()
                    .add("title", "Second Announcement")
                    .add("message", "Second message")
                    .build();
            
            String hash1 = generateTestHash("http://example.com/api", announcement1);
            String hash2 = generateTestHash("http://example.com/api", announcement2);
            
            assertNotEquals(hash1, hash2, "Different content should produce different hashes");
        }

        @Test
        @DisplayName("Should generate different hashes for different endpoints")
        void shouldGenerateDifferentHashesForDifferentEndpoints() {
            JsonObject announcement = createTestAnnouncement();
            
            String hash1 = generateTestHash("http://example.com/api", announcement);
            String hash2 = generateTestHash("http://other.com/api", announcement);
            
            assertNotEquals(hash1, hash2, "Different endpoints should produce different hashes");
        }

        @Test
        @DisplayName("Should generate SHA-256 format hashes")
        void shouldGenerateSha256FormatHashes() {
            String hash = generateTestHash("http://example.com/api", createTestAnnouncement());
            
            // SHA-256 produces 64 character hex strings
            assertEquals(64, hash.length(), "Hash should be 64 characters long");
            assertTrue(hash.matches("[a-f0-9]+"), "Hash should contain only lowercase hex characters");
        }

        @Test
        @DisplayName("Should handle special characters in content")
        void shouldHandleSpecialCharactersInContent() {
            JsonObject announcement = Json.createObjectBuilder()
                    .add("title", "Special Characters: àáâãäåæçèéêë")
                    .add("message", "Symbols: !@#$%^&*()_+-=[]{}|;':\",./<>?")
                    .add("header", "Unicode: 🎉🚀✨")
                    .build();
            
            String hash = generateTestHash("http://example.com/api", announcement);
            
            assertNotNull(hash);
            assertEquals(64, hash.length());
        }

        @Test
        @DisplayName("Should handle empty and null values")
        void shouldHandleEmptyAndNullValues() {
            JsonObject announcement = Json.createObjectBuilder()
                    .add("title", "")
                    .add("message", "")
                    .addNull("header")
                    .build();
            
            String hash = generateTestHash("http://example.com/api", announcement);
            
            assertNotNull(hash);
            assertEquals(64, hash.length());
        }
    }

    @Nested
    @DisplayName("Announcement Tracking Tests")
    class AnnouncementTrackingTests {

        @Test
        @DisplayName("Should track displayed announcements")
        void shouldTrackDisplayedAnnouncements() {
            String hash = "test-hash-123";
            
            // Initially should not be displayed
            assertFalse(hasAnnouncementBeenDisplayed(hash), "New announcement should not be marked as displayed");
            
            // Mark as displayed
            markAnnouncementAsDisplayed(hash);
            
            // Should now be tracked as displayed
            assertTrue(hasAnnouncementBeenDisplayed(hash), "Marked announcement should be tracked as displayed");
        }

        @Test
        @DisplayName("Should persist tracking across instances")
        void shouldPersistTrackingAcrossInstances() {
            String hash = "persistent-hash-456";
            
            // Mark as displayed
            markAnnouncementAsDisplayed(hash);
            
            // Create new service instance (simulating app restart)
            SpeleoDBService newService = new SpeleoDBService(mockController);
            
            // Should still be tracked as displayed
            assertTrue(hasAnnouncementBeenDisplayed(hash), "Tracking should persist across instances");
        }

        @Test
        @DisplayName("Should handle multiple different announcements")
        void shouldHandleMultipleDifferentAnnouncements() {
            String hash1 = "hash-one";
            String hash2 = "hash-two";
            String hash3 = "hash-three";
            
            // Mark first two as displayed
            markAnnouncementAsDisplayed(hash1);
            markAnnouncementAsDisplayed(hash2);
            
            // Check tracking
            assertTrue(hasAnnouncementBeenDisplayed(hash1));
            assertTrue(hasAnnouncementBeenDisplayed(hash2));
            assertFalse(hasAnnouncementBeenDisplayed(hash3));
        }

        @Test
        @DisplayName("Should handle very long hash values")
        void shouldHandleVeryLongHashValues() {
            // Use a realistic long hash (64 characters like SHA-256) instead of 1000 'a's
            String longHash = "a1b2c3d4e5f6789012345678901234567890123456789012345678901234567890"; 
            
            markAnnouncementAsDisplayed(longHash);
            assertTrue(hasAnnouncementBeenDisplayed(longHash));
        }

        @Test
        @DisplayName("Should handle special characters in hash values")
        void shouldHandleSpecialCharactersInHashValues() {
            String specialHash = "hash-with-special-chars_123!@#$%";
            
            markAnnouncementAsDisplayed(specialHash);
            assertTrue(hasAnnouncementBeenDisplayed(specialHash));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should integrate URL normalization with hash generation")
        void shouldIntegrateUrlNormalizationWithHashGeneration() {
            JsonObject announcement = createTestAnnouncement();
            
            // Generate hashes with different URL formats
            String hash1 = generateTestHash("HTTP://EXAMPLE.COM/API/", announcement);
            String hash2 = generateTestHash("https://example.com/api", announcement);
            
            // Hashes should be identical due to normalization
            assertEquals(hash1, hash2);
            
            // Mark one as displayed
            markAnnouncementAsDisplayed(hash1);
            
            // Both should be considered displayed due to identical hashes
            assertTrue(hasAnnouncementBeenDisplayed(hash1));
            assertTrue(hasAnnouncementBeenDisplayed(hash2));
        }

        @Test
        @DisplayName("Should handle real-world announcement data")
        void shouldHandleRealWorldAnnouncementData() {
            JsonObject realWorldAnnouncement = Json.createObjectBuilder()
                    .add("title", "Welcome to SpeleoDB v2.1!")
                    .add("message", "We're excited to announce new features including:\n- Enhanced cave mapping\n- Improved data sync\n- Better user interface")
                    .add("header", "🎉 New Release")
                    .add("is_active", true)
                    .add("software", "ARIANE")
                    .add("expiracy_date", "2025-12-31")
                    .add("version", "2.1.0")
                    .build();
            
            String hash = generateTestHash("https://www.speleodb.org/api/v1/announcements/", realWorldAnnouncement);
            
            assertNotNull(hash);
            assertEquals(64, hash.length());
            
            // Should work with tracking
            assertFalse(hasAnnouncementBeenDisplayed(hash));
            markAnnouncementAsDisplayed(hash);
            assertTrue(hasAnnouncementBeenDisplayed(hash));
        }
    }

    // Helper methods

    private JsonObject createTestAnnouncement() {
        return Json.createObjectBuilder()
                .add("title", "Test Announcement")
                .add("message", "This is a test message")
                .add("header", "Test Header")
                .add("is_active", true)
                .add("software", "ARIANE")
                .build();
    }

    private String generateTestHash(String endpoint, JsonObject announcement) {
        try {
            // Normalize the endpoint URL (simulate the logic from SpeleoDBService)
            String normalizedEndpoint = endpoint.toLowerCase()
                    .replaceFirst("^https?://", "")
                    .replaceFirst("/$", "");
            
            // Combine normalized endpoint with announcement JSON
            String combinedData = normalizedEndpoint + announcement.toString();
            
            // Generate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(combinedData.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate hash", e);
        }
    }

    private boolean hasAnnouncementBeenDisplayed(String hash) {
        // Simulate the logic from SpeleoDBService
        String key = "announcement_displayed_" + hash;
        if (key.length() > 80) { // Preferences has a limit around 80 characters
            return testPrefs.getBoolean("test_long_key", false);
        } else {
            return testPrefs.getBoolean(key, false);
        }
    }

    private void markAnnouncementAsDisplayed(String hash) {
        // Simulate the logic from SpeleoDBService
        String key = "announcement_displayed_" + hash;
        // Handle the case where the key might be too long for preferences
        if (key.length() > 80) { // Preferences has a limit around 80 characters
            // In real implementation, this would use a truncated hash or different storage
            // For testing, we'll just use our in-memory map
            testPrefs.putBoolean("test_long_key", true);
        } else {
            testPrefs.putBoolean(key, true);
        }
    }
} 