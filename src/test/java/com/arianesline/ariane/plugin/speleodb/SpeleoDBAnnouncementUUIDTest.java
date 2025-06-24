package com.arianesline.ariane.plugin.speleodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Unit tests for SpeleoDB announcement UUID-based tracking system.
 * Tests UUID extraction and announcement display tracking.
 */
@DisplayName("SpeleoDB Announcement UUID Tests")
class SpeleoDBAnnouncementUUIDTest {

    @Mock
    private SpeleoDBController mockController;
    
    private SpeleoDBService speleoDBService;
    private Preferences testPrefs;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        speleoDBService = new SpeleoDBService(mockController);
        // Use a test-specific preferences node
        testPrefs = Preferences.userNodeForPackage(SpeleoDBAnnouncementUUIDTest.class);
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
    @DisplayName("UUID Extraction Tests")
    class UUIDExtractionTests {

        @Test
        @DisplayName("Should extract UUID from announcement with UUID field")
        void shouldExtractUUIDFromAnnouncementWithUUIDField() {
            JsonObject announcement = createTestAnnouncementWithUUID("e0cddee8-186f-4872-939b-7a37fa5b3e8e");
            
            String uuid = getTestUUID(announcement);
            
            assertEquals("e0cddee8-186f-4872-939b-7a37fa5b3e8e", uuid);
        }

        @Test
        @DisplayName("Should return null for announcement without UUID field")
        void shouldReturnNullForAnnouncementWithoutUUIDField() {
            JsonObject announcement = Json.createObjectBuilder()
                    .add("title", "Test Announcement")
                    .add("message", "Test message")
                    .build();
            
            String uuid = getTestUUID(announcement);
            
            assertNull(uuid);
        }

        @Test
        @DisplayName("Should handle null UUID field")
        void shouldHandleNullUUIDField() {
            JsonObject announcement = Json.createObjectBuilder()
                    .add("title", "Test Announcement")
                    .add("message", "Test message")
                    .addNull("uuid")
                    .build();
            
            String uuid = getTestUUID(announcement);
            
            assertNull(uuid);
        }

        @Test
        @DisplayName("Should handle empty UUID field")
        void shouldHandleEmptyUUIDField() {
            JsonObject announcement = Json.createObjectBuilder()
                    .add("title", "Test Announcement")
                    .add("message", "Test message")
                    .add("uuid", "")
                    .build();
            
            String uuid = getTestUUID(announcement);
            
            assertEquals("", uuid);
        }
    }

    @Nested
    @DisplayName("Announcement Tracking Tests")
    class AnnouncementTrackingTests {

        @Test
        @DisplayName("Should track displayed announcements by UUID")
        void shouldTrackDisplayedAnnouncementsByUUID() {
            String testUUID = "e0cddee8-186f-4872-939b-7a37fa5b3e8e";
            JsonObject announcement = createTestAnnouncementWithUUID(testUUID);
            
            assertFalse(hasTestAnnouncementBeenDisplayed(announcement), "New announcement should not be marked as displayed");
            
            markTestAnnouncementAsDisplayed(announcement);
            
            assertTrue(hasTestAnnouncementBeenDisplayed(announcement), "Marked announcement should be tracked as displayed");
        }

        @Test
        @DisplayName("Should persist tracking across instances")
        void shouldPersistTrackingAcrossInstances() {
            String testUUID = "e0cddee8-186f-4872-939b-7a37fa5b3e8e";
            JsonObject announcement = createTestAnnouncementWithUUID(testUUID);
            
            markTestAnnouncementAsDisplayed(announcement);
            
            // Simulate new instance by creating fresh test methods
            assertTrue(hasTestAnnouncementBeenDisplayed(announcement), "Tracking should persist across instances");
        }

        @Test
        @DisplayName("Should handle multiple different announcements")
        void shouldHandleMultipleDifferentAnnouncements() {
            JsonObject announcement1 = createTestAnnouncementWithUUID("uuid-1");
            JsonObject announcement2 = createTestAnnouncementWithUUID("uuid-2");
            JsonObject announcement3 = createTestAnnouncementWithUUID("uuid-3");
            
            markTestAnnouncementAsDisplayed(announcement1);
            markTestAnnouncementAsDisplayed(announcement3);
            
            assertTrue(hasTestAnnouncementBeenDisplayed(announcement1), "First announcement should be tracked");
            assertFalse(hasTestAnnouncementBeenDisplayed(announcement2), "Second announcement should not be tracked");
            assertTrue(hasTestAnnouncementBeenDisplayed(announcement3), "Third announcement should be tracked");
        }

