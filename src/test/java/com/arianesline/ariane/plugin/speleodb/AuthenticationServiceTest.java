package com.arianesline.ariane.plugin.speleodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.arianesline.ariane.plugin.speleodb.AuthenticationService.AuthenticationCallback;
import com.arianesline.ariane.plugin.speleodb.AuthenticationService.AuthenticationRequest;
import com.arianesline.ariane.plugin.speleodb.AuthenticationService.AuthenticationResult;

/**
 * Unit tests for AuthenticationService.
 * Tests business logic without UI dependencies.
 */
@DisplayName("Authentication Service Tests")
class AuthenticationServiceTest {

    private AuthenticationService authService;
    private MockSpeleoDBService mockSpeleoDBService;
    private MockAuthenticationCallback mockCallback;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        mockSpeleoDBService = new MockSpeleoDBService();
        mockCallback = new MockAuthenticationCallback();
        executorService = Executors.newSingleThreadExecutor();
        authService = new AuthenticationService(mockSpeleoDBService, executorService, mockCallback);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Nested
    @DisplayName("Authentication Request")
    class AuthenticationRequestTests {
        
        @Test
        @DisplayName("Should create authentication request with all fields")
        void shouldCreateAuthenticationRequestWithAllFields() {
            AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password", "oauth123", "localhost");
            
            assertThat(request.getEmail()).isEqualTo("test@example.com");
            assertThat(request.getPassword()).isEqualTo("password");
            assertThat(request.getOAuthToken()).isEqualTo("oauth123");
            assertThat(request.getInstance()).isEqualTo("localhost");
        }
        
        @Test
        @DisplayName("Should handle null values in request")
        void shouldHandleNullValuesInRequest() {
            AuthenticationRequest request = new AuthenticationRequest(null, null, null, null);
            
            assertThat(request.getEmail()).isNull();
            assertThat(request.getPassword()).isNull();
            assertThat(request.getOAuthToken()).isNull();
            assertThat(request.getInstance()).isNull();
        }
    }

    @Nested
    @DisplayName("Authentication Result")
    class AuthenticationResultTests {
        
        @Test
        @DisplayName("Should create success result")
        void shouldCreateSuccessResult() {
            AuthenticationResult result = AuthenticationResult.success("test.instance.com");
            
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Connected successfully");
            assertThat(result.getInstance()).isEqualTo("test.instance.com");
        }
        
        @Test
        @DisplayName("Should create failure result")
        void shouldCreateFailureResult() {
            AuthenticationResult result = AuthenticationResult.failure("Connection failed");
            
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Connection failed");
            assertThat(result.getInstance()).isNull();
        }
    }

    @Nested
    @DisplayName("Synchronous Authentication")
    class SyncAuthenticationTests {
        
        @Test
        @DisplayName("Should authenticate successfully with valid credentials")
        void shouldAuthenticateSuccessfullyWithValidCredentials() {
            mockSpeleoDBService.setAuthenticationSuccess(true);
            AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password", null, "test.instance.com");
            
            AuthenticationResult result = authService.authenticate(request);
            
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getInstance()).isEqualTo("test.instance.com");
            assertThat(mockCallback.getLoggedMessages()).contains("Connecting to test.instance.com", "Connected successfully.");
        }
        
        @Test
        @DisplayName("Should fail authentication with invalid credentials")
        void shouldFailAuthenticationWithInvalidCredentials() {
            mockSpeleoDBService.setAuthenticationSuccess(false);
            AuthenticationRequest request = new AuthenticationRequest("test@example.com", "wrongpassword", null, "test.instance.com");
            
            AuthenticationResult result = authService.authenticate(request);
            
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Connection failed");
        }
        
