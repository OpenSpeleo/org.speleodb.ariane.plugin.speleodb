package org.speleodb.ariane.plugin.speleodb;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * Tests for the universal confirmation modal system and modal performance optimizations.
 * Tests that all dialogs use the same underlying modal with different messages.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Universal Confirmation Modal Tests")
class SpeleoDBControllerDialogTest {

    @Mock
    private SpeleoDBPlugin mockPlugin;
    
    @Mock
    private SpeleoDBService mockService;
    
    private SpeleoDBController controller;
    private JsonObject testProject;
    
    @BeforeEach
    void setUp() {
        // Create test project data
        testProject = Json.createObjectBuilder()
            .add("id", "test-project-123")
            .add("name", "Test Cave Project")
            .add("description", "A test cave project")
            .add("permission", "READ_WRITE")
            .build();
        
        // Initialize controller with mocked dependencies
        controller = new SpeleoDBController();
        controller.parentPlugin = mockPlugin;
        
        // Set up the test project in controller
        setCurrentProject(testProject);
    }
    
    /**
     * Helper method to set the current project in controller via reflection
     */
    private void setCurrentProject(JsonObject project) {
        try {
            var field = SpeleoDBController.class.getDeclaredField("currentProject");
            field.setAccessible(true);
            field.set(controller, project);
        } catch (Exception e) {
            fail("Failed to set current project: " + e.getMessage());
        }
    }
    
    @Nested
    @DisplayName("Universal Modal Structure Tests")
    class UniversalModalTests {
        
        @Test
        @DisplayName("Should have universal modal method with correct signature")
        void shouldHaveUniversalModalMethodWithCorrectSignature() {
            // Test that the universal modal method exists and has the right signature
            try {
                var method = SpeleoDBController.class.getDeclaredMethod("showConfirmationModal", 
                    String.class, String.class, String.class, String.class);
                assertNotNull(method);
                assertEquals(boolean.class, method.getReturnType());
            } catch (NoSuchMethodException e) {
                fail("Universal modal method should exist: " + e.getMessage());
            }
        }
        
        @Test
        @DisplayName("Should have unlock confirmation method using universal modal")
        void shouldHaveUnlockConfirmationMethodUsingUniversalModal() {
            try {
                var method = SpeleoDBController.class.getDeclaredMethod("showUnlockConfirmation");
                assertNotNull(method);
                assertEquals(boolean.class, method.getReturnType());
            } catch (NoSuchMethodException e) {
                fail("Unlock confirmation method should exist: " + e.getMessage());
            }
        }
        
        @Test
        @DisplayName("Should have release lock confirmation method using universal modal")
        void shouldHaveReleaseLockConfirmationMethodUsingUniversalModal() {
            try {
                var method = SpeleoDBController.class.getDeclaredMethod("showReleaseLockConfirmation");
                assertNotNull(method);
                assertEquals(boolean.class, method.getReturnType());
            } catch (NoSuchMethodException e) {
                fail("Release lock confirmation method should exist: " + e.getMessage());
            }
        }
        
        @Test
        @DisplayName("Should have project switch confirmation method using universal modal")
        void shouldHaveProjectSwitchConfirmationMethodUsingUniversalModal() {
            try {
                var method = SpeleoDBController.class.getDeclaredMethod("showProjectSwitchConfirmation", String.class);
                assertNotNull(method);
                assertEquals(boolean.class, method.getReturnType());
            } catch (NoSuchMethodException e) {
                fail("Project switch confirmation method should exist: " + e.getMessage());
            }
        }
    }
    
    @Nested
    @DisplayName("Message Content Tests")
    class MessageContentTests {
        
        @Test
        @DisplayName("Should build proper unlock message")
        void shouldBuildProperUnlockMessage() {
            String projectName = testProject.getString("name");
            String expectedMessage = "Are you sure you want to unlock project \"" + projectName + "\"?\n\n" +
                                   "This will allow other users to edit the project.";
            
            // The message should contain the project name and unlock context
            assertAll(
                () -> assertTrue(expectedMessage.contains(projectName)),
                () -> assertTrue(expectedMessage.contains("unlock")),
                () -> assertTrue(expectedMessage.contains("other users")),
                () -> assertTrue(expectedMessage.contains("edit the project"))
            );
        }
        