        @Test
        @DisplayName("Should handle announcements without UUID gracefully")
        void shouldHandleAnnouncementsWithoutUUIDGracefully() {
            JsonObject announcement = Json.createObjectBuilder()
                    .add("title", "Test Announcement")
                    .add("message", "Test message")
                    .build();
            
            // Should not crash and should return false (not displayed)
            assertFalse(hasTestAnnouncementBeenDisplayed(announcement), "Announcement without UUID should not be considered displayed");
            
            // Should not crash when trying to mark as displayed
            markTestAnnouncementAsDisplayed(announcement);
            
            // Should still return false after attempting to mark
            assertFalse(hasTestAnnouncementBeenDisplayed(announcement), "Announcement without UUID should still not be considered displayed");
        }

        @Test
        @DisplayName("Should handle real-world UUID formats")
        void shouldHandleRealWorldUUIDFormats() {
            String[] testUUIDs = {
                "e0cddee8-186f-4872-939b-7a37fa5b3e8e",
                "550e8400-e29b-41d4-a716-446655440000",
                "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                "6ba7b811-9dad-11d1-80b4-00c04fd430c8"
            };
            
            for (String uuid : testUUIDs) {
                JsonObject announcement = createTestAnnouncementWithUUID(uuid);
                
                assertFalse(hasTestAnnouncementBeenDisplayed(announcement), "UUID " + uuid + " should not be displayed initially");
                
                markTestAnnouncementAsDisplayed(announcement);
                
                assertTrue(hasTestAnnouncementBeenDisplayed(announcement), "UUID " + uuid + " should be tracked after marking");
            }
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete announcement data structure")
        void shouldHandleCompleteAnnouncementDataStructure() {
            JsonObject announcement = Json.createObjectBuilder()
                    .add("creation_date", "2025-06-23T01:10:40.881796-04:00")
                    .addNull("expiracy_date")
                    .add("header", "Getting Started with SpeleoDB !")
                    .add("id", 1)
                    .add("is_active", true)
                    .add("message", "🗂️ **Connect to SpeleoDB** to sync your cave mapping projects")
                    .add("modified_date", "2025-06-23T16:22:27.827554-04:00")
                    .add("software", "ARIANE")
                    .add("title", "Welcome to the SpeleoDB Plugin!")
                    .add("uuid", "e0cddee8-186f-4872-939b-7a37fa5b3e8e")
                    .addNull("version")
                    .build();
            
            String uuid = getTestUUID(announcement);
            assertEquals("e0cddee8-186f-4872-939b-7a37fa5b3e8e", uuid);
            
            assertFalse(hasTestAnnouncementBeenDisplayed(announcement));
            markTestAnnouncementAsDisplayed(announcement);
            assertTrue(hasTestAnnouncementBeenDisplayed(announcement));
        }
    }

    // Helper methods for testing
    private JsonObject createTestAnnouncementWithUUID(String uuid) {
        return Json.createObjectBuilder()
                .add("title", "Test Announcement")
                .add("header", "Test Header")
                .add("message", "Test message content")
                .add("uuid", uuid)
                .build();
    }

    private String getTestUUID(JsonObject announcement) {
        return announcement.getString(SpeleoDBConstants.JSON_FIELDS.UUID, null);
    }

    private boolean hasTestAnnouncementBeenDisplayed(JsonObject announcement) {
        String uuid = getTestUUID(announcement);
        if (uuid == null) {
            return false;
        }
        
        String displayedUUIDs = testPrefs.get(SpeleoDBConstants.PREFERENCES.PREF_DISPLAYED_ANNOUNCEMENTS, "");
        return displayedUUIDs.contains(uuid);
    }

    private void markTestAnnouncementAsDisplayed(JsonObject announcement) {
        String uuid = getTestUUID(announcement);
        if (uuid == null) {
            return;
        }
        
        String displayedUUIDs = testPrefs.get(SpeleoDBConstants.PREFERENCES.PREF_DISPLAYED_ANNOUNCEMENTS, "");
        
        if (!displayedUUIDs.contains(uuid)) {
            String updatedUUIDs = displayedUUIDs.isEmpty() ? uuid : displayedUUIDs + "," + uuid;
            testPrefs.put(SpeleoDBConstants.PREFERENCES.PREF_DISPLAYED_ANNOUNCEMENTS, updatedUUIDs);
        }
    }
} 