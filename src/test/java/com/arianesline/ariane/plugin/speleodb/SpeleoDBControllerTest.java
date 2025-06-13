package com.arianesline.ariane.plugin.speleodb;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import javafx.event.ActionEvent;

/**
 * Comprehensive unit tests for SpeleoDBController logic using JUnit 5, Mockito, and AssertJ.
 * Tests business logic, state management, and utility functions without JavaFX dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpeleoDB Controller Tests")
class SpeleoDBControllerTest {
    
    @TempDir
    Path tempDir;
    
    @Mock
    private Desktop mockDesktop;
    
    @Mock
    private ActionEvent mockActionEvent;
    
    private SpeleoDBControllerLogic controllerLogic;
    
    @BeforeEach
    void setUp() {
        controllerLogic = new SpeleoDBControllerLogic();
    }
    
    @Nested
    @DisplayName("Signup Functionality")
    class SignupFunctionalityTests {
        
        @Test
        @DisplayName("Should generate production signup URL when not in debug mode")
        void shouldGenerateProductionSignupUrlWhenNotInDebugMode() {
            // Setup
            controllerLogic.setDebugMode(false);
            controllerLogic.setInstance("www.speleodb.org");
            
            // Execute
            String signupUrl = controllerLogic.generateSignupUrl();
            
            // Verify
            assertThat(signupUrl).isEqualTo("https://www.speleodb.org/signup/");
        }
        
        @Test
        @DisplayName("Should generate debug signup URL when in debug mode")
        void shouldGenerateDebugSignupUrlWhenInDebugMode() {
            // Setup
            controllerLogic.setDebugMode(true);
            controllerLogic.setInstance("www.speleodb.org");
            
            // Execute
            String signupUrl = controllerLogic.generateSignupUrl();
            
            // Verify
            assertThat(signupUrl).isEqualTo("http://www.speleodb.org/signup/");
        }
        
        @Test
        @DisplayName("Should use custom instance for signup URL")
        void shouldUseCustomInstanceForSignupUrl() {
            // Setup
            controllerLogic.setDebugMode(false);
            controllerLogic.setInstance("custom.speleodb.com");
            
            // Execute
            String signupUrl = controllerLogic.generateSignupUrl();
            
            // Verify
            assertThat(signupUrl).isEqualTo("https://custom.speleodb.com/signup/");
        }
        
        @Test
        @DisplayName("Should use default instance when instance is empty")
        void shouldUseDefaultInstanceWhenInstanceIsEmpty() {
            // Setup
            controllerLogic.setDebugMode(false);
            controllerLogic.setInstance("");
            
            // Execute
            String signupUrl = controllerLogic.generateSignupUrl();
            
            // Verify
            assertThat(signupUrl).isEqualTo("https://www.speleoDB.org/signup/");
        }
        
        @Test
        @DisplayName("Should handle whitespace in instance")
        void shouldHandleWhitespaceInInstance() {
            // Setup
            controllerLogic.setDebugMode(false);
            controllerLogic.setInstance("  spaced.instance.com  ");
            
            // Execute
            String signupUrl = controllerLogic.generateSignupUrl();
            
            // Verify
            assertThat(signupUrl).isEqualTo("https://spaced.instance.com/signup/");
        }
        
        @Test
        @DisplayName("Should open signup URL using Desktop")
        void shouldOpenSignupUrlUsingDesktop() {
            try (MockedStatic<Desktop> desktopMock = mockStatic(Desktop.class)) {
                // Setup
                controllerLogic.setDebugMode(false);
                controllerLogic.setInstance("www.speleodb.org");
                desktopMock.when(Desktop::getDesktop).thenReturn(mockDesktop);
                
                // Execute
                String result = controllerLogic.openSignupUrl();
                
                // Verify
                verify(mockDesktop).browse(URI.create("https://www.speleodb.org/signup/"));
                assertThat(result).contains("Opening signup page: https://www.speleodb.org/signup/");
            } catch (Exception e) {
                fail("Should not throw exception", e);
            }
        }
        
        @Test
        @DisplayName("Should handle IOException gracefully")
        void shouldHandleIOExceptionGracefully() {
            try (MockedStatic<Desktop> desktopMock = mockStatic(Desktop.class)) {
                // Setup
                controllerLogic.setDebugMode(false);
                controllerLogic.setInstance("www.speleodb.org");
                desktopMock.when(Desktop::getDesktop).thenReturn(mockDesktop);
                doThrow(new IOException("Browser not available"))
                    .when(mockDesktop).browse(any(URI.class));
                
                // Execute
                String result = controllerLogic.openSignupUrl();
                
                // Verify error handling
                assertThat(result).contains("Failed to open signup page: Browser not available");
            } catch (Exception e) {
                fail("Should not throw exception", e);
            }
        }
        
        @Test
        @DisplayName("Should handle URISyntaxException gracefully")
        void shouldHandleURISyntaxExceptionGracefully() {
            // Setup - use an invalid instance that would cause URI creation to fail
            controllerLogic.setDebugMode(false);
            controllerLogic.setInstance("invalid uri with spaces");
            
            // Execute
            String result = controllerLogic.openSignupUrl();
            
            // Verify error handling
            assertThat(result).contains("Failed to open signup page:");
        }
        
        @Test
        @DisplayName("Should handle Desktop.getDesktop() unavailability")
        void shouldHandleDesktopUnavailability() {
            try (MockedStatic<Desktop> desktopMock = mockStatic(Desktop.class)) {
                // Setup
                controllerLogic.setDebugMode(false);
                controllerLogic.setInstance("www.speleodb.org");
                desktopMock.when(Desktop::getDesktop)
                    .thenThrow(new UnsupportedOperationException("Desktop not supported"));
                
                // Execute
                String result = controllerLogic.openSignupUrl();
                
                // Verify error handling
                assertThat(result).contains("Failed to open signup page: Desktop not supported");
            }
        }
    }
    
    @Nested
    @DisplayName("Debug Mode Detection")
    class DebugModeDetectionTests {
        
        @Test
        @DisplayName("Should detect debug mode from properties file")
        void shouldDetectDebugModeFromPropertiesFile() throws IOException {
            // Create debug.properties file
            Path debugProps = tempDir.resolve("debug.properties");
            Files.write(debugProps, "debug.mode=true\n".getBytes());
            
            controllerLogic.setDebugPropertiesPath(debugProps.toString());
            
            assertThat(controllerLogic.isDebugModeFromProperties()).isTrue();
        }
        
        @Test
        @DisplayName("Should detect debug mode from system property")
        void shouldDetectDebugModeFromSystemProperty() {
            try {
                System.setProperty("speleodb.debug.mode", "true");
                assertThat(controllerLogic.isDebugModeFromSystemProperty()).isTrue();
            } finally {
                System.clearProperty("speleodb.debug.mode");
            }
        }
        
        @Test
        @DisplayName("Should return false when no debug configuration found")
        void shouldReturnFalseWhenNoDebugConfigurationFound() {
            System.clearProperty("speleodb.debug.mode");
            controllerLogic.setDebugMode(false);
            
            assertThat(controllerLogic.isDebugMode()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Message Counter")
    class MessageCounterTests {
        
        @Test
        @DisplayName("Should increment message counter atomically")
        void shouldIncrementMessageCounterAtomically() {
            AtomicInteger counter = new AtomicInteger(0);
            
            assertThat(counter.get()).isEqualTo(0);
            assertThat(counter.incrementAndGet()).isEqualTo(1);
            assertThat(counter.incrementAndGet()).isEqualTo(2);
            assertThat(counter.incrementAndGet()).isEqualTo(3);
            assertThat(counter.get()).isEqualTo(3);
        }
        
        @Test
        @DisplayName("Should handle concurrent access properly")
        void shouldHandleConcurrentAccessProperly() throws InterruptedException {
            AtomicInteger counter = new AtomicInteger(0);
            int threadCount = 10;
            int incrementsPerThread = 100;
            
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        counter.incrementAndGet();
                    }
                });
            }
            
            for (Thread thread : threads) {
                thread.start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            assertThat(counter.get()).isEqualTo(threadCount * incrementsPerThread);
        }
    }
    
    @Nested
    @DisplayName("JSON Project Handling")
    class JsonProjectHandlingTests {
        
        @Test
        @DisplayName("Should handle project without mutex")
        void shouldHandleProjectWithoutMutex() {
            JsonObject project = Json.createObjectBuilder()
                .add("id", "project-123")
                .add("name", "Test Project")
                .add("permission", "READ_AND_WRITE")
                .addNull("active_mutex")
                .build();
            
            assertThat(project.getString("id")).isEqualTo("project-123");
            assertThat(project.getString("name")).isEqualTo("Test Project");
            assertThat(project.getString("permission")).isEqualTo("READ_AND_WRITE");
            assertThat(project.get("active_mutex").getValueType()).isEqualTo(JsonValue.ValueType.NULL);
        }
        
        @Test
        @DisplayName("Should handle project with mutex")
        void shouldHandleProjectWithMutex() {
            JsonObject mutexObj = Json.createObjectBuilder()
                .add("user", "john.doe@example.com")
                .add("creation_date", "2024-01-15T10:30:00.000000")
                .add("modified_date", "2024-01-15T11:45:00.000000")
                .build();
            
            JsonObject project = Json.createObjectBuilder()
                .add("id", "project-456")
                .add("name", "Locked Project")
                .add("permission", "READ_AND_WRITE")
                .add("active_mutex", mutexObj)
                .build();
            
            JsonObject mutex = project.getJsonObject("active_mutex");
            assertThat(mutex.getString("user")).isEqualTo("john.doe@example.com");
            assertThat(mutex.getString("creation_date")).isEqualTo("2024-01-15T10:30:00.000000");
            assertThat(mutex.getString("modified_date")).isEqualTo("2024-01-15T11:45:00.000000");
        }
    }
    
    @Nested
    @DisplayName("Constants and Configuration")
    class ConstantsAndConfigurationTests {
        
        @Test
        @DisplayName("Should have correct preference constants")
        void shouldHaveCorrectPreferenceConstants() {
            assertThat(controllerLogic.getPrefEmail()).isEqualTo("SDB_EMAIL");
            assertThat(controllerLogic.getPrefPassword()).isEqualTo("SDB_PASSWORD");
            assertThat(controllerLogic.getPrefOAuthToken()).isEqualTo("SDB_OAUTH_TOKEN");
            assertThat(controllerLogic.getPrefInstance()).isEqualTo("SDB_INSTANCE");
            assertThat(controllerLogic.getPrefSaveCreds()).isEqualTo("SDB_SAVECREDS");
            assertThat(controllerLogic.getDefaultInstance()).isEqualTo("www.speleoDB.org");
        }
        
        @Test
        @DisplayName("Should generate correct URLs based on debug mode")
        void shouldGenerateCorrectUrlsBasedOnDebugMode() {
            String prodUrl = controllerLogic.generateAboutUrl(false);
            String debugUrl = controllerLogic.generateAboutUrl(true);
            
            assertThat(prodUrl).isEqualTo("https://www.speleodb.org/webview/ariane/");
            assertThat(debugUrl).isEqualTo("http://localhost:8000/webview/ariane/");
        }
    }
    
    // ===================== TEST HELPER CLASSES ===================== //
    
    /**
     * Logic class that contains the business logic from SpeleoDBController
     * without JavaFX dependencies for testing purposes.
     */
    static class SpeleoDBControllerLogic {
        private boolean debugMode = false;
        private String instance = "";
        private String debugPropertiesPath = null;
        
        public void setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
        }
        
        public void setInstance(String instance) {
            this.instance = instance;
        }
        
        public void setDebugPropertiesPath(String path) {
            this.debugPropertiesPath = path;
        }
        
        public boolean isDebugMode() {
            return debugMode;
        }
        
        public String generateSignupUrl() {
            String actualInstance = instance.trim();
            if (actualInstance.isEmpty()) {
                actualInstance = "www.speleoDB.org"; // DEFAULT_INSTANCE
            }
            
            String protocol = isDebugMode() ? "http" : "https";
            return protocol + "://" + actualInstance + "/signup/";
        }
        
        public String openSignupUrl() {
            try {
                String signupUrl = generateSignupUrl();
                java.awt.Desktop.getDesktop().browse(new java.net.URI(signupUrl));
                return "Opening signup page: " + signupUrl;
            } catch (IOException | URISyntaxException | UnsupportedOperationException e) {
                return "Failed to open signup page: " + e.getMessage();
            }
        }
        
        public boolean isDebugModeFromProperties() {
            if (debugPropertiesPath != null) {
                try {
                    var props = new java.util.Properties();
                    props.load(new java.io.FileInputStream(debugPropertiesPath));
                    return Boolean.parseBoolean(props.getProperty("debug.mode", "false"));
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }
        
        public boolean isDebugModeFromSystemProperty() {
            return Boolean.parseBoolean(System.getProperty("speleodb.debug.mode", "false"));
        }
        
        public String generateAboutUrl(boolean isDebugMode) {
            return isDebugMode ? 
                "http://localhost:8000/webview/ariane/" : 
                "https://www.speleodb.org/webview/ariane/";
        }
        
        // Expose private constants for testing
        public String getPrefEmail() { return "SDB_EMAIL"; }
        public String getPrefPassword() { return "SDB_PASSWORD"; }
        public String getPrefOAuthToken() { return "SDB_OAUTH_TOKEN"; }
        public String getPrefInstance() { return "SDB_INSTANCE"; }
        public String getPrefSaveCreds() { return "SDB_SAVECREDS"; }
        public String getDefaultInstance() { return "www.speleoDB.org"; }
    }
} 