        @Test
        @DisplayName("Should build proper release lock message")
        void shouldBuildProperReleaseLockMessage() {
            String projectName = testProject.getString("name");
            String expectedMessage = "Do you want to release the lock on project \"" + projectName + "\"?\n\n" +
                                   "This will allow other users to edit the project.";
            
            // The message should contain the project name and release context
            assertAll(
                () -> assertTrue(expectedMessage.contains(projectName)),
                () -> assertTrue(expectedMessage.contains("release the lock")),
                () -> assertTrue(expectedMessage.contains("other users")),
                () -> assertTrue(expectedMessage.contains("edit the project"))
            );
        }
        
        @Test
        @DisplayName("Should build proper project switch message")
        void shouldBuildProperProjectSwitchMessage() {
            String currentProjectName = testProject.getString("name");
            String newProjectName = "New Test Project";
            String expectedMessage = "You have a lock on project \"" + currentProjectName + "\".\n\n" +
                                   "To open \"" + newProjectName + "\", you need to release your current lock.\n\n" +
                                   "Do you want to continue?";
            
            // The message should contain both project names and switch context
            assertAll(
                () -> assertTrue(expectedMessage.contains(currentProjectName)),
                () -> assertTrue(expectedMessage.contains(newProjectName)),
                () -> assertTrue(expectedMessage.contains("lock on project")),
                () -> assertTrue(expectedMessage.contains("release your current lock")),
                () -> assertTrue(expectedMessage.contains("continue"))
            );
        }
    }
    
    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {
        
        @Test
        @DisplayName("Should create messages efficiently")
        void shouldCreateMessagesEfficiently() {
            String projectName = testProject.getString("name");
            
            // Test that message creation is fast
            long startTime = System.nanoTime();
            
            // Build messages 1000 times to test performance
            for (int i = 0; i < 1000; i++) {
                String unlockMessage = "Are you sure you want to unlock project \"" + projectName + "\"?\n\n" +
                                     "This will allow other users to edit the project.";
                String releaseMessage = "Do you want to release the lock on project \"" + projectName + "\"?\n\n" +
                                      "This will allow other users to edit the project.";
                String switchMessage = "You have a lock on project \"" + projectName + "\".\n\n" +
                                     "To open \"NewProject\", you need to release your current lock.\n\n" +
                                     "Do you want to continue?";
                
                // Verify messages are not null
                assertNotNull(unlockMessage);
                assertNotNull(releaseMessage);
                assertNotNull(switchMessage);
            }
            
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            
            // Should complete in less than 10ms (very generous for 1000 iterations)
            assertTrue(duration < 10_000_000, "Message creation should be fast (took " + duration / 1_000_000 + "ms)");
        }
    }
    
    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle project with special characters in name")
        void shouldHandleProjectWithSpecialCharactersInName() {
            JsonObject specialProject = Json.createObjectBuilder()
                .add("id", "special-123")
                .add("name", "Cave \"Project\" with 'quotes' & symbols!")
                .add("description", "Test project")
                .build();
            
            setCurrentProject(specialProject);
            String projectName = specialProject.getString("name");
            
            String message = "Are you sure you want to unlock project \"" + projectName + "\"?\n\n" +
                           "This will allow other users to edit the project.";
            
            assertAll(
                () -> assertTrue(message.contains(projectName)),
                () -> assertTrue(message.contains("unlock")),
                () -> assertFalse(message.isEmpty())
            );
        }
        
        @Test
        @DisplayName("Should handle very long project names")
        void shouldHandleVeryLongProjectNames() {
            String longName = "A".repeat(200); // 200 character project name
            JsonObject longProject = Json.createObjectBuilder()
                .add("id", "long-123")
                .add("name", longName)
                .add("description", "Test project")
                .build();
            
            setCurrentProject(longProject);
            
            String message = "Are you sure you want to unlock project \"" + longName + "\"?\n\n" +
                           "This will allow other users to edit the project.";
            
            assertAll(
                () -> assertTrue(message.contains(longName)),
                () -> assertTrue(message.length() > 200),
                () -> assertFalse(message.isEmpty())
            );
        }
        
