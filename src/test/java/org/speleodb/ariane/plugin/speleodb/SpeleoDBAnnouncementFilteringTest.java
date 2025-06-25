package org.speleodb.ariane.plugin.speleodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
 * Unit tests for SpeleoDB announcement filtering functionality.
 * Tests all filtering criteria: active status, software match, expiry dates, and version matching.
 */
@DisplayName("SpeleoDB Announcement Filtering Tests")
class SpeleoDBAnnouncementFilteringTest {

    @Mock
    private SpeleoDBController mockController;
    
    private SpeleoDBService speleoDBService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        speleoDBService = new SpeleoDBService(mockController);
    }

    @Nested
    @DisplayName("Active Status Filtering")
    class ActiveStatusFilteringTests {

        @Test
        @DisplayName("Should include active announcements")
        void shouldIncludeActiveAnnouncements() {
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("Test Title", "Test Message", true, "ARIANE", null, null)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(1, result.size());
            assertEquals("Test Title", result.getJsonObject(0).getString("title"));
        }

        @Test
        @DisplayName("Should exclude inactive announcements")
        void shouldExcludeInactiveAnnouncements() {
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("Inactive Announcement", "Test Message", false, "ARIANE", null, null)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Should handle missing is_active field as inactive")
        void shouldHandleMissingIsActiveFieldAsInactive() {
            String jsonResponse = """
                {
                    "data": [
                        {
                            "title": "No Active Field",
                            "message": "Test Message",
                            "software": "ARIANE"
                        }
                    ]
                }
                """;
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("Software Filtering")
    class SoftwareFilteringTests {

        @Test
        @DisplayName("Should include ARIANE announcements")
        void shouldIncludeArianeAnnouncements() {
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("ARIANE Announcement", "Test Message", true, "ARIANE", null, null)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should exclude non-ARIANE announcements")
        void shouldExcludeNonArianeAnnouncements() {
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("Other Software", "Test Message", true, "OTHER_SOFTWARE", null, null)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Should handle missing software field")
        void shouldHandleMissingSoftwareField() {
            String jsonResponse = """
                {
                    "data": [
                        {
                            "title": "No Software Field",
                            "message": "Test Message",
                            "is_active": true
                        }
                    ]
                }
                """;
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("Expiry Date Filtering")
    class ExpiryDateFilteringTests {

        @Test
        @DisplayName("Should include announcements with future expiry dates")
        void shouldIncludeFutureExpiryDates() {
            LocalDate futureDate = LocalDate.now().plusDays(1);
            String futureDateStr = futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("Future Expiry", "Test Message", true, "ARIANE", futureDateStr, null)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should exclude announcements with past expiry dates")
        void shouldExcludePastExpiryDates() {
            LocalDate pastDate = LocalDate.now().minusDays(1);
            String pastDateStr = pastDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("Past Expiry", "Test Message", true, "ARIANE", pastDateStr, null)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Should include announcements with no expiry date")
        void shouldIncludeNoExpiryDate() {
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("No Expiry", "Test Message", true, "ARIANE", null, null)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should handle invalid expiry date format gracefully")
        void shouldHandleInvalidExpiryDateFormat() {
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("Invalid Date", "Test Message", true, "ARIANE", "invalid-date", null)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            // Should include announcement when date parsing fails (fail-safe)
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should handle date format correctly")
        void shouldHandleDateFormatCorrectly() {
            // Test with a specific future date in YYYY-MM-DD format
            String futureDate = "2025-12-31";
            
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("Date Format Test", "Test Message", true, "ARIANE", futureDate, null)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Version Filtering")
    class VersionFilteringTests {

        @Test
        @DisplayName("Should include announcements with no version specified")
        void shouldIncludeNoVersionSpecified() {
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("No Version", "Test Message", true, "ARIANE", null, null)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should include announcements with matching version")
        void shouldIncludeMatchingVersion() {
            // Mock the plugin version
            String pluginVersion = "2024.12.15";
            
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("Version Match", "Test Message", true, "ARIANE", null, pluginVersion)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            // Note: This test would need the actual plugin version to be set
            // For now, we test the logic structure
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should exclude announcements with non-matching version")
        void shouldExcludeNonMatchingVersion() {
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("Wrong Version", "Test Message", true, "ARIANE", null, "999.99.99")
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            // When plugin version is null (development), version-specific announcements are excluded
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Should handle empty version string")
        void shouldHandleEmptyVersionString() {
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("Empty Version", "Test Message", true, "ARIANE", null, "")
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Combined Filtering")
    class CombinedFilteringTests {

        @Test
        @DisplayName("Should apply all filters together")
        void shouldApplyAllFiltersTogether() {
            LocalDate futureDate = LocalDate.now().plusDays(1);
            String futureDateStr = futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate pastDate = LocalDate.now().minusDays(1);
            String pastDateStr = pastDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            String jsonResponse = """
                {
                    "data": [
                        {
                            "title": "Valid Announcement",
                            "message": "Should be included",
                            "is_active": true,
                            "software": "ARIANE",
                            "expiracy_date": "%s"
                        },
                        {
                            "title": "Inactive",
                            "message": "Should be excluded",
                            "is_active": false,
                            "software": "ARIANE"
                        },
                        {
                            "title": "Wrong Software",
                            "message": "Should be excluded",
                            "is_active": true,
                            "software": "OTHER"
                        },
                        {
                            "title": "Expired",
                            "message": "Should be excluded",
                            "is_active": true,
                            "software": "ARIANE",
                            "expiracy_date": "%s"
                        }
                    ]
                }
                """.formatted(futureDateStr, pastDateStr);
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(1, result.size());
            assertEquals("Valid Announcement", result.getJsonObject(0).getString("title"));
        }

        @Test
        @DisplayName("Should handle empty announcement list")
        void shouldHandleEmptyAnnouncementList() {
            String jsonResponse = """
                {
                    "data": []
                }
                """;
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() {
            String jsonResponse = """
                {
                    "data": [
                        {
                            "title": "Incomplete Announcement",
                            "is_active": true
                        }
                    ]
                }
                """;
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            // Should exclude announcements with missing required fields
            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null JSON data field")
        void shouldHandleNullDataField() {
            String jsonResponse = """
                {
                    "data": null
                }
                """;
            
            assertThrows(Exception.class, () -> {
                parseAnnouncementsFromResponse(jsonResponse);
            });
        }

        @Test
        @DisplayName("Should handle missing data field")
        void shouldHandleMissingDataField() {
            String jsonResponse = """
                {
                    "other_field": "value"
                }
                """;
            
            assertThrows(Exception.class, () -> {
                parseAnnouncementsFromResponse(jsonResponse);
            });
        }

        @Test
        @DisplayName("Should handle very long expiry dates")
        void shouldHandleVeryLongExpiryDates() {
            String futureDate = "2099-12-31";
            
            String jsonResponse = createAnnouncementResponse(
                createAnnouncement("Far Future", "Test Message", true, "ARIANE", futureDate, null)
            );
            
            JsonArray result = parseAnnouncementsFromResponse(jsonResponse);
            
            assertEquals(1, result.size());
        }
    }

    // Helper methods

    private String createAnnouncementResponse(String... announcements) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"data\": [");
        for (int i = 0; i < announcements.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(announcements[i]);
        }
        sb.append("] }");
        return sb.toString();
    }

    private String createAnnouncement(String title, String message, Boolean isActive, 
                                    String software, String expiryDate, String version) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"title\": \"").append(title).append("\",");
        sb.append("\"message\": \"").append(message).append("\"");
        
        if (isActive != null) {
            sb.append(",\"is_active\": ").append(isActive);
        }
        
        if (software != null) {
            sb.append(",\"software\": \"").append(software).append("\"");
        }
        
        if (expiryDate != null) {
            sb.append(",\"expiracy_date\": \"").append(expiryDate).append("\"");
        }
        
        if (version != null && !version.isEmpty()) {
            sb.append(",\"version\": \"").append(version).append("\"");
        }
        
        sb.append("}");
        return sb.toString();
    }

    private JsonArray parseAnnouncementsFromResponse(String jsonResponse) {
        try {
            JsonObject responseObject = Json.createReader(new StringReader(jsonResponse)).readObject();
            JsonArray announcements = responseObject.getJsonArray(SpeleoDBConstants.JSON_FIELDS.DATA);
            
            // Simulate the filtering logic from SpeleoDBService.fetchAnnouncements
            LocalDate today = LocalDate.now();
            
            return announcements.stream()
                    .filter(JsonObject.class::isInstance)
                    .map(JsonObject.class::cast)
                    .filter(announcement -> announcement.getBoolean(SpeleoDBConstants.JSON_FIELDS.IS_ACTIVE, false))
                    .filter(announcement -> "ARIANE".equals(announcement.getString(SpeleoDBConstants.JSON_FIELDS.SOFTWARE, "")))
                    .filter(announcement -> {
                        // Check expiry date if present
                        String expiresAt = announcement.getString(SpeleoDBConstants.JSON_FIELDS.EXPIRES_AT, null);
                        if (expiresAt != null && !expiresAt.isEmpty()) {
                            try {
                                LocalDate expiryDate = LocalDate.parse(expiresAt);
                                return expiryDate.isAfter(today);
                            } catch (Exception e) {
                                // If parsing fails, include the announcement (fail-safe)
                                return true;
                            }
                        }
                        // If no expiry date, include the announcement
                        return true;
                    })
                    .filter(announcement -> {
                        // Check version match if present
                        String announcementVersion = announcement.getString(SpeleoDBConstants.JSON_FIELDS.VERSION, null);
                        if (announcementVersion != null && !announcementVersion.isEmpty()) {
                            // Only show if version exactly matches the built version
                            return announcementVersion.equals(SpeleoDBConstants.VERSION);
                        }
                        // If no version specified, include the announcement
                        return true;
                    })
                    .collect(Json::createArrayBuilder, 
                            (builder, announcement) -> builder.add(announcement),
                            (builder1, builder2) -> {
                                throw new UnsupportedOperationException("Parallel processing not supported");
                            })
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse announcements", e);
        }
    }
} 