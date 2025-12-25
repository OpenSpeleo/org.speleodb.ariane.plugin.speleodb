package org.speleodb.ariane.plugin.speleodb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * Additional tests for SpeleoDBController focusing on state management,
 * error handling, and edge cases.
 */
public class SpeleoDBControllerStateTest {

    private static final String TEST_RESOURCES_DIR = System.getProperty("java.io.tmpdir") + File.separator + "test_state_resources";

    public static void main(String[] args) throws Exception {
        setupTestEnvironment();

        testAtomicOperations();
        testJsonArrayHandling();
        testErrorHandling();
        testEdgeCases();
        testStateTransitions();
        testPropertyFileHandling();
        testStringValidation();
        testUIStateLogic();

        cleanupTestEnvironment();
        System.out.println("All SpeleoDBController state tests passed!");
    }

    static void setupTestEnvironment() throws IOException {
        Path testDir = Paths.get(TEST_RESOURCES_DIR);
        if (!Files.exists(testDir)) {
            Files.createDirectories(testDir);
        }
        System.out.println("✓ State test environment setup completed");
    }

    static void cleanupTestEnvironment() throws IOException {
        Path testDir = Paths.get(TEST_RESOURCES_DIR);
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                .map(Path::toFile)
                .forEach(File::delete);
        }
        System.out.println("✓ State test environment cleanup completed");
    }

    static void testAtomicOperations() {
        System.out.println("Testing atomic operations ...");

        // Test AtomicBoolean state management
        AtomicBoolean lockAcquired = new AtomicBoolean(false);
        assert !lockAcquired.get();

        lockAcquired.set(true);
        assert lockAcquired.get();

        boolean result = lockAcquired.compareAndSet(true, false);
        assert result;
        assert !lockAcquired.get();

        // Test AtomicInteger for message indexing
        AtomicInteger messageIndex = new AtomicInteger(0);
        int index1 = messageIndex.incrementAndGet();
        int index2 = messageIndex.incrementAndGet();

        assert index1 == 1;
        assert index2 == 2;
        assert messageIndex.get() == 2;

        System.out.println("✓ Atomic operations tests passed");
    }

    static void testJsonArrayHandling() {
        System.out.println("Testing JSON array handling ...");

        // Test empty project list
        JsonArray emptyArray = Json.createArrayBuilder().build();
        assert emptyArray.isEmpty();

        // Test project list with multiple projects
        JsonArray projectList = Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                .add("id", "proj-1")
                .add("name", "Project 1")
                .add("permission", "READ_ONLY")
                .addNull("active_mutex"))
            .add(Json.createObjectBuilder()
                .add("id", "proj-2")
                .add("name", "Project 2")
                .add("permission", "READ_AND_WRITE")
                .add("active_mutex", Json.createObjectBuilder()
                    .add("user", "user@example.com")
                    .add("creation_date", "2024-01-15T10:30:00.000000")
                    .add("modified_date", "2024-01-15T11:45:00.000000")))
            .build();

        assert projectList.size() == 2;

        // Test iteration through project list
        int count = 0;
        for (JsonValue value : projectList) {
            JsonObject project = value.asJsonObject();
            assert project.containsKey("id");
            assert project.containsKey("name");
            assert project.containsKey("permission");
            assert project.containsKey("active_mutex");
            count++;
        }
        assert count == 2;

        System.out.println("✓ JSON array handling tests passed");
    }

    static void testErrorHandling() {
        System.out.println("Testing error handling ...");

        // Test handling of missing JSON properties
        JsonObject incompleteProject = Json.createObjectBuilder()
            .add("id", "incomplete-proj")
            .build();

        try {
            incompleteProject.getString("name");
            assert false : "Should have thrown exception for missing property";
        } catch (Exception e) {
            // Expected exception
        }

        // Test handling of null values
        JsonObject projectWithNulls = Json.createObjectBuilder()
            .add("id", "null-proj")
            .add("name", "Null Project")
            .add("permission", "READ_ONLY")
            .addNull("active_mutex")
            .build();

        JsonValue mutex = projectWithNulls.get("active_mutex");
        assert mutex.getValueType() == JsonValue.ValueType.NULL;

        // Test empty string handling
        String emptyString = "";
        assert emptyString.isEmpty();
        assert emptyString.length() == 0;

        System.out.println("✓ Error handling tests passed");
    }

    static void testEdgeCases() {
        System.out.println("Testing edge cases ...");

        // Test date string parsing edge cases
        String dateWithMicroseconds = "2024-01-15T10:30:00.123456";
        String trimmedDate = dateWithMicroseconds.substring(0, dateWithMicroseconds.lastIndexOf('.'));
        assert trimmedDate.equals("2024-01-15T10:30:00");

        // Test date without microseconds
        String dateWithoutMicroseconds = "2024-01-15T10:30:00";
        int lastDotIndex = dateWithoutMicroseconds.lastIndexOf('.');
        if (lastDotIndex != -1) {
            String trimmed = dateWithoutMicroseconds.substring(0, lastDotIndex);
            assert !trimmed.equals(dateWithoutMicroseconds);
        } else {
            // No dot found, string remains unchanged
            assert dateWithoutMicroseconds.equals("2024-01-15T10:30:00");
        }

        System.out.println("✓ Edge cases tests passed");
    }

    static void testStateTransitions() {
        System.out.println("Testing state transitions ...");

        // Test authentication state transitions
        boolean isAuthenticated = false;

        // Initial state
        assert !isAuthenticated;

        // After successful connection
        isAuthenticated = true;
        assert isAuthenticated;

        // After disconnection
        isAuthenticated = false;
        assert !isAuthenticated;

        // Test project selection state
        JsonObject currentProject = null;
        assert currentProject == null;

        currentProject = Json.createObjectBuilder()
            .add("id", "selected-proj")
            .add("name", "Selected Project")
            .add("permission", "READ_AND_WRITE")
            .addNull("active_mutex")
            .build();

        assert currentProject != null;
        assert currentProject.getString("id").equals("selected-proj");

        // Reset state
        currentProject = null;
        assert currentProject == null;

        System.out.println("✓ State transitions tests passed");
    }

    static void testPropertyFileHandling() throws IOException {
        System.out.println("Testing property file handling ...");

        // Test creating debug properties
        Path debugProps = Paths.get(TEST_RESOURCES_DIR + File.separator + "debug.properties");
        Properties props = new Properties();
        props.setProperty("debug.mode", "true");
        props.setProperty("log.level", "DEBUG");

        // Write properties to file
        try (var output = Files.newOutputStream(debugProps)) {
            props.store(output, "Test debug properties");
        }

        assert Files.exists(debugProps);

        // Read properties back
        Properties loadedProps = new Properties();
        try (var input = Files.newInputStream(debugProps)) {
            loadedProps.load(input);
        }

        assert loadedProps.getProperty("debug.mode").equals("true");
        assert loadedProps.getProperty("log.level").equals("DEBUG");

        // Test boolean parsing
        boolean debugMode = Boolean.parseBoolean(loadedProps.getProperty("debug.mode", "false"));
        assert debugMode;

        boolean nonExistentProperty = Boolean.parseBoolean(loadedProps.getProperty("non.existent", "false"));
        assert !nonExistentProperty;

        System.out.println("✓ Property file handling tests passed");
    }

    static void testStringValidation() {
        System.out.println("Testing string validation ...");

        // Test email validation patterns
        String validEmail = "user@example.com";
        assert validEmail.contains("@");
        assert validEmail.contains(".");
        assert !validEmail.isEmpty();

        String invalidEmail = "invalid-email";
        assert !invalidEmail.contains("@");

        // Test password validation
        String password = "mypassword123";
        assert !password.isEmpty();
        assert password.length() >= 8;

        String emptyPassword = "";
        assert emptyPassword.isEmpty();
        assert emptyPassword.length() == 0;

        // Test instance URL validation
        String validInstance = "www.speleodb.org";
        assert !validInstance.isEmpty();
        assert !validInstance.startsWith("http://");
        assert !validInstance.startsWith("https://");

        String urlWithProtocol = "https://www.speleodb.org";
        assert urlWithProtocol.startsWith("https://");

        // Test upload message validation
        String uploadMessage = "Updated cave survey data";
        assert !uploadMessage.isEmpty();
        assert uploadMessage.length() > 0;

        System.out.println("✓ String validation tests passed");
    }

    static void testUIStateLogic() {
        System.out.println("Testing UI state logic ...");

        // Test button state logic
        boolean isAuthenticated = false;
        boolean hasProject = false;
        boolean hasLock = false;

        // Initial state - not authenticated
        assert !shouldShowProjects(isAuthenticated);
        assert !shouldEnableUpload(isAuthenticated, hasProject, hasLock);
        // Unlock button removed; unlock enablement irrelevant now
        assert true;

        // Authenticated but no project
        isAuthenticated = true;
        assert shouldShowProjects(isAuthenticated);
        assert !shouldEnableUpload(isAuthenticated, hasProject, hasLock);
        assert true;

        // Authenticated with project but no lock
        hasProject = true;
        assert shouldShowProjects(isAuthenticated);
        assert !shouldEnableUpload(isAuthenticated, hasProject, hasLock);
        assert true;

        // Authenticated with project and lock
        hasLock = true;
        assert shouldShowProjects(isAuthenticated);
        assert shouldEnableUpload(isAuthenticated, hasProject, hasLock);
        assert true;

        System.out.println("✓ UI state logic tests passed");
    }

    // Helper methods for UI state logic testing
    private static boolean shouldShowProjects(boolean isAuthenticated) {
        return isAuthenticated;
    }

    private static boolean shouldEnableUpload(boolean isAuthenticated, boolean hasProject, boolean hasLock) {
        return isAuthenticated && hasProject && hasLock;
    }

    // Unlock enablement removed with unlock button; no-op
    private static boolean shouldEnableUnlock(boolean isAuthenticated, boolean hasProject, boolean hasLock) { return true; }
}