        @Test
        @DisplayName("Should use default instance when instance is empty")
        void shouldUseDefaultInstanceWhenInstanceIsEmpty() {
            mockSpeleoDBService.setAuthenticationSuccess(true);
            AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password", null, "");
            
            AuthenticationResult result = authService.authenticate(request);
            
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getInstance()).isEqualTo("www.speleoDB.org");
            assertThat(mockCallback.getLoggedMessages()).contains("Connecting to www.speleoDB.org");
        }
        
        @Test
        @DisplayName("Should use default instance when instance is null")
        void shouldUseDefaultInstanceWhenInstanceIsNull() {
            mockSpeleoDBService.setAuthenticationSuccess(true);
            AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password", null, null);
            
            AuthenticationResult result = authService.authenticate(request);
            
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getInstance()).isEqualTo("www.speleoDB.org");
        }
    }

    @Nested
    @DisplayName("Asynchronous Authentication")
    class AsyncAuthenticationTests {
        
        @Test
        @DisplayName("Should authenticate asynchronously")
        void shouldAuthenticateAsynchronously() throws Exception {
            mockSpeleoDBService.setAuthenticationSuccess(true);
            AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password", null, "async.test.com");
            
            CompletableFuture<AuthenticationResult> future = authService.authenticateAsync(request);
            AuthenticationResult result = future.get();
            
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getInstance()).isEqualTo("async.test.com");
            assertThat(mockCallback.isAuthenticationStartedCalled()).isTrue();
        }
        
        @Test
        @DisplayName("Should handle async authentication failure")
        void shouldHandleAsyncAuthenticationFailure() throws Exception {
            mockSpeleoDBService.setAuthenticationSuccess(false);
            AuthenticationRequest request = new AuthenticationRequest("test@example.com", "wrongpassword", null, "async.test.com");
            
            CompletableFuture<AuthenticationResult> future = authService.authenticateAsync(request);
            AuthenticationResult result = future.get();
            
            assertThat(result.isSuccess()).isFalse();
            assertThat(mockCallback.isAuthenticationStartedCalled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Disconnection")
    class DisconnectionTests {
        
        @Test
        @DisplayName("Should disconnect when authenticated")
        void shouldDisconnectWhenAuthenticated() {
            mockSpeleoDBService.setAuthenticated(true);
            mockSpeleoDBService.setInstanceUrl("test.instance.com");
            
            authService.disconnect();
            
            assertThat(mockSpeleoDBService.isLoggedOut()).isTrue();
            assertThat(mockCallback.isDisconnectedCalled()).isTrue();
            assertThat(mockCallback.getLoggedMessages()).contains("Disconnected from test.instance.com");
        }
        
        @Test
        @DisplayName("Should handle disconnect when not authenticated")
        void shouldHandleDisconnectWhenNotAuthenticated() {
            mockSpeleoDBService.setAuthenticated(false);
            
            authService.disconnect();
            
            assertThat(mockCallback.isDisconnectedCalled()).isTrue();
            assertThat(mockSpeleoDBService.isLoggedOut()).isFalse(); // logout() not called if not authenticated
        }
        
        @Test
        @DisplayName("Should disconnect asynchronously")
        void shouldDisconnectAsynchronously() throws Exception {
            mockSpeleoDBService.setAuthenticated(true);
            mockSpeleoDBService.setInstanceUrl("async.test.com");
            
            CompletableFuture<Void> future = authService.disconnectAsync();
            future.get();
            
            assertThat(mockSpeleoDBService.isLoggedOut()).isTrue();
            assertThat(mockCallback.isDisconnectedCalled()).isTrue();
        }
    }

    @Nested
    @DisplayName("State Queries")
    class StateQueryTests {
        
        @Test
        @DisplayName("Should return authentication status")
        void shouldReturnAuthenticationStatus() {
            mockSpeleoDBService.setAuthenticated(false);
            assertThat(authService.isAuthenticated()).isFalse();
            
            mockSpeleoDBService.setAuthenticated(true);
            assertThat(authService.isAuthenticated()).isTrue();
        }
        
        @Test
        @DisplayName("Should return current instance when authenticated")
        void shouldReturnCurrentInstanceWhenAuthenticated() {
            mockSpeleoDBService.setAuthenticated(true);
            mockSpeleoDBService.setInstanceUrl("current.instance.com");
            
            assertThat(authService.getCurrentInstance()).isEqualTo("current.instance.com");
        }
        
        @Test
        @DisplayName("Should return null when not authenticated")
        void shouldReturnNullWhenNotAuthenticated() {
            mockSpeleoDBService.setAuthenticated(false);
            
            assertThat(authService.getCurrentInstance()).isNull();
        }
    }

    // ===================== MOCK CLASSES ===================== //

    static class MockSpeleoDBService extends SpeleoDBService {
        private boolean authenticated = false;
        private boolean authenticationSuccess = true;
        private boolean loggedOut = false;
        private String instanceUrl = "";

        public MockSpeleoDBService() {
            super(null); // No controller needed for mock
        }

        @Override
        public void authenticate(String email, String password, String oAuthToken, String instanceUrl) throws Exception {
            this.instanceUrl = instanceUrl;
            if (!authenticationSuccess) {
                throw new Exception("Authentication failed");
            }
            authenticated = true;
            loggedOut = false;
        }

        @Override
        public boolean isAuthenticated() {
            return authenticated;
        }

        @Override
        public String getSDBInstance() throws IllegalStateException {
            if (!authenticated) {
                throw new IllegalStateException("Not authenticated");
            }
            return instanceUrl;
        }

        @Override
        public void logout() {
            authenticated = false;
            loggedOut = true;
        }

        // Test helper methods
        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }

        public void setAuthenticationSuccess(boolean authenticationSuccess) {
            this.authenticationSuccess = authenticationSuccess;
        }

        public void setInstanceUrl(String instanceUrl) {
            this.instanceUrl = instanceUrl;
        }

        public boolean isLoggedOut() {
            return loggedOut;
        }
    }

    static class MockAuthenticationCallback implements AuthenticationCallback {
        private boolean authenticationStartedCalled = false;
        private boolean authenticationSuccessCalled = false;
        private boolean authenticationFailedCalled = false;
        private boolean disconnectedCalled = false;
        private final java.util.List<String> loggedMessages = new java.util.ArrayList<>();

        @Override
        public void onAuthenticationStarted() {
            authenticationStartedCalled = true;
        }

        @Override
        public void onAuthenticationSuccess() {
            authenticationSuccessCalled = true;
        }

        @Override
        public void onAuthenticationFailed(String message) {
            authenticationFailedCalled = true;
            loggedMessages.add("AUTH_FAILED: " + message);
        }

        @Override
        public void onDisconnected() {
            disconnectedCalled = true;
        }

        @Override
        public void logMessage(String message) {
            loggedMessages.add(message);
        }

        // Test helper methods
        public boolean isAuthenticationStartedCalled() { return authenticationStartedCalled; }
        public boolean isAuthenticationSuccessCalled() { return authenticationSuccessCalled; }
        public boolean isAuthenticationFailedCalled() { return authenticationFailedCalled; }
        public boolean isDisconnectedCalled() { return disconnectedCalled; }
        public java.util.List<String> getLoggedMessages() { return loggedMessages; }
    }
} 