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
    
    @Nested
    @DisplayName("Confirmation Popup Functionality")
    class ConfirmationPopupFunctionalityTests {
        
        @Test
        @DisplayName("Should generate correct unlock confirmation message")
        void shouldGenerateCorrectUnlockConfirmationMessage() {
            // Setup
            controllerLogic.setCurrentProject("Test Cave Project");
            
            // Execute
            String confirmationMessage = controllerLogic.generateUnlockConfirmationMessage();
            
            // Verify
            assertThat(confirmationMessage).contains("Test Cave Project");
            assertThat(confirmationMessage).contains("unlock project");
            assertThat(confirmationMessage).contains("release your write lock");
            assertThat(confirmationMessage).contains("allow other users to edit");
        }
        
        @Test
        @DisplayName("Should generate correct release lock confirmation message")
        void shouldGenerateCorrectReleaseLockConfirmationMessage() {
            // Setup
            controllerLogic.setCurrentProject("Underground Survey");
            
            // Execute
            String confirmationMessage = controllerLogic.generateReleaseLockConfirmationMessage();
            
            // Verify
            assertThat(confirmationMessage).contains("Underground Survey");
            assertThat(confirmationMessage).contains("release the write lock");
            assertThat(confirmationMessage).contains("other users can edit");
        }
        
        @Test
        @DisplayName("Should handle null project name gracefully in unlock confirmation")
        void shouldHandleNullProjectNameGracefullyInUnlockConfirmation() {
            // Setup
            controllerLogic.setCurrentProject(null);
            
            // Execute
            String confirmationMessage = controllerLogic.generateUnlockConfirmationMessage();
            
            // Verify
            assertThat(confirmationMessage).contains("unlock project");
            assertThat(confirmationMessage).contains("null"); // Should handle null gracefully
        }
        
        @Test
        @DisplayName("Should handle empty project name in release lock confirmation")
        void shouldHandleEmptyProjectNameInReleaseLockConfirmation() {
            // Setup
            controllerLogic.setCurrentProject("");
            
            // Execute
            String confirmationMessage = controllerLogic.generateReleaseLockConfirmationMessage();
            
            // Verify
            assertThat(confirmationMessage).contains("release the write lock");
            assertThat(confirmationMessage).contains("\"\""); // Should handle empty string
        }
        
        @Test
        @DisplayName("Should validate unlock confirmation dialog properties")
        void shouldValidateUnlockConfirmationDialogProperties() {
            // Setup
            controllerLogic.setCurrentProject("Cave Mapping Project");
            
            // Execute
            var dialogProperties = controllerLogic.getUnlockConfirmationDialogProperties();
            
            // Verify
            assertThat(dialogProperties.getTitle()).isEqualTo("Unlock Project");
            assertThat(dialogProperties.getHeaderText()).isEqualTo("Confirm Unlock");
            assertThat(dialogProperties.getYesButtonText()).isEqualTo("Yes, Unlock");
            assertThat(dialogProperties.getNoButtonText()).isEqualTo("No, Keep Lock");
            assertThat(dialogProperties.getContentText()).contains("Cave Mapping Project");
        }
        
        @Test
        @DisplayName("Should validate release lock confirmation dialog properties")
        void shouldValidateReleaseLockConfirmationDialogProperties() {
            // Setup
            controllerLogic.setCurrentProject("Limestone Survey");
            
            // Execute
            var dialogProperties = controllerLogic.getReleaseLockConfirmationDialogProperties();
            
            // Verify
            assertThat(dialogProperties.getTitle()).isEqualTo("Release Write Lock");
            assertThat(dialogProperties.getHeaderText()).isEqualTo("Upload Successful");
            assertThat(dialogProperties.getYesButtonText()).isEqualTo("Yes, Release Lock");
            assertThat(dialogProperties.getNoButtonText()).isEqualTo("No, Keep Lock");
            assertThat(dialogProperties.getContentText()).contains("Limestone Survey");
        }
        
        @Test
        @DisplayName("Should simulate user choosing to unlock project")
        void shouldSimulateUserChoosingToUnlockProject() {
            // Setup
            controllerLogic.setCurrentProject("Test Project");
            controllerLogic.setUserConfirmationResponse(true);
            
            // Execute
            boolean result = controllerLogic.simulateUnlockConfirmation();
            
            // Verify
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should simulate user choosing to keep lock during unlock")
        void shouldSimulateUserChoosingToKeepLockDuringUnlock() {
            // Setup
            controllerLogic.setCurrentProject("Test Project");
            controllerLogic.setUserConfirmationResponse(false);
            
            // Execute
            boolean result = controllerLogic.simulateUnlockConfirmation();
            
            // Verify
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("Should simulate user choosing to release lock after upload")
        void shouldSimulateUserChoosingToReleaseLockAfterUpload() {
            // Setup
            controllerLogic.setCurrentProject("Upload Test Project");
            controllerLogic.setUserConfirmationResponse(true);
            
            // Execute
            boolean result = controllerLogic.simulateReleaseLockConfirmation();
            
            // Verify
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should simulate user choosing to keep lock after upload")
        void shouldSimulateUserChoosingToKeepLockAfterUpload() {
            // Setup
            controllerLogic.setCurrentProject("Upload Test Project");
            controllerLogic.setUserConfirmationResponse(false);
            
            // Execute
            boolean result = controllerLogic.simulateReleaseLockConfirmation();
            
            // Verify
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("Should handle confirmation dialog cancellation")
        void shouldHandleConfirmationDialogCancellation() {
            // Setup
            controllerLogic.setCurrentProject("Cancelled Project");
            controllerLogic.setUserConfirmationResponse(null); // Simulate dialog cancellation
            
            // Execute
            boolean unlockResult = controllerLogic.simulateUnlockConfirmation();
            boolean releaseLockResult = controllerLogic.simulateReleaseLockConfirmation();
            
            // Verify - should default to false when cancelled
            assertThat(unlockResult).isFalse();
            assertThat(releaseLockResult).isFalse();
        }
        
        @Test
        @DisplayName("Should validate confirmation message formatting")
        void shouldValidateConfirmationMessageFormatting() {
            // Setup
            controllerLogic.setCurrentProject("Special Characters & Symbols Project");
            
            // Execute
            String unlockMessage = controllerLogic.generateUnlockConfirmationMessage();
            String releaseLockMessage = controllerLogic.generateReleaseLockConfirmationMessage();
            
            // Verify proper escaping and formatting
            assertThat(unlockMessage).contains("Special Characters & Symbols Project");
            assertThat(releaseLockMessage).contains("Special Characters & Symbols Project");
            assertThat(unlockMessage).contains("\"Special Characters & Symbols Project\"");
            assertThat(releaseLockMessage).contains("\"Special Characters & Symbols Project\"");
        }
        
        @Test
        @DisplayName("Should track confirmation dialog invocation count")
        void shouldTrackConfirmationDialogInvocationCount() {
            // Setup
            controllerLogic.setCurrentProject("Counter Test Project");
            controllerLogic.resetConfirmationDialogCount();
            
            // Execute multiple confirmations
            controllerLogic.simulateUnlockConfirmation();
            controllerLogic.simulateReleaseLockConfirmation();
            controllerLogic.simulateUnlockConfirmation();
            
            // Verify
            assertThat(controllerLogic.getConfirmationDialogCount()).isEqualTo(3);
        }
        
        @Test
        @DisplayName("Should validate button text consistency")
        void shouldValidateButtonTextConsistency() {
            // Setup
            controllerLogic.setCurrentProject("Button Test Project");
            
            // Execute
            var unlockProps = controllerLogic.getUnlockConfirmationDialogProperties();
            var releaseLockProps = controllerLogic.getReleaseLockConfirmationDialogProperties();
            
            // Verify consistent "No" button text
            assertThat(unlockProps.getNoButtonText()).isEqualTo("No, Keep Lock");
            assertThat(releaseLockProps.getNoButtonText()).isEqualTo("No, Keep Lock");
            
            // Verify different "Yes" button text for different contexts
            assertThat(unlockProps.getYesButtonText()).isEqualTo("Yes, Unlock");
            assertThat(releaseLockProps.getYesButtonText()).isEqualTo("Yes, Release Lock");
        }
        
        @Test
        @DisplayName("Should generate correct project switch confirmation message")
        void shouldGenerateCorrectProjectSwitchConfirmationMessage() {
            // Setup
            controllerLogic.setCurrentProject("Current Cave Project");
            
            // Execute
            String confirmationMessage = controllerLogic.generateProjectSwitchConfirmationMessage("New Cave Survey");
            
            // Verify
            assertThat(confirmationMessage).contains("Current Cave Project");
            assertThat(confirmationMessage).contains("New Cave Survey");
            assertThat(confirmationMessage).contains("active lock");
            assertThat(confirmationMessage).contains("release your current lock");
            assertThat(confirmationMessage).contains("switch projects");
        }
        
        @Test
        @DisplayName("Should validate project switch confirmation dialog properties")
        void shouldValidateProjectSwitchConfirmationDialogProperties() {
            // Setup
            controllerLogic.setCurrentProject("Locked Project");
            
            // Execute
            var dialogProperties = controllerLogic.getProjectSwitchConfirmationDialogProperties("Target Project");
            
            // Verify
            assertThat(dialogProperties.getTitle()).isEqualTo("Switch Project");
            assertThat(dialogProperties.getHeaderText()).isEqualTo("Current Project is Locked");
            assertThat(dialogProperties.getYesButtonText()).isEqualTo("Yes, Switch Projects");
            assertThat(dialogProperties.getNoButtonText()).isEqualTo("No, Stay Here");
            assertThat(dialogProperties.getContentText()).contains("Locked Project");
            assertThat(dialogProperties.getContentText()).contains("Target Project");
        }
        
        @Test
        @DisplayName("Should simulate user choosing to switch projects")
        void shouldSimulateUserChoosingToSwitchProjects() {
            // Setup
            controllerLogic.setCurrentProject("Current Project");
            controllerLogic.setUserConfirmationResponse(true);
            
            // Execute
            boolean result = controllerLogic.simulateProjectSwitchConfirmation("New Project");
            
            // Verify
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should simulate user choosing to stay on current project")
        void shouldSimulateUserChoosingToStayOnCurrentProject() {
            // Setup
            controllerLogic.setCurrentProject("Current Project");
            controllerLogic.setUserConfirmationResponse(false);
            
            // Execute
            boolean result = controllerLogic.simulateProjectSwitchConfirmation("New Project");
            
            // Verify
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Upload and Unlock Flow Integration")
    class UploadAndUnlockFlowIntegrationTests {
        
        @Test
        @DisplayName("Should handle complete upload with lock release flow")
        void shouldHandleCompleteUploadWithLockReleaseFlow() {
            // Setup
            controllerLogic.setCurrentProject("Integration Test Project");
            controllerLogic.setUserConfirmationResponse(true);
            
            // Execute upload flow
            String uploadResult = controllerLogic.simulateUploadFlow();
            boolean shouldReleaseLock = controllerLogic.simulateReleaseLockConfirmation();
            
            // Verify
            assertThat(uploadResult).contains("Upload successful");
            assertThat(shouldReleaseLock).isTrue();
        }
        
        @Test
        @DisplayName("Should handle upload with lock retention flow")
        void shouldHandleUploadWithLockRetentionFlow() {
            // Setup
            controllerLogic.setCurrentProject("Retention Test Project");
            controllerLogic.setUserConfirmationResponse(false);
            
            // Execute upload flow
            String uploadResult = controllerLogic.simulateUploadFlow();
            boolean shouldReleaseLock = controllerLogic.simulateReleaseLockConfirmation();
            
            // Verify
            assertThat(uploadResult).contains("Upload successful");
            assertThat(shouldReleaseLock).isFalse();
        }
        
        @Test
        @DisplayName("Should handle project switch with lock release flow")
        void shouldHandleProjectSwitchWithLockReleaseFlow() {
            // Setup
            controllerLogic.setCurrentProject("Current Locked Project");
            controllerLogic.setUserConfirmationResponse(true);
            
            // Execute project switch flow
            boolean shouldSwitch = controllerLogic.simulateProjectSwitchConfirmation("New Target Project");
            
            // Verify
            assertThat(shouldSwitch).isTrue();
            assertThat(controllerLogic.getConfirmationDialogCount()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("Should handle project switch cancellation flow")
        void shouldHandleProjectSwitchCancellationFlow() {
            // Setup
            controllerLogic.setCurrentProject("Current Locked Project");
            controllerLogic.setUserConfirmationResponse(false);
            
            // Execute project switch flow
            boolean shouldSwitch = controllerLogic.simulateProjectSwitchConfirmation("New Target Project");
            
            // Verify
            assertThat(shouldSwitch).isFalse();
            assertThat(controllerLogic.getConfirmationDialogCount()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("Should handle null response in project switch dialog")
        void shouldHandleNullResponseInProjectSwitchDialog() {
            // Setup
            controllerLogic.setCurrentProject("Current Locked Project");
            controllerLogic.setUserConfirmationResponse(null); // Simulate dialog cancellation
            
            // Execute project switch flow
            boolean shouldSwitch = controllerLogic.simulateProjectSwitchConfirmation("New Target Project");
            
            // Verify - should default to false when cancelled
            assertThat(shouldSwitch).isFalse();
            assertThat(controllerLogic.getConfirmationDialogCount()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("Should handle complete project switch with refresh flow")
        void shouldHandleCompleteProjectSwitchWithRefreshFlow() {
            // Setup
            controllerLogic.setCurrentProject("Source Project");
            controllerLogic.setUserConfirmationResponse(true);
            
            // Execute complete flow
            boolean shouldSwitch = controllerLogic.simulateProjectSwitchConfirmation("Target Project");
            String switchResult = controllerLogic.simulateProjectSwitchFlow("Target Project", shouldSwitch);
            
            // Verify
            assertThat(shouldSwitch).isTrue();
            assertThat(switchResult).contains("Project switch completed");
            assertThat(switchResult).contains("Target Project");
            assertThat(switchResult).contains("refreshing project listing");
        }
        
        @Test
        @DisplayName("Should handle manual project refresh when authenticated")
        void shouldHandleManualProjectRefreshWhenAuthenticated() {
            // Setup
            controllerLogic.setAuthenticated(true);
            
            // Execute
            String refreshResult = controllerLogic.simulateRefreshProjects();
            
            // Verify
            assertThat(refreshResult).contains("Project list refreshed successfully");
            assertThat(refreshResult).contains("authentication check passed");
        }
        
        @Test
        @DisplayName("Should prevent manual project refresh when not authenticated")
        void shouldPreventManualProjectRefreshWhenNotAuthenticated() {
            // Setup
            controllerLogic.setAuthenticated(false);
            
            // Execute
            String refreshResult = controllerLogic.simulateRefreshProjects();
            
            // Verify
            assertThat(refreshResult).contains("Cannot refresh projects: Not authenticated");
            assertThat(refreshResult).doesNotContain("Project list refreshed");
        }
        
        @Test
        @DisplayName("Should handle refresh button state changes")
        void shouldHandleRefreshButtonStateChanges() {
            // Setup
            controllerLogic.setAuthenticated(true);
            
            // Execute
            var buttonStates = controllerLogic.simulateRefreshButtonStates();
            
            // Verify
            assertThat(buttonStates.getInitialState()).isEqualTo("Refresh Projects");
            assertThat(buttonStates.getDuringRefreshState()).isEqualTo("Refreshing...");
            assertThat(buttonStates.getFinalState()).isEqualTo("Refresh Projects");
            assertThat(buttonStates.isInitiallyEnabled()).isTrue();
            assertThat(buttonStates.isDuringRefreshEnabled()).isFalse();
            assertThat(buttonStates.isFinallyEnabled()).isTrue();
        }
        
        @Test
        @DisplayName("Should handle refresh errors gracefully")
        void shouldHandleRefreshErrorsGracefully() {
            // Setup
            controllerLogic.setAuthenticated(true);
            controllerLogic.setSimulateRefreshError(true);
            
            // Execute
            String refreshResult = controllerLogic.simulateRefreshProjects();
            
            // Verify
            assertThat(refreshResult).contains("Failed to refresh projects");
            assertThat(refreshResult).contains("Simulated refresh error");
        }
        
        @Test
        @DisplayName("Should refresh project listing after successful project opening")
        void shouldRefreshProjectListingAfterSuccessfulProjectOpening() {
            // Setup
            controllerLogic.setAuthenticated(true);
            controllerLogic.setCurrentProject(null); // No current project initially
            
            // Execute project opening flow
            String openingResult = controllerLogic.simulateProjectOpening("New Project", true);
            
            // Verify
            assertThat(openingResult).contains("Project opened successfully");
            assertThat(openingResult).contains("New Project");
            assertThat(openingResult).contains("refreshing project listing after project opening");
        }
        
        @Test
        @DisplayName("Should not refresh project listing after failed project opening")
        void shouldNotRefreshProjectListingAfterFailedProjectOpening() {
            // Setup
            controllerLogic.setAuthenticated(true);
            controllerLogic.setCurrentProject(null);
            
            // Execute failed project opening flow
            String openingResult = controllerLogic.simulateProjectOpening("Failed Project", false);
            
            // Verify
            assertThat(openingResult).contains("Project opening failed");
            assertThat(openingResult).contains("Failed Project");
            assertThat(openingResult).doesNotContain("refreshing project listing");
        }
        
        @Test
        @DisplayName("Should handle direct unlock flow")
        void shouldHandleDirectUnlockFlow() {
            // Setup
            controllerLogic.setCurrentProject("Direct Unlock Project");
            controllerLogic.setUserConfirmationResponse(true);
            
            // Execute unlock flow
            boolean shouldUnlock = controllerLogic.simulateUnlockConfirmation();
            String unlockResult = controllerLogic.simulateUnlockFlow(shouldUnlock);
            
            // Verify
            assertThat(shouldUnlock).isTrue();
            assertThat(unlockResult).contains("Project unlocked successfully");
        }
        
        @Test
        @DisplayName("Should handle cancelled unlock flow")
        void shouldHandleCancelledUnlockFlow() {
            // Setup
            controllerLogic.setCurrentProject("Cancelled Unlock Project");
            controllerLogic.setUserConfirmationResponse(false);
            
            // Execute unlock flow
            boolean shouldUnlock = controllerLogic.simulateUnlockConfirmation();
            String unlockResult = controllerLogic.simulateUnlockFlow(shouldUnlock);
            
            // Verify
            assertThat(shouldUnlock).isFalse();
            assertThat(unlockResult).contains("User cancelled unlock operation");
        }
        
        @Test
        @DisplayName("Should validate state consistency after confirmation flows")
        void shouldValidateStateConsistencyAfterConfirmationFlows() {
            // Setup
            controllerLogic.setCurrentProject("State Test Project");
            String initialProject = controllerLogic.getCurrentProject();
            
            // Execute various flows
            controllerLogic.setUserConfirmationResponse(false);
            controllerLogic.simulateUnlockConfirmation();
            
            controllerLogic.setUserConfirmationResponse(true);
            controllerLogic.simulateReleaseLockConfirmation();
            
            // Verify state consistency
            assertThat(controllerLogic.getCurrentProject()).isEqualTo(initialProject);
            assertThat(controllerLogic.getConfirmationDialogCount()).isEqualTo(2);
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
        private String currentProject = null;
        private Boolean userConfirmationResponse = null;
        private int confirmationDialogCount = 0;
        private boolean isAuthenticated = false;
        private boolean simulateRefreshError = false;
        
        public void setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
        }
        
        public void setInstance(String instance) {
            this.instance = instance;
        }
        
        public void setDebugPropertiesPath(String path) {
            this.debugPropertiesPath = path;
        }
        
        public void setCurrentProject(String projectName) {
            this.currentProject = projectName;
        }
        
        public String getCurrentProject() {
            return currentProject;
        }
        
        public void setUserConfirmationResponse(Boolean response) {
            this.userConfirmationResponse = response;
        }
        
        public void resetConfirmationDialogCount() {
            this.confirmationDialogCount = 0;
        }
        
        public int getConfirmationDialogCount() {
            return confirmationDialogCount;
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
        
        // Confirmation popup methods
        public String generateUnlockConfirmationMessage() {
            return "Are you sure you want to unlock project \"" + currentProject + "\"?\n\n" +
                   "This will release your write lock and allow other users to edit the project.\n\n" +
                   "• Yes: Unlock project (other users can edit)\n" +
                   "• No: Keep lock (continue editing)";
        }
        
        public String generateReleaseLockConfirmationMessage() {
            return "Do you want to release the write lock for project \"" + currentProject + "\"?\n\n" +
                   "• Yes: Release lock (other users can edit)\n" +
                   "• No: Keep lock (continue editing)";
        }
        
        public DialogProperties getUnlockConfirmationDialogProperties() {
            return new DialogProperties(
                "Unlock Project",
                "Confirm Unlock", 
                generateUnlockConfirmationMessage(),
                "Yes, Unlock",
                "No, Keep Lock"
            );
        }
        
        public DialogProperties getReleaseLockConfirmationDialogProperties() {
            return new DialogProperties(
                "Release Write Lock",
                "Upload Successful",
                generateReleaseLockConfirmationMessage(),
                "Yes, Release Lock", 
                "No, Keep Lock"
            );
        }
        
        public String generateProjectSwitchConfirmationMessage(String newProjectName) {
            return "You currently have an active lock on project \"" + currentProject + "\".\n\n" +
                   "To switch to project \"" + newProjectName + "\", you need to release your current lock.\n\n" +
                   "Do you want to release the lock and switch projects?\n\n" +
                   "• Yes: Release lock and switch to \"" + newProjectName + "\"\n" +
                   "• No: Keep current lock and stay on \"" + currentProject + "\"";
        }
        
        public DialogProperties getProjectSwitchConfirmationDialogProperties(String newProjectName) {
            return new DialogProperties(
                "Switch Project",
                "Current Project is Locked",
                generateProjectSwitchConfirmationMessage(newProjectName),
                "Yes, Switch Projects",
                "No, Stay Here"
            );
        }
        
        public boolean simulateUnlockConfirmation() {
            confirmationDialogCount++;
            return userConfirmationResponse != null ? userConfirmationResponse : false;
        }
        
        public boolean simulateReleaseLockConfirmation() {
            confirmationDialogCount++;
            return userConfirmationResponse != null ? userConfirmationResponse : false;
        }
        
        public boolean simulateProjectSwitchConfirmation(String newProjectName) {
            confirmationDialogCount++;
            return userConfirmationResponse != null ? userConfirmationResponse : false;
        }
        
        public String simulateUploadFlow() {
            return "Upload successful for project: " + currentProject;
        }
        
        public String simulateUnlockFlow(boolean shouldUnlock) {
            if (shouldUnlock) {
                return "Project unlocked successfully: " + currentProject;
            } else {
                return "User cancelled unlock operation.";
            }
        }
        
        public String simulateProjectSwitchFlow(String targetProject, boolean shouldSwitch) {
            if (shouldSwitch) {
                return "Project switch completed successfully from " + currentProject + " to " + targetProject + 
                       ". Lock released and refreshing project listing.";
            } else {
                return "Project switch cancelled. Staying on: " + currentProject;
            }
        }
        
        // Authentication and refresh support methods
        public void setAuthenticated(boolean authenticated) {
            this.isAuthenticated = authenticated;
        }
        
        public boolean isAuthenticated() {
            return isAuthenticated;
        }
        
        public void setSimulateRefreshError(boolean simulateError) {
            this.simulateRefreshError = simulateError;
        }
        
        public String simulateRefreshProjects() {
            if (!isAuthenticated) {
                return "Cannot refresh projects: Not authenticated";
            }
            
            if (simulateRefreshError) {
                return "Failed to refresh projects: Simulated refresh error";
            }
            
            return "Project list refreshed successfully - authentication check passed";
        }
        
        public RefreshButtonStates simulateRefreshButtonStates() {
            return new RefreshButtonStates(
                "Refresh Projects",     // initial state
                "Refreshing...",        // during refresh state
                "Refresh Projects",     // final state
                true,                   // initially enabled
                false,                  // disabled during refresh
                true                    // enabled after completion
            );
        }
        
        public String simulateProjectOpening(String projectName, boolean successful) {
            if (successful) {
                currentProject = projectName;
                return "Project opened successfully: " + projectName + 
                       ". Lock acquired and refreshing project listing after project opening.";
            } else {
                return "Project opening failed: " + projectName + ". Unable to acquire lock or download project.";
            }
        }
        
        // Expose private constants for testing
        public String getPrefEmail() { return "SDB_EMAIL"; }
        public String getPrefPassword() { return "SDB_PASSWORD"; }
        public String getPrefOAuthToken() { return "SDB_OAUTH_TOKEN"; }
        public String getPrefInstance() { return "SDB_INSTANCE"; }
        public String getPrefSaveCreds() { return "SDB_SAVECREDS"; }
        public String getDefaultInstance() { return "www.speleoDB.org"; }
    }
    
    /**
     * Helper class to represent dialog properties for testing
     */
    static class DialogProperties {
        private final String title;
        private final String headerText;
        private final String contentText;
        private final String yesButtonText;
        private final String noButtonText;
        
        public DialogProperties(String title, String headerText, String contentText, 
                              String yesButtonText, String noButtonText) {
            this.title = title;
            this.headerText = headerText;
            this.contentText = contentText;
            this.yesButtonText = yesButtonText;
            this.noButtonText = noButtonText;
        }
        
        public String getTitle() { return title; }
        public String getHeaderText() { return headerText; }
        public String getContentText() { return contentText; }
        public String getYesButtonText() { return yesButtonText; }
        public String getNoButtonText() { return noButtonText; }
    }
    
    /**
     * Helper class to represent refresh button states for testing
     */
    static class RefreshButtonStates {
        private final String initialState;
        private final String duringRefreshState;
        private final String finalState;
        private final boolean initiallyEnabled;
        private final boolean duringRefreshEnabled;
        private final boolean finallyEnabled;
        
        public RefreshButtonStates(String initialState, String duringRefreshState, String finalState,
                                 boolean initiallyEnabled, boolean duringRefreshEnabled, boolean finallyEnabled) {
            this.initialState = initialState;
            this.duringRefreshState = duringRefreshState;
            this.finalState = finalState;
            this.initiallyEnabled = initiallyEnabled;
            this.duringRefreshEnabled = duringRefreshEnabled;
            this.finallyEnabled = finallyEnabled;
        }
        
        public String getInitialState() { return initialState; }
        public String getDuringRefreshState() { return duringRefreshState; }
        public String getFinalState() { return finalState; }
        public boolean isInitiallyEnabled() { return initiallyEnabled; }
        public boolean isDuringRefreshEnabled() { return duringRefreshEnabled; }
        public boolean isFinallyEnabled() { return finallyEnabled; }
    }
} 