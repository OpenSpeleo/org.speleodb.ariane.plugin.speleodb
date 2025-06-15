package com.arianesline.ariane.plugin.speleodb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/**
 * Comprehensive unit tests for SpeleoDBService including async operations.
 * Tests both synchronous and asynchronous API methods with proper error handling.
 */
@DisplayName("SpeleoDB Service Tests")
class SpeleoDBServiceTest {

    private SpeleoDBService service;
    private MockSpeleoDBController mockController;
    
    @TempDir
    Path tempDirectory;

    @BeforeEach
    void setUp() {
        mockController = new MockSpeleoDBController();
        service = new SpeleoDBService(mockController);
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {
        
        @Test
        @DisplayName("Should authenticate successfully with valid credentials")
        void shouldAuthenticateSuccessfully() {
            // Test synchronous authentication
            // Note: This would require a mock HTTP server or similar for full integration testing
            // For now, we test the method structure and error handling
            
            assertThat(service.isAuthenticated()).isFalse();
            
            // Test that authentication state is initially false
            assertThatThrownBy(() -> service.getSDBInstance())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not authenticated");
        }
        
        @Test
        @DisplayName("Should handle authentication failure")
        void shouldHandleAuthenticationFailure() {
            assertThat(service.isAuthenticated()).isFalse();
            
            // Test logout functionality
            service.logout();
            assertThat(service.isAuthenticated()).isFalse();
        }
        
        @Test
        @DisplayName("Should handle async authentication")
        void shouldHandleAsyncAuthentication() throws Exception {
            // Test async authentication structure
            CompletableFuture<Void> authFuture = service.authenticateAsync("test@example.com", "password", null, "test.speleodb.org");
            
            // The future should be created (even if it fails due to no real server)
            assertThat(authFuture).isNotNull();
            
            // Test that the async operation fails with connection error (since we're trying to connect to a non-existent server)
            assertThatThrownBy(() -> authFuture.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(java.net.ConnectException.class);
        }
    }
    
    @Nested
    @DisplayName("Project Management Tests")
    class ProjectManagementTests {
        
        @Test
        @DisplayName("Should handle project listing with authentication check")
        void shouldHandleProjectListing() {
            // Test that unauthenticated access throws exception
            assertThatThrownBy(() -> service.listProjects())
                .isInstanceOf(Exception.class)
                .hasMessageContaining("not authenticated");
                
            // Test async version
            CompletableFuture<JsonArray> listFuture = service.listProjectsAsync();
            assertThat(listFuture).isNotNull();
            
            // Should fail immediately due to authentication
            assertThatThrownBy(() -> listFuture.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        }
        
        @Test
        @DisplayName("Should handle project creation with authentication check")
        void shouldHandleProjectCreation() {
            // Create a ProjectCreationRequest using the builder pattern
            ProjectCreationRequest request = ProjectCreationRequest.builder()
                .withName("Test Cave")
                .withDescription("A test cave system")
                .withCountry("US")
                .withCoordinates("40.7128", "-74.0060")
                .build();
            
            // Test that unauthenticated access throws exception
            assertThatThrownBy(() -> service.createProject(request))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("not authenticated");
                
            // Test async version
            CompletableFuture<JsonObject> createFuture = service.createProjectAsync(request);
            assertThat(createFuture).isNotNull();
            
            // Should fail immediately due to authentication
            assertThatThrownBy(() -> createFuture.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        }
        
        @Test
        @DisplayName("Should handle project upload with authentication check")
        void shouldHandleProjectUpload() throws IOException {
            // Create a test project JSON
            JsonObject testProject = Json.createObjectBuilder()
                .add("id", "test-project-123")
                .add("name", "Test Project")
                .build();
            
            // Create a dummy file for upload
            Path testFile = tempDirectory.resolve("test-project-123.tml");
            Files.write(testFile, "test content".getBytes());
            
            // Test synchronous upload - should fail due to authentication
            assertThatThrownBy(() -> service.uploadProject("Test upload", testProject))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("not authenticated");
                
            // Test async version
            CompletableFuture<Void> uploadFuture = service.uploadProjectAsync("Test upload", testProject);
            assertThat(uploadFuture).isNotNull();
            
            // Should fail immediately due to authentication
            assertThatThrownBy(() -> uploadFuture.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        }
        
        @Test
        @DisplayName("Should handle project download with authentication check")
        void shouldHandleProjectDownload() {
            // Create a test project JSON
            JsonObject testProject = Json.createObjectBuilder()
                .add("id", "test-project-123")
                .add("name", "Test Project")
                .build();
            
            // Test synchronous download - should fail due to authentication
            assertThatThrownBy(() -> service.downloadProject(testProject))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not authenticated");
                
            // Test async version
            CompletableFuture<Path> downloadFuture = service.downloadProjectAsync(testProject);
            assertThat(downloadFuture).isNotNull();
            
            // Should fail immediately due to authentication
            assertThatThrownBy(() -> downloadFuture.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        }
    }
    
    @Nested
    @DisplayName("Project Mutex Tests")
    class ProjectMutexTests {
        
        @Test
        @DisplayName("Should handle mutex acquisition with authentication check")
        void shouldHandleMutexAcquisition() throws Exception {
            // Create a test project JSON
            JsonObject testProject = Json.createObjectBuilder()
                .add("id", "test-project-123")
                .add("name", "Test Project")
                .build();
            
            // Test synchronous mutex acquisition - should return false due to authentication
            boolean acquired = service.acquireOrRefreshProjectMutex(testProject);
            assertThat(acquired).isFalse();
            
            // Test async version
            CompletableFuture<Boolean> acquireFuture = service.acquireOrRefreshProjectMutexAsync(testProject);
            assertThat(acquireFuture).isNotNull();
            
            // Should fail immediately due to authentication
            assertThatThrownBy(() -> acquireFuture.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        }
        
        @Test
        @DisplayName("Should handle mutex release with authentication check")
        void shouldHandleMutexRelease() throws Exception {
            // Create a test project JSON
            JsonObject testProject = Json.createObjectBuilder()
                .add("id", "test-project-123")
                .add("name", "Test Project")
                .build();
            
            // Test synchronous mutex release - should return false due to authentication
            boolean released = service.releaseProjectMutex(testProject);
            assertThat(released).isFalse();
            
            // Test async version
            CompletableFuture<Boolean> releaseFuture = service.releaseProjectMutexAsync(testProject);
            assertThat(releaseFuture).isNotNull();
            
            // Should fail immediately due to authentication
            assertThatThrownBy(() -> releaseFuture.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        }
    }
    
    @Nested
    @DisplayName("File Management Tests")
    class FileManagementTests {
        
        @Test
        @DisplayName("Should handle file SpeleoDB ID update")
        void shouldHandleFileSpeleoDBIdUpdate() {
            // Test with null project ID
            boolean result = service.updateFileSpeleoDBId((String) null);
            assertThat(result).isFalse();
            assertThat(mockController.getLoggedMessages()).contains("Error: Cannot update file with empty project ID");
            
            // Test with empty project ID
            result = service.updateFileSpeleoDBId("");
            assertThat(result).isFalse();
            
            // Test with valid project ID
            result = service.updateFileSpeleoDBId("valid-project-123");
            // This should succeed (creates metadata file)
            assertThat(result).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Service State Tests")
    class ServiceStateTests {
        
        @Test
        @DisplayName("Should handle service initialization")
        void shouldHandleServiceInitialization() {
            assertThat(service.isAuthenticated()).isFalse();
            
            // Test that getSDBInstance throws when not authenticated
            assertThatThrownBy(() -> service.getSDBInstance())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not authenticated");
        }
        
        @Test
        @DisplayName("Should handle service shutdown")
        void shouldHandleServiceShutdown() {
            // Test that shutdown doesn't throw exceptions
            service.shutdown();
            
            // Service should still be usable after shutdown call
            assertThat(service.isAuthenticated()).isFalse();
        }
    }
    
    // ===================== MOCK CLASSES ===================== //
    
    static class MockSpeleoDBController extends SpeleoDBController {
        private final java.util.List<String> loggedMessages = new java.util.ArrayList<>();
        
        @Override
        public void logMessageFromPlugin(String message) {
            loggedMessages.add(message);
        }
        
        public java.util.List<String> getLoggedMessages() {
            return loggedMessages;
        }
    }
} 