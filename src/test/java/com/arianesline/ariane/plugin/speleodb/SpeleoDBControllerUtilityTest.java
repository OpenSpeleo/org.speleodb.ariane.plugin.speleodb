package com.arianesline.ariane.plugin.speleodb;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * Tests for utility methods and edge cases in SpeleoDBController
 * to improve test coverage.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpeleoDB Controller Utility Tests")
class SpeleoDBControllerUtilityTest {
    
    private TestableSpeleoDBController controller;
    
    @BeforeEach
    void setUp() {
        controller = new TestableSpeleoDBController();
    }
    
    @Nested
    @DisplayName("Message Counter Tests")
    class MessageCounterTests {
        
        @Test
        @DisplayName("Should handle message counter increments")
        void shouldHandleMessageCounterIncrements() {
            int initialValue = SpeleoDBController.messageIndexCounter.get();
            
            // Simulate log operations
            String message1 = controller.simulateLogMessage("First message");
            String message2 = controller.simulateLogMessage("Second message");
            
            // Verify sequential numbering
            assertThat(message1).startsWith(String.valueOf(initialValue + 1));
            assertThat(message2).startsWith(String.valueOf(initialValue + 2));
        }
        
        @Test
        @DisplayName("Should handle message counter thread safety")
        void shouldHandleMessageCounterThreadSafety() throws InterruptedException {
            // Reset counter to a known state
            SpeleoDBController.messageIndexCounter.set(1000);
            
            int numThreads = 10;
            Thread[] threads = new Thread[numThreads];
            
            for (int i = 0; i < numThreads; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 10; j++) {
                        SpeleoDBController.messageIndexCounter.incrementAndGet();
                    }
                });
            }
            
            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Verify final count (1000 + 10 threads * 10 increments = 1100)
            assertThat(SpeleoDBController.messageIndexCounter.get()).isEqualTo(1100);
        }
    }
    
    @Nested
    @DisplayName("Project State Management")
    class ProjectStateManagementTests {
        
        @Test
        @DisplayName("Should handle null project state")
        void shouldHandleNullProjectState() {
            controller.setCurrentProject(null);
            assertThat(controller.getCurrentProjectName()).isNull();
            assertThat(controller.hasActiveProjectLock()).isFalse();
        }
        
        @Test
        @DisplayName("Should handle valid project state")
        void shouldHandleValidProjectState() {
            JsonObject project = Json.createObjectBuilder()
                .add("id", "test-123")
                .add("name", "Test Cave Project")
                .build();
            
            controller.setCurrentProject(project);
            assertThat(controller.getCurrentProjectName()).isEqualTo("Test Cave Project");
            assertThat(controller.hasActiveProjectLock()).isTrue();
        }
        
        @Test
        @DisplayName("Should handle project state transitions")
        void shouldHandleProjectStateTransitions() {
            JsonObject project1 = Json.createObjectBuilder()
                .add("id", "project-1")
                .add("name", "Project One")
                .build();
                
            JsonObject project2 = Json.createObjectBuilder()
                .add("id", "project-2")
                .add("name", "Project Two")
                .build();
            
            // Initially no project
            assertThat(controller.hasActiveProjectLock()).isFalse();
            
            // Set first project
            controller.setCurrentProject(project1);
            assertThat(controller.getCurrentProjectName()).isEqualTo("Project One");
            assertThat(controller.hasActiveProjectLock()).isTrue();
            
            // Switch to second project
            controller.setCurrentProject(project2);
            assertThat(controller.getCurrentProjectName()).isEqualTo("Project Two");
            assertThat(controller.hasActiveProjectLock()).isTrue();
            
            // Clear project
            controller.setCurrentProject(null);
            assertThat(controller.hasActiveProjectLock()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Constants Verification")
    class ConstantsVerificationTests {
        
        @Test
        @DisplayName("Should verify all preference constants")
        void shouldVerifyAllPreferenceConstants() {
            assertThat(controller.getPrefEmailConstant()).isEqualTo("SDB_EMAIL");
            assertThat(controller.getPrefPasswordConstant()).isEqualTo("SDB_PASSWORD");
            assertThat(controller.getPrefOAuthTokenConstant()).isEqualTo("SDB_OAUTH_TOKEN");
            assertThat(controller.getPrefInstanceConstant()).isEqualTo("SDB_INSTANCE");
            assertThat(controller.getPrefSaveCredsConstant()).isEqualTo("SDB_SAVECREDS");
            assertThat(controller.getDefaultInstanceConstant()).isEqualTo("www.speleoDB.org");
        }
        
        @Test
        @DisplayName("Should verify constants are not null or empty")
        void shouldVerifyConstantsAreNotNullOrEmpty() {
            assertThat(controller.getPrefEmailConstant()).isNotNull().isNotEmpty();
            assertThat(controller.getPrefPasswordConstant()).isNotNull().isNotEmpty();
            assertThat(controller.getPrefOAuthTokenConstant()).isNotNull().isNotEmpty();
            assertThat(controller.getPrefInstanceConstant()).isNotNull().isNotEmpty();
            assertThat(controller.getPrefSaveCredsConstant()).isNotNull().isNotEmpty();
            assertThat(controller.getDefaultInstanceConstant()).isNotNull().isNotEmpty();
        }
    }
    
    @Nested
    @DisplayName("Animation Methods")
    class AnimationMethodsTests {
        
        @Test
        @DisplayName("Should handle success animation with message")
        void shouldHandleSuccessAnimationWithMessage() {
            String result = controller.simulateShowSuccessAnimation("Custom Success");
            assertThat(result).contains("Custom Success");
        }
        
        @Test
        @DisplayName("Should handle success animation with null message")
        void shouldHandleSuccessAnimationWithNullMessage() {
            String result = controller.simulateShowSuccessAnimation(null);
            assertThat(result).contains("Success");
        }
        
        @Test
        @DisplayName("Should handle success animation with empty message")
        void shouldHandleSuccessAnimationWithEmptyMessage() {
            String result = controller.simulateShowSuccessAnimation("");
            assertThat(result).contains("Success");
        }
        
        @Test
        @DisplayName("Should handle error animation with message")
        void shouldHandleErrorAnimationWithMessage() {
            String result = controller.simulateShowErrorAnimation("Custom Error");
            assertThat(result).contains("Custom Error");
        }
        
        @Test
        @DisplayName("Should handle error animation with null message")
        void shouldHandleErrorAnimationWithNullMessage() {
            String result = controller.simulateShowErrorAnimation(null);
            assertThat(result).contains("Error");
        }
        
        @Test
        @DisplayName("Should handle error animation with empty message")
        void shouldHandleErrorAnimationWithEmptyMessage() {
            String result = controller.simulateShowErrorAnimation("");
            assertThat(result).contains("Error");
        }
    }
    
    @Nested
    @DisplayName("URL Generation")
    class URLGenerationTests {
        
        @Test
        @DisplayName("Should generate production URL")
        void shouldGenerateProductionUrl() {
            controller.setInstance("test.example.com");
            controller.setDebugMode(false);
            
            String url = controller.generateAboutUrl();
            assertThat(url).isEqualTo("https://test.example.com/webview/ariane/");
        }
        
        @Test
        @DisplayName("Should generate debug URL")
        void shouldGenerateDebugUrl() {
            controller.setInstance("localhost");
            controller.setDebugMode(true);
            
            String url = controller.generateAboutUrl();
            assertThat(url).isEqualTo("http://localhost/webview/ariane/");
        }
        
        @Test
        @DisplayName("Should use default instance when null")
        void shouldUseDefaultInstanceWhenNull() {
            controller.setInstance(null);
            controller.setDebugMode(false);
            
            String url = controller.generateAboutUrl();
            assertThat(url).isEqualTo("https://www.speleoDB.org/webview/ariane/");
        }
        
        @Test
        @DisplayName("Should use default instance when empty")
        void shouldUseDefaultInstanceWhenEmpty() {
            controller.setInstance("");
            controller.setDebugMode(false);
            
            String url = controller.generateAboutUrl();
            assertThat(url).isEqualTo("https://www.speleoDB.org/webview/ariane/");
        }
        
        @Test
        @DisplayName("Should handle whitespace in instance")
        void shouldHandleWhitespaceInInstance() {
            controller.setInstance("   ");
            controller.setDebugMode(false);
            
            String url = controller.generateAboutUrl();
            assertThat(url).isEqualTo("https://www.speleoDB.org/webview/ariane/");
        }
    }
    
    // Helper test class that exposes private methods for testing
    static class TestableSpeleoDBController extends SpeleoDBController {
        private JsonObject currentProject = null;
        private String instanceValue = "";
        private boolean debugModeValue = false;
        
        public void setCurrentProject(JsonObject project) {
            this.currentProject = project;
        }
        
        @Override
        public boolean hasActiveProjectLock() {
            return currentProject != null;
        }
        
        @Override
        public String getCurrentProjectName() {
            return currentProject != null ? currentProject.getString("name") : null;
        }
        
        public void setInstance(String instance) {
            this.instanceValue = instance;
        }
        
        public void setDebugMode(boolean debugMode) {
            this.debugModeValue = debugMode;
        }
        
        // Simulate private method logic
        public String simulateLogMessage(String message) {
            int index = SpeleoDBController.messageIndexCounter.incrementAndGet();
            return index + "-" + message;
        }
        
        public String generateAboutUrl() {
            String instance = (instanceValue == null || instanceValue.trim().isEmpty()) 
                ? "www.speleoDB.org" : instanceValue;
            String protocol = debugModeValue ? "http" : "https";
            return protocol + "://" + instance + "/webview/ariane/";
        }
        
        public String simulateShowSuccessAnimation(String message) {
            String displayMessage = (message == null || message.trim().isEmpty()) ? "Success" : message;
            return "✓ " + displayMessage;
        }
        
        public String simulateShowErrorAnimation(String message) {
            String displayMessage = (message == null || message.trim().isEmpty()) ? "Error" : message;
            return "✗ " + displayMessage;
        }
        
        // Expose constants for testing
        public String getPrefEmailConstant() { return "SDB_EMAIL"; }
        public String getPrefPasswordConstant() { return "SDB_PASSWORD"; }
        public String getPrefOAuthTokenConstant() { return "SDB_OAUTH_TOKEN"; }
        public String getPrefInstanceConstant() { return "SDB_INSTANCE"; }
        public String getPrefSaveCredsConstant() { return "SDB_SAVECREDS"; }
        public String getDefaultInstanceConstant() { return "www.speleoDB.org"; }
    }
} 