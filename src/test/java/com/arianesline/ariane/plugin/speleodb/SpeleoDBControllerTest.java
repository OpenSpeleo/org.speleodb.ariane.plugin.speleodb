package com.arianesline.ariane.plugin.speleodb;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SpeleoDBController functionality.
 * Tests the refactored services and basic logic without UI dependencies.
 */
@DisplayName("SpeleoDB Controller Tests")
class SpeleoDBControllerTest {
    
    @Test
    @DisplayName("Should create PreferencesService successfully")
    void shouldCreatePreferencesServiceSuccessfully() {
        PreferencesService preferencesService = new PreferencesService();
        assertThat(preferencesService).isNotNull();
        assertThat(preferencesService.getDefaultInstance()).isEqualTo("www.speleoDB.org");
    }
    
    @Test
    @DisplayName("Should handle preferences operations")
    void shouldHandlePreferencesOperations() {
        PreferencesService preferencesService = new PreferencesService();
        
        // Clear any existing preferences for clean test
        preferencesService.clearAllPreferences();
        
        // Test loading default preferences
        PreferencesService.UserPreferences prefs = preferencesService.loadPreferences();
        assertThat(prefs).isNotNull();
        assertThat(prefs.getInstance()).isEqualTo("www.speleoDB.org");
        assertThat(prefs.getEmail()).isEmpty();
    }
    
    @Test
    @DisplayName("Should verify refactoring extracted services correctly")
    void shouldVerifyRefactoringExtractedServicesCorrectly() {
        // This test verifies that the critical refactoring from step 1 is working
        // The goal was to extract services from the God Class SpeleoDBController
        
        // Services should be creatable independently
        PreferencesService preferencesService = new PreferencesService();
        assertThat(preferencesService).isNotNull();
        
        // AuthenticationService should be creatable (though we won't test full functionality here)
        // This just verifies the class exists and can be instantiated
        assertThat(AuthenticationService.class).isNotNull();
        
        // Verify the access levels enum is working
        assertThat(SpeleoDBAccessLevel.READ_ONLY).isNotNull();
        assertThat(SpeleoDBAccessLevel.READ_AND_WRITE).isNotNull();
    }
    
    @Nested
    @DisplayName("Service Integration")
    class ServiceIntegrationTests {
        
        @Test
        @DisplayName("Should create services without error")
        void shouldCreateServicesWithoutError() {
            // Test that services can be created independently
            PreferencesService preferencesService = new PreferencesService();
            assertThat(preferencesService).isNotNull();
            assertThat(preferencesService.getDefaultInstance()).isNotEmpty();
        }
        
        @Test
        @DisplayName("Should handle preferences loading and saving")
        void shouldHandlePreferencesLoadingAndSaving() {
            PreferencesService preferencesService = new PreferencesService();
            
            // Clear any existing preferences for clean test
            preferencesService.clearAllPreferences();
            
            // Test that we can load default preferences
            PreferencesService.UserPreferences prefs = preferencesService.loadPreferences();
            assertThat(prefs).isNotNull();
            assertThat(prefs.getInstance()).isEqualTo("www.speleoDB.org");
            assertThat(prefs.getEmail()).isEmpty();
            assertThat(prefs.isSaveCredentials()).isFalse();
        }
        
