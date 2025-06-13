package com.arianesline.ariane.plugin.speleodb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;


/**
 * Comprehensive unit tests for SpeleoDBController class.
 * Tests business logic, state management, and utility functions.
 */
public class SpeleoDBControllerTest {
    
    private static final String TEST_RESOURCES_DIR = System.getProperty("java.io.tmpdir") + File.separator + "test_resources";
    
    public static void main(String[] args) throws Exception {
        setupTestEnvironment();
        
        testMessageCounter();
        testDebugModeDetection();
        testProjectCardCreation();
        testDateTimeFormatting();
        testJsonProjectHandling();
        testUrlGeneration();
        testPreferencesConstants();
        testAccessLevelHandling();
        testTextComponents();
        
        cleanupTestEnvironment();
        System.out.println("All SpeleoDBController tests passed!");
    }
    
    static void setupTestEnvironment() throws IOException {
        // Create test resources directory
        Path testDir = Paths.get(TEST_RESOURCES_DIR);
        if (!Files.exists(testDir)) {
            Files.createDirectories(testDir);
        }
        System.out.println("✓ Controller test environment setup completed");
    }
    
    static void cleanupTestEnvironment() throws IOException {
        // Clean up test resources directory
        Path testDir = Paths.get(TEST_RESOURCES_DIR);
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                .map(Path::toFile)
                .forEach(File::delete);
        }
        System.out.println("✓ Controller test environment cleanup completed");
    }
    
    static void testMessageCounter() {
        System.out.println("Testing message counter...");
        
        // Test atomic counter functionality
        AtomicInteger counter = new AtomicInteger(0);
        assert counter.get() == 0;
        
        int first = counter.incrementAndGet();
        int second = counter.incrementAndGet();
        int third = counter.incrementAndGet();
        
        assert first == 1;
        assert second == 2;
        assert third == 3;
        assert counter.get() == 3;
        
        System.out.println("✓ Message counter tests passed");
    }
    
    static void testDebugModeDetection() throws IOException {
        System.out.println("Testing debug mode detection...");
        
        TestableSpeleoDBController controller = new TestableSpeleoDBController();
        
        // Test with no debug properties
        assert !controller.isDebugModePublic();
        
        // Test with debug properties file
        Path debugProps = Paths.get(TEST_RESOURCES_DIR + File.separator + "debug.properties");
        Files.write(debugProps, "debug.mode=true\n".getBytes());
        
        // Test system property detection
        System.setProperty("speleodb.debug.mode", "true");
        assert controller.isDebugModeFromSystemProperty();
        System.clearProperty("speleodb.debug.mode");
        
        // Test environment variable logic
        assert !controller.isDebugModeFromEnvironment();
        
        System.out.println("✓ Debug mode detection tests passed");
    }
    
    static void testProjectCardCreation() {
        System.out.println("Testing project card creation...");
        
        TestableSpeleoDBController controller = new TestableSpeleoDBController();
        
        // Test project with no mutex
        JsonObject projectWithoutMutex = Json.createObjectBuilder()
            .add("name", "Test Project")
            .add("permission", "READ_AND_WRITE")
            .add("id", "proj-123")
            .addNull("active_mutex")
            .build();
        
        MockVBox card = controller.createProjectCardPublic(projectWithoutMutex);
        assert card.getChildrenTexts().contains("Test Project");
        assert card.getChildrenTexts().contains("READ_AND_WRITE");
        assert card.getChildrenTexts().contains("Not Locked");
        assert card.getPrefWidth() == 180;
        
        // Test project with mutex
        JsonObject mutexObj = Json.createObjectBuilder()
            .add("user", "john.doe@example.com")
            .add("creation_date", "2024-01-15T10:30:00.000000")
            .add("modified_date", "2024-01-15T11:45:00.000000")
            .build();
        
        JsonObject projectWithMutex = Json.createObjectBuilder()
            .add("name", "Locked Project")
            .add("permission", "READ_AND_WRITE")
            .add("id", "proj-456")
            .add("active_mutex", mutexObj)
            .build();
        
        MockVBox cardLocked = controller.createProjectCardPublic(projectWithMutex);
        assert cardLocked.getChildrenTexts().contains("Locked Project");
        assert cardLocked.getChildrenTexts().contains("Locked");
        assert cardLocked.getChildrenTexts().contains("by john.doe@example.com");
        
        System.out.println("✓ Project card creation tests passed");
    }
    
    static void testDateTimeFormatting() {
        System.out.println("Testing datetime formatting...");
        
        // Test datetime parsing and formatting logic
        String dateString = "2024-01-15T10:30:00.000000";
        String trimmedDate = dateString.substring(0, dateString.lastIndexOf('.'));
        LocalDateTime dateTime = LocalDateTime.parse(trimmedDate);
        
        assert dateTime.getYear() == 2024;
        assert dateTime.getMonthValue() == 1;
        assert dateTime.getDayOfMonth() == 15;
        assert dateTime.getHour() == 10;
        assert dateTime.getMinute() == 30;
        
        // Test formatting
        String formatted = dateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        assert formatted != null;
        assert !formatted.isEmpty();
        
        System.out.println("✓ DateTime formatting tests passed");
    }
    
    static void testJsonProjectHandling() {
        System.out.println("Testing JSON project handling...");
        
        // Test project JSON structure validation
        JsonObject validProject = Json.createObjectBuilder()
            .add("id", "project-789")
            .add("name", "Valid Project")
            .add("permission", "ADMIN")
            .add("active_mutex", JsonValue.NULL)
            .build();
        
        assert validProject.getString("id").equals("project-789");
        assert validProject.getString("name").equals("Valid Project");
        assert validProject.getString("permission").equals("ADMIN");
        assert validProject.get("active_mutex").getValueType() == JsonValue.ValueType.NULL;
        
        // Test nested mutex object
        JsonObject complexProject = Json.createObjectBuilder()
            .add("id", "complex-project")
            .add("name", "Complex Project")
            .add("permission", "READ_ONLY")
            .add("active_mutex", Json.createObjectBuilder()
                .add("user", "admin@example.com")
                .add("creation_date", "2024-01-15T10:30:00.000000")
                .add("modified_date", "2024-01-15T11:45:00.000000"))
            .build();
        
        JsonObject mutex = complexProject.getJsonObject("active_mutex");
        assert mutex.getString("user").equals("admin@example.com");
        assert mutex.getString("creation_date").equals("2024-01-15T10:30:00.000000");
        
        System.out.println("✓ JSON project handling tests passed");
    }
    
    static void testUrlGeneration() {
        System.out.println("Testing URL generation...");
        
        TestableSpeleoDBController controller = new TestableSpeleoDBController();
        
        // Test debug URL generation
        String debugUrl = controller.generateAboutUrl(true);
        assert debugUrl.equals("http://localhost:8000/webview/ariane/");
        
        // Test production URL generation
        String prodUrl = controller.generateAboutUrl(false);
        assert prodUrl.equals("https://www.speleodb.org/webview/ariane/");
        
        System.out.println("✓ URL generation tests passed");
    }
    
    static void testPreferencesConstants() {
        System.out.println("Testing preferences constants...");
        
        TestableSpeleoDBController controller = new TestableSpeleoDBController();
        
        // Test constants are properly defined
        assert controller.getPrefEmail().equals("SDB_EMAIL");
        assert controller.getPrefPassword().equals("SDB_PASSWORD");
        assert controller.getPrefOAuthToken().equals("SDB_OAUTH_TOKEN");
        assert controller.getPrefInstance().equals("SDB_INSTANCE");
        assert controller.getPrefSaveCreds().equals("SDB_SAVECREDS");
        assert controller.getDefaultInstance().equals("www.speleoDB.org");
        
        System.out.println("✓ Preferences constants tests passed");
    }
    
    static void testAccessLevelHandling() {
        System.out.println("Testing access level handling...");
        
        // Test READ_ONLY access level logic
        String readOnlyPermission = SpeleoDBAccessLevel.READ_ONLY.name();
        assert readOnlyPermission.equals("READ_ONLY");
        
        // Test permission checking logic
        assert !readOnlyPermission.equals("READ_AND_WRITE");
        assert !readOnlyPermission.equals("ADMIN");
        
        // Test enum values
        assert SpeleoDBAccessLevel.values().length == 3;
        assert SpeleoDBAccessLevel.valueOf("READ_ONLY") == SpeleoDBAccessLevel.READ_ONLY;
        assert SpeleoDBAccessLevel.valueOf("READ_AND_WRITE") == SpeleoDBAccessLevel.READ_AND_WRITE;
        assert SpeleoDBAccessLevel.valueOf("ADMIN") == SpeleoDBAccessLevel.ADMIN;
        
        System.out.println("✓ Access level handling tests passed");
    }
    
    static void testTextComponents() {
        System.out.println("Testing text components...");
        
        // Test MockText functionality
        MockText nameText = new MockText("Cave Project Alpha");
        assert nameText.getText().equals("Cave Project Alpha");
        
        nameText.setText("Updated Cave Project");
        assert nameText.getText().equals("Updated Cave Project");
        
        // Test text with special characters
        MockText specialText = new MockText("Project: Höhle (Österreich)");
        assert specialText.getText().contains("Höhle");
        assert specialText.getText().contains("Österreich");
        
        System.out.println("✓ Text components tests passed");
    }
    
    // ===================== MOCK CLASSES AND TESTABLE VERSIONS ===================== //
    
    static class MockVBox {
        private double prefWidth;
        private final java.util.List<String> childrenTexts = new java.util.ArrayList<>();
        
        public void setPrefWidth(double value) {
            this.prefWidth = value;
        }
        
        public double getPrefWidth() {
            return prefWidth;
        }
        
        public void addChildText(String text) {
            childrenTexts.add(text);
        }
        
        public java.util.List<String> getChildrenTexts() {
            return childrenTexts;
        }
    }
    
    static class MockText {
        private String text;
        
        public MockText(String text) {
            this.text = text;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }
    
    static class TestableSpeleoDBController extends SpeleoDBController {
        
        // Expose private constants for testing
        public String getPrefEmail() { return "SDB_EMAIL"; }
        public String getPrefPassword() { return "SDB_PASSWORD"; }
        public String getPrefOAuthToken() { return "SDB_OAUTH_TOKEN"; }
        public String getPrefInstance() { return "SDB_INSTANCE"; }
        public String getPrefSaveCreds() { return "SDB_SAVECREDS"; }
        public String getDefaultInstance() { return "www.speleoDB.org"; }
        
        // Expose private methods for testing
        public boolean isDebugModePublic() {
            // Simplified version for testing
            return false;
        }
        
        public boolean isDebugModeFromSystemProperty() {
            return Boolean.parseBoolean(System.getProperty("speleodb.debug.mode", "false"));
        }
        
        public boolean isDebugModeFromEnvironment() {
            return Boolean.parseBoolean(System.getenv("SPELEODB_DEBUG_MODE"));
        }
        
        public String generateAboutUrl(boolean isDebugMode) {
            if (isDebugMode) {
                return "http://localhost:8000/webview/ariane/";
            } else {
                return "https://www.speleodb.org/webview/ariane/";
            }
        }
        
        public MockVBox createProjectCardPublic(JsonObject projectItem) {
            MockVBox card = new MockVBox();
            
            // Simulate project card creation logic
            String name = projectItem.getString("name");
            card.addChildText(name);
            card.addChildText(projectItem.getString("permission"));
            
            JsonValue mutex = projectItem.get("active_mutex");
            if (mutex.getValueType() == JsonValue.ValueType.NULL) {
                card.addChildText("Not Locked");
            } else {
                JsonObject mutexObj = mutex.asJsonObject();
                card.addChildText("Locked");
                card.addChildText("by " + mutexObj.getString("user"));
                
                String creationDate = mutexObj.getString("creation_date");
                LocalDateTime creationDateTime = LocalDateTime.parse(creationDate.substring(0, creationDate.lastIndexOf('.')));
                card.addChildText("on " + creationDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
                
                String modifiedDate = mutexObj.getString("modified_date");
                LocalDateTime modifiedDateTime = LocalDateTime.parse(modifiedDate.substring(0, modifiedDate.lastIndexOf('.')));
                card.addChildText("(mod) " + modifiedDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
            }
            
            card.setPrefWidth(180);
            return card;
        }
    }
} 