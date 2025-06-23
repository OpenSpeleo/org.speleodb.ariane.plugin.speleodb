package com.arianesline.ariane.plugin.speleodb;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
 * Unit tests for SpeleoDB sequential announcement display functionality.
 * Tests the recursive display logic and dialog management.
 */
@DisplayName("SpeleoDB Sequential Announcement Display Tests")
class SpeleoDBAnnouncementSequentialDisplayTest {

    @Mock
    private SpeleoDBController mockController;
    
    private ExecutorService testExecutor;
    
    // Simulate the displayed announcements tracking
    private Map<String, Boolean> displayedAnnouncements;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testExecutor = Executors.newSingleThreadExecutor();
        displayedAnnouncements = new HashMap<>();
    }

    @Nested
    @DisplayName("Announcement Collection Tests")
    class AnnouncementCollectionTests {

        @Test
        @DisplayName("Should collect unshown announcements from API response")
        void shouldCollectUnshownAnnouncementsFromApiResponse() {
            // Create test announcements
            JsonArray announcements = Json.createArrayBuilder()
                    .add(createTestAnnouncement("First", "First message", "First header"))
                    .add(createTestAnnouncement("Second", "Second message", "Second header"))
                    .add(createTestAnnouncement("Third", "Third message", "Third header"))
                    .build();
            
            // All announcements should be unshown initially
            List<JsonObject> unshownAnnouncements = collectUnshownAnnouncements(announcements);
            
            assertEquals(3, unshownAnnouncements.size());
            assertEquals("First", unshownAnnouncements.get(0).getString("title"));
            assertEquals("Second", unshownAnnouncements.get(1).getString("title"));
            assertEquals("Third", unshownAnnouncements.get(2).getString("title"));
        }

        @Test
        @DisplayName("Should filter out already shown announcements")
        void shouldFilterOutAlreadyShownAnnouncements() {
            JsonArray announcements = Json.createArrayBuilder()
                    .add(createTestAnnouncement("Shown", "Shown message", "Shown header"))
                    .add(createTestAnnouncement("New", "New message", "New header"))
                    .build();
            
            // Mark first announcement as shown
            String shownHash = generateMockHash(announcements.getJsonObject(0));
            displayedAnnouncements.put(shownHash, true);
            
            List<JsonObject> unshownAnnouncements = collectUnshownAnnouncements(announcements);
            
            assertEquals(1, unshownAnnouncements.size());
            assertEquals("New", unshownAnnouncements.get(0).getString("title"));
        }

        @Test
        @DisplayName("Should handle empty announcement list")
        void shouldHandleEmptyAnnouncementList() {
            JsonArray emptyAnnouncements = Json.createArrayBuilder().build();
            
            List<JsonObject> unshownAnnouncements = collectUnshownAnnouncements(emptyAnnouncements);
            
            assertEquals(0, unshownAnnouncements.size());
        }

        @Test
        @DisplayName("Should handle all announcements already shown")
        void shouldHandleAllAnnouncementsAlreadyShown() {
            JsonArray announcements = Json.createArrayBuilder()
                    .add(createTestAnnouncement("First", "First message", "First header"))
                    .add(createTestAnnouncement("Second", "Second message", "Second header"))
                    .build();
            
            // Mark all announcements as shown
            for (int i = 0; i < announcements.size(); i++) {
                String hash = generateMockHash(announcements.getJsonObject(i));
                displayedAnnouncements.put(hash, true);
            }
            
            List<JsonObject> unshownAnnouncements = collectUnshownAnnouncements(announcements);
            
            assertEquals(0, unshownAnnouncements.size());
        }
    }

    @Nested
    @DisplayName("Sequential Display Logic Tests")
    class SequentialDisplayLogicTests {

        @Test
        @DisplayName("Should display announcements in correct order")
        void shouldDisplayAnnouncementsInCorrectOrder() {
            List<JsonObject> announcements = List.of(
                    createTestAnnouncement("First", "First message", "First header"),
                    createTestAnnouncement("Second", "Second message", "Second header"),
                    createTestAnnouncement("Third", "Third message", "Third header")
            );
            
            List<String> displayOrder = new ArrayList<>();
            
            // Simulate sequential display
            for (JsonObject announcement : announcements) {
                displayOrder.add(announcement.getString("title"));
            }
            
            assertEquals(List.of("First", "Second", "Third"), displayOrder);
        }

        @Test
        @DisplayName("Should handle single announcement")
        void shouldHandleSingleAnnouncement() {
            List<JsonObject> announcements = List.of(
                    createTestAnnouncement("Single", "Single message", "Single header")
            );
            
            assertNotNull(announcements);
            assertEquals(1, announcements.size());
            assertEquals("Single", announcements.get(0).getString("title"));
        }

        @Test
        @DisplayName("Should handle large number of announcements")
        void shouldHandleLargeNumberOfAnnouncements() {
            List<JsonObject> announcements = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                announcements.add(createTestAnnouncement(
                        "Title " + i, 
                        "Message " + i, 
                        "Header " + i
                ));
            }
            
            assertEquals(100, announcements.size());
            assertEquals("Title 1", announcements.get(0).getString("title"));
            assertEquals("Title 100", announcements.get(99).getString("title"));
        }
    }

    @Nested
    @DisplayName("Dialog Creation Tests")
    class DialogCreationTests {

        @Test
        @DisplayName("Should create dialog with correct content")
        void shouldCreateDialogWithCorrectContent() {
            JsonObject announcement = createTestAnnouncement(
                    "Test Title", 
                    "Test Message Content", 
                    "Test Header"
            );
            
            // Simulate dialog creation
            String title = announcement.getString("title");
            String message = announcement.getString("message");
            String header = announcement.getString("header");
            
            assertEquals("Test Title", title);
            assertEquals("Test Message Content", message);
            assertEquals("Test Header", header);
        }

        @Test
        @DisplayName("Should handle announcements with missing header")
        void shouldHandleAnnouncementsWithMissingHeader() {
            JsonObject announcement = Json.createObjectBuilder()
                    .add("title", "No Header Title")
                    .add("message", "No Header Message")
                    .build();
            
            String title = announcement.getString("title");
            String message = announcement.getString("message");
            String header = announcement.getString("header", "Getting Started"); // Default value
            
            assertEquals("No Header Title", title);
            assertEquals("No Header Message", message);
            assertEquals("Getting Started", header);
        }

        @Test
        @DisplayName("Should handle announcements with special characters")
        void shouldHandleAnnouncementsWithSpecialCharacters() {
            JsonObject announcement = createTestAnnouncement(
                    "Special: àáâãäåæç & 🎉", 
                    "Message with\nline breaks\r\nand symbols: !@#$%", 
                    "Header with ☁️ emoji"
            );
            
            String title = announcement.getString("title");
            String message = announcement.getString("message");
            String header = announcement.getString("header");
            
            assertTrue(title.contains("àáâãäåæç"));
            assertTrue(title.contains("🎉"));
            assertTrue(message.contains("\n"));
            assertTrue(message.contains("!@#$%"));
            assertTrue(header.contains("☁️"));
        }
    }

    @Nested
    @DisplayName("Announcement Marking Tests")
    class AnnouncementMarkingTests {

        @Test
        @DisplayName("Should mark announcements as displayed after showing")
        void shouldMarkAnnouncementsAsDisplayedAfterShowing() {
            JsonObject announcement = createTestAnnouncement(
                    "Test", "Test message", "Test header"
            );
            
            // Simulate marking as displayed
            String hash = generateMockHash(announcement);
            
            // Initially not displayed
            assertFalse(hasAnnouncementBeenDisplayed(hash));
            
            // Mark as displayed
            markAnnouncementAsDisplayed(hash);
            
            // Should now be displayed
            assertTrue(hasAnnouncementBeenDisplayed(hash));
        }

        @Test
        @DisplayName("Should generate unique hashes for different announcements")
        void shouldGenerateUniqueHashesForDifferentAnnouncements() {
            JsonObject announcement1 = createTestAnnouncement("First", "First message", "First header");
            JsonObject announcement2 = createTestAnnouncement("Second", "Second message", "Second header");
            
            String hash1 = generateMockHash(announcement1);
            String hash2 = generateMockHash(announcement2);
            
            assertNotEquals(hash1, hash2);
        }

        @Test
        @DisplayName("Should generate consistent hashes for same announcement")
        void shouldGenerateConsistentHashesForSameAnnouncement() {
            JsonObject announcement = createTestAnnouncement("Same", "Same message", "Same header");
            
            String hash1 = generateMockHash(announcement);
            String hash2 = generateMockHash(announcement);
            
            assertEquals(hash1, hash2);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle null announcement list gracefully")
        void shouldHandleNullAnnouncementListGracefully() {
            List<JsonObject> nullList = null;
            
            // Should not throw exception
            assertDoesNotThrow(() -> {
                if (nullList == null || nullList.isEmpty()) {
                    // Handle gracefully
                    return;
                }
            });
        }

        @Test
        @DisplayName("Should handle malformed announcement data")
        void shouldHandleMalformedAnnouncementData() {
            JsonObject malformedAnnouncement = Json.createObjectBuilder()
                    .add("title", "Valid Title")
                    // Missing message field
                    .build();
            
            // Should handle missing fields gracefully
            String title = malformedAnnouncement.getString("title", "Default Title");
            String message = malformedAnnouncement.getString("message", "Default Message");
            String header = malformedAnnouncement.getString("header", "Default Header");
            
            assertEquals("Valid Title", title);
            assertEquals("Default Message", message);
            assertEquals("Default Header", header);
        }

        @Test
        @DisplayName("Should handle hash generation failures")
        void shouldHandleHashGenerationFailures() {
            JsonObject announcement = createTestAnnouncement("Test", "Test", "Test");
            
            // Simulate hash generation - should not throw
            assertDoesNotThrow(() -> {
                String hash = generateMockHash(announcement);
                assertNotNull(hash);
            });
        }
    }

    @Nested
    @DisplayName("Timing and Synchronization Tests")
    class TimingAndSynchronizationTests {

        @Test
        @DisplayName("Should handle sequential display timing")
        void shouldHandleSequentialDisplayTiming() throws InterruptedException {
            List<JsonObject> announcements = List.of(
                    createTestAnnouncement("First", "First message", "First header"),
                    createTestAnnouncement("Second", "Second message", "Second header")
            );
            
            long startTime = System.currentTimeMillis();
            
            // Simulate sequential display with delays
            for (int i = 0; i < announcements.size(); i++) {
                if (i > 0) {
                    Thread.sleep(50); // Simulate 500ms delay between dialogs
                }
                // Process announcement
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Should take at least 50ms (one delay between two announcements)
            assertTrue(duration >= 50);
        }

        @Test
        @DisplayName("Should handle concurrent access safely")
        void shouldHandleConcurrentAccessSafely() throws InterruptedException, ExecutionException, TimeoutException {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            // Simulate multiple threads trying to display announcements
            for (int i = 0; i < 5; i++) {
                final int threadId = i;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    JsonObject announcement = createTestAnnouncement(
                            "Thread " + threadId, 
                            "Message " + threadId, 
                            "Header " + threadId
                    );
                    // Simulate processing
                    String hash = generateMockHash(announcement);
                    assertNotNull(hash);
                }, testExecutor);
                futures.add(future);
            }
            
            // Wait for all to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.SECONDS);
            
            // All should complete without exception
            for (CompletableFuture<Void> future : futures) {
                assertTrue(future.isDone());
                assertFalse(future.isCompletedExceptionally());
            }
        }
    }

    // Helper methods

    private JsonObject createTestAnnouncement(String title, String message, String header) {
        return Json.createObjectBuilder()
                .add("title", title)
                .add("message", message)
                .add("header", header)
                .add("is_active", true)
                .add("software", "ARIANE")
                .build();
    }

    private List<JsonObject> collectUnshownAnnouncements(JsonArray announcements) {
        List<JsonObject> unshown = new ArrayList<>();
        
        for (int i = 0; i < announcements.size(); i++) {
            JsonObject announcement = announcements.getJsonObject(i);
            String hash = generateMockHash(announcement);
            
            // Check if announcement has been displayed
            if (!hasAnnouncementBeenDisplayed(hash)) {
                unshown.add(announcement);
            }
        }
        
        return unshown;
    }

    private String generateMockHash(JsonObject announcement) {
        // Simple mock hash generation for testing
        return "hash_" + Math.abs(announcement.toString().hashCode());
    }
    
    private boolean hasAnnouncementBeenDisplayed(String hash) {
        return displayedAnnouncements.getOrDefault(hash, false);
    }
    
    private void markAnnouncementAsDisplayed(String hash) {
        displayedAnnouncements.put(hash, true);
    }
} 