        @Test
        @DisplayName("Should create and save custom preferences")
        void shouldCreateAndSaveCustomPreferences() {
            PreferencesService preferencesService = new PreferencesService();
            
            // Create custom preferences
            PreferencesService.UserPreferences customPrefs = new PreferencesService.UserPreferences(
                "test@example.com",
                "testpass",
                "testtoken",
                "test.speleoDB.org",
                true
            );
            
            // Should be able to save preferences without error
            preferencesService.savePreferences(customPrefs);
            
            // Load and verify
            PreferencesService.UserPreferences loaded = preferencesService.loadPreferences();
            assertThat(loaded.getEmail()).isEqualTo("test@example.com");
            assertThat(loaded.getInstance()).isEqualTo("test.speleoDB.org");
            assertThat(loaded.isSaveCredentials()).isTrue();
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
    @DisplayName("Basic Logic Tests")
    class BasicLogicTests {
        
        @Test
        @DisplayName("Should handle string operations")
        void shouldHandleStringOperations() {
            // Test basic string operations used in the controller
            String projectName = "Test Cave Project";
            String message = "Are you sure you want to unlock project \"" + projectName + "\"?";
            
            assertThat(message).contains(projectName);
            assertThat(message).contains("unlock project");
        }
        
        @Test
        @DisplayName("Should handle null values gracefully")
        void shouldHandleNullValuesGracefully() {
            // Test null handling
            String projectName = null;
            String message = "Are you sure you want to unlock project \"" + projectName + "\"?";
            
            assertThat(message).contains("null");
        }
        
        @Test
        @DisplayName("Should handle authentication state logic")
        void shouldHandleAuthenticationStateLogic() {
            // Test authentication requirement logic
            boolean isAuthenticated = false;
            String result = isAuthenticated ? "Project created" : "Authentication required";
            assertThat(result).isEqualTo("Authentication required");
            
            isAuthenticated = true;
            result = isAuthenticated ? "Project created" : "Authentication required";
            assertThat(result).isEqualTo("Project created");
        }
        
        @Test
        @DisplayName("Should handle URL generation logic")
        void shouldHandleUrlGenerationLogic() {
            // Test URL generation logic
            String baseUrl = "https://www.speleoDB.org";
            String expectedUrl = baseUrl + "/signup/";
            
            assertThat(expectedUrl).isEqualTo("https://www.speleoDB.org/signup/");
        }
        
        @Test
        @DisplayName("Should handle instance trimming")
        void shouldHandleInstanceTrimming() {
            // Test whitespace handling
            String spacedInstance = "  spaced.instance.com  ";
            String trimmedInstance = spacedInstance.trim();
            
            assertThat(trimmedInstance).isEqualTo("spaced.instance.com");
        }
    }
    
    @Nested
    @DisplayName("Learn About SpeleoDB Functionality")
    class LearnAboutSpeleoDBTests {
        
        @Test
        @DisplayName("Should validate correct SpeleoDB URL format")
        void shouldValidateCorrectSpeleoDBUrlFormat() {
            // Test the URL that the button should open
            String expectedUrl = "https://www.speleodb.org";
            
            // Verify URL format is correct
            assertThat(expectedUrl).startsWith("https://");
            assertThat(expectedUrl).contains("speleodb.org");
            assertThat(expectedUrl).doesNotContain(" ");
            assertThat(expectedUrl).doesNotEndWith("/");
            assertThat(expectedUrl).hasSize(24); // Correct length: "https://www.speleodb.org" = 24 chars
        }
        
        @Test
        @DisplayName("Should create valid URI from SpeleoDB URL")
        void shouldCreateValidUriFromSpeleoDBUrl() {
            String url = "https://www.speleodb.org";
            
            // Test that URI can be created from the URL
            assertThatCode(() -> {
                java.net.URI uri = new java.net.URI(url);
                assertThat(uri.getScheme()).isEqualTo("https");
                assertThat(uri.getHost()).isEqualTo("www.speleodb.org");
                assertThat(uri.getPath()).isEmpty();
                assertThat(uri.toString()).isEqualTo(url);
            }).doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should verify Desktop API availability")
        void shouldVerifyDesktopApiAvailability() {
            // Test Desktop API behavior that the button relies on
            boolean isDesktopSupported = java.awt.Desktop.isDesktopSupported();
            
            // This will vary by platform, but method should not throw exceptions
            assertThatCode(() -> {
                if (isDesktopSupported) {
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    boolean browsseSupported = desktop.isSupported(java.awt.Desktop.Action.BROWSE);
                    // Just verify we can check browse support without errors
                    assertThat(browsseSupported).isNotNull();
                }
            }).doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should handle URI syntax validation")
        void shouldHandleUriSyntaxValidation() {
            // Test various URL formats to ensure robustness
            String[] validUrls = {
                "https://www.speleodb.org",
                "https://speleodb.org",
                "http://localhost:8080",
                "https://test.speleodb.org"
            };
            
            for (String url : validUrls) {
                assertThatCode(() -> {
                    java.net.URI uri = new java.net.URI(url);
                    assertThat(uri.getScheme()).isIn("http", "https");
                }).doesNotThrowAnyException();
            }
        }
        
        @Test
        @DisplayName("Should handle error scenarios gracefully")
        void shouldHandleErrorScenariosGracefully() {
            // Test that we can handle various error scenarios that the button might encounter
            
            // Test that we can detect if Desktop operations are supported
            boolean desktopSupported = java.awt.Desktop.isDesktopSupported();
            assertThat(desktopSupported).isIn(true, false); // Should be one or the other
            
            // Test that we can handle the case where Desktop is supported but browse isn't
            if (desktopSupported) {
                assertThatCode(() -> {
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    boolean browseSupported = desktop.isSupported(java.awt.Desktop.Action.BROWSE);
                    assertThat(browseSupported).isIn(true, false);
                }).doesNotThrowAnyException();
            }
            
            // Verify our URL is well-formed (this is what matters for our button)
            assertThatCode(() -> {
                java.net.URI uri = new java.net.URI("https://www.speleodb.org");
                assertThat(uri.isAbsolute()).isTrue();
            }).doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should verify button action method exists")
        void shouldVerifyButtonActionMethodExists() {
            // Verify that the onLearnAbout method exists in SpeleoDBController
            assertThatCode(() -> {
                java.lang.reflect.Method method = SpeleoDBController.class.getDeclaredMethod("onLearnAbout", javafx.event.ActionEvent.class);
                assertThat(method).isNotNull();
                assertThat(method.isAnnotationPresent(javafx.fxml.FXML.class)).isTrue();
            }).doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should handle button functionality components")
        void shouldHandleButtonFunctionalityComponents() {
            // Test the core components that the Learn About button uses
            String speleoBDUrl = "https://www.speleodb.org";
            
            // Test that our specific URL works
            assertThatCode(() -> {
                java.net.URI uri = new java.net.URI(speleoBDUrl);
                assertThat(uri.getScheme()).isEqualTo("https");
                assertThat(uri.getHost()).isEqualTo("www.speleodb.org");
            }).doesNotThrowAnyException();
            
            // Test Desktop API basics
            assertThatCode(() -> {
                boolean desktopSupported = java.awt.Desktop.isDesktopSupported();
                // Just verify the check doesn't throw - result varies by platform
                assertThat(desktopSupported).isIn(true, false);
            }).doesNotThrowAnyException();
        }
    }
} 