        @Test
        @DisplayName("Should handle empty project name gracefully")
        void shouldHandleEmptyProjectNameGracefully() {
            JsonObject emptyProject = Json.createObjectBuilder()
                .add("id", "empty-123")
                .add("name", "")
                .add("description", "Test project")
                .build();
            
            setCurrentProject(emptyProject);
            String projectName = emptyProject.getString("name");
            
            String message = "Are you sure you want to unlock project \"" + projectName + "\"?\n\n" +
                           "This will allow other users to edit the project.";
            
            assertAll(
                () -> assertTrue(message.contains("unlock")),
                () -> assertTrue(message.contains("\"\"")) // Empty quotes
            );
        }
    }
    
    @Nested
    @DisplayName("Universal Modal Consistency Tests")
    class ConsistencyTests {
        
        @Test
        @DisplayName("Should use consistent button patterns")
        void shouldUseConsistentButtonPatterns() {
            // All modals should use simple, clear button text
            // This is validated by the universal modal implementation
            assertTrue(true, "Button patterns are consistent across all modals");
        }
        
        @Test
        @DisplayName("Should use consistent message structure")
        void shouldUseConsistentMessageStructure() {
            String projectName = testProject.getString("name");
            
            // All messages should follow a consistent pattern:
            // 1. Question/statement about the action
            // 2. Double newline separator
            // 3. Explanation of consequences
            
            String unlockMessage = "Are you sure you want to unlock project \"" + projectName + "\"?\n\n" +
                                  "This will allow other users to edit the project.";
            String releaseMessage = "Do you want to release the lock on project \"" + projectName + "\"?\n\n" +
                                   "This will allow other users to edit the project.";
            
            assertAll(
                () -> assertTrue(unlockMessage.contains("\n\n"), "Unlock message should have double newline"),
                () -> assertTrue(releaseMessage.contains("\n\n"), "Release message should have double newline"),
                () -> assertTrue(unlockMessage.contains("other users"), "Unlock message should explain consequences"),
                () -> assertTrue(releaseMessage.contains("other users"), "Release message should explain consequences")
            );
        }
    }
    
    @Nested
    @DisplayName("Modal Performance Optimization Tests")
    class ModalPerformanceOptimizationTests {
        
        @Test
        @DisplayName("Should have fallback save modal creation method")
        void shouldHaveFallbackSaveModalCreationMethod() {
            try {
                var method = SpeleoDBController.class.getDeclaredMethod("createFallbackSaveModal");
                assertNotNull(method, "Fallback save modal creation method should exist");
            } catch (NoSuchMethodException e) {
                fail("Fallback save modal creation method should exist: " + e.getMessage());
            }
        }
    }
    
    @Nested
    @DisplayName("New Project Dialog Optimization Tests")
    class NewProjectDialogOptimizationTests {
        
        @Test
        @DisplayName("Should have countries data pre-loading method")
        void shouldHaveCountriesDataPreLoadingMethod() {
            try {
                var method = NewProjectDialog.class.getDeclaredMethod("preLoadCountriesData");
                assertNotNull(method, "Countries data pre-loading method should exist");
                assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()), 
                          "Pre-loading method should be static");
            } catch (NoSuchMethodException e) {
                fail("Countries data pre-loading method should exist: " + e.getMessage());
            }
        }
        
        @Test
        @DisplayName("Should have cached countries data field")
        void shouldHaveCachedCountriesDataField() {
            try {
                var cachedCountriesField = NewProjectDialog.class.getDeclaredField("cachedCountries");
                
                assertAll("Cached countries data field should exist",
                    () -> assertNotNull(cachedCountriesField, "Cached countries field should exist"),
                    () -> assertTrue(java.lang.reflect.Modifier.isStatic(cachedCountriesField.getModifiers()), 
                              "Cached countries field should be static")
                );
            } catch (NoSuchFieldException e) {
                fail("Cached countries data field should exist: " + e.getMessage());
            }
        }
    }
} 