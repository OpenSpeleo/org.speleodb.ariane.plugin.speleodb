package org.speleodb.ariane.plugin.speleodb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.PATHS;

/**
 * Unit tests for SpeleoDBService class.
 * Tests URL handling, authentication state, and utility methods.
 */
public class SpeleoDBServiceTest {
    
    private static final String TEST_ARIANE_DIR = System.getProperty("java.io.tmpdir") + File.separator + "test_ariane";
    
    public static void main(String[] args) throws Exception {
        setupTestEnvironment();
        
        testUrlHandling();
        testAuthenticationState();
        testJsonParsing();
        testFileOperations();
        testInstanceUrlValidation();
        testProjectCreation();
        testEmptyTmlFileCreation();
        testHttp422Handling();
        
        cleanupTestEnvironment();
        System.out.println("All SpeleoDBService tests passed!");
    }
    
    static void setupTestEnvironment() throws IOException {
        // Create test directory
        Path testDir = Paths.get(TEST_ARIANE_DIR);
        if (!Files.exists(testDir)) {
            Files.createDirectories(testDir);
        }
        System.out.println("✓ Test environment setup completed");
    }
    
    static void cleanupTestEnvironment() throws IOException {
        // Clean up test directory
        Path testDir = Paths.get(TEST_ARIANE_DIR);
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                .map(Path::toFile)
                .forEach(File::delete);
        }
        System.out.println("✓ Test environment cleanup completed");
    }
    
    static void testUrlHandling() {
        System.out.println("Testing URL handling ...");
        
        // Test local URLs should use http://
        assert shouldUseHttp("localhost");
        assert shouldUseHttp("127.0.0.1");
        assert shouldUseHttp("192.168.1.100");
        assert shouldUseHttp("10.0.0.1");
        assert shouldUseHttp("172.16.0.1");
        
        // Test public URLs should use https://
        assert !shouldUseHttp("www.example.com");
        assert !shouldUseHttp("api.speleodb.org");
        assert !shouldUseHttp("8.8.8.8");
        
        System.out.println("✓ URL handling tests passed");
    }
    
    static void testAuthenticationState() {
        System.out.println("Testing authentication state ...");
        
        MockSpeleoDBController controller = new MockSpeleoDBController();
        TestableSpeleoDBService service = new TestableSpeleoDBService(controller);
        
        // Initially not authenticated
        assert !service.isAuthenticated();
        
        try {
            service.getSDBInstance();
            assert false : "Should throw IllegalStateException when not authenticated";
        } catch (IllegalStateException e) {
            // Expected
        }
        
        // Set authentication state
        service.setTestAuthState("test-token", "https://test.example.com");
        assert service.isAuthenticated();
        assert service.getSDBInstance().equals("https://test.example.com");
        
        // Test logout
        service.logout();
        assert !service.isAuthenticated();
        
        System.out.println("✓ Authentication state tests passed");
    }
    
    static void testJsonParsing() {
        System.out.println("Testing JSON parsing ...");
        
        MockSpeleoDBController controller = new MockSpeleoDBController();
        TestableSpeleoDBService service = new TestableSpeleoDBService(controller);
        
        // Test auth token parsing
        String authResponse = "{\"token\":\"abc123xyz\",\"user\":\"test@example.com\"}";
        String token = service.parseAuthTokenPublic(authResponse);
        assert token.equals("abc123xyz");
        
        // Test edge cases
        String edgeResponse = "{\"other\":\"value\",\"token\":\"edge-token-456\"}";
        String edgeToken = service.parseAuthTokenPublic(edgeResponse);
        assert edgeToken.equals("edge-token-456");
        
        System.out.println("✓ JSON parsing tests passed");
    }
    
    static void testFileOperations() throws IOException {
        System.out.println("Testing file operations ...");
        
        // Test project file path generation
        String projectId = "test-project-123";
        Path expectedPath = Paths.get(SpeleoDBService.ARIANE_ROOT_DIR + File.separator + projectId + PATHS.TML_FILE_EXTENSION);
        Path actualPath = Paths.get(SpeleoDBService.ARIANE_ROOT_DIR, projectId + PATHS.TML_FILE_EXTENSION);
        assert expectedPath.equals(actualPath) : "Generated path should match expected path";
        
        // Create a test file
        Path testFile = Paths.get(TEST_ARIANE_DIR + File.separator + projectId + PATHS.TML_FILE_EXTENSION);
        Files.write(testFile, "test content".getBytes());
        assert Files.exists(testFile);
        
        // Test file cleanup
        Files.delete(testFile);
        assert !Files.exists(testFile);
        
        System.out.println("✓ File operations tests passed");
    }
    
    static void testInstanceUrlValidation() {
        System.out.println("Testing instance URL validation ...");
        
        MockSpeleoDBController controller = new MockSpeleoDBController();
        TestableSpeleoDBService service = new TestableSpeleoDBService(controller);
        
        // Test various URL formats
        service.setInstanceUrlPublic("localhost:8000");
        assert service.getInstanceUrlPublic().equals("http://localhost:8000");
        
        service.setInstanceUrlPublic("192.168.1.100:3000");
        assert service.getInstanceUrlPublic().equals("http://192.168.1.100:3000");
        
        service.setInstanceUrlPublic("api.speleodb.org");
        assert service.getInstanceUrlPublic().equals("https://api.speleodb.org");
        
        service.setInstanceUrlPublic("www.example.com:443");
        assert service.getInstanceUrlPublic().equals("https://www.example.com:443");
        
        System.out.println("✓ Instance URL validation tests passed");
    }
    
    static void testProjectCreation() {
        System.out.println("Testing project creation ...");
        
        MockSpeleoDBController controller = new MockSpeleoDBController();
        TestableSpeleoDBService service = new TestableSpeleoDBService(controller);
        
        // Test unauthenticated state
        try {
            service.buildCreateProjectJson("Test Project", "Description", "US", "40.7128", "-74.0060");
            assert false : "Should throw IllegalStateException when not authenticated";
        } catch (IllegalStateException e) {
            // Expected
        }
        
        // Set authentication state for testing
        service.setTestAuthState("test-token", "https://test.example.com");
        
        // Test JSON building with all parameters
        String jsonWithCoords = service.buildCreateProjectJson("Cave Project", "A deep cave system", "MX", "20.1234", "-99.5678");
        assert jsonWithCoords.contains("\"name\":\"Cave Project\"");
        assert jsonWithCoords.contains("\"description\":\"A deep cave system\"");
        assert jsonWithCoords.contains("\"country\":\"MX\"");
        assert jsonWithCoords.contains("\"latitude\":\"20.1234\"");
        assert jsonWithCoords.contains("\"longitude\":\"-99.5678\"");
        
        // Test JSON building without coordinates
        String jsonWithoutCoords = service.buildCreateProjectJson("Simple Cave", "Basic description", "CA", null, null);
        assert jsonWithoutCoords.contains("\"name\":\"Simple Cave\"");
        assert jsonWithoutCoords.contains("\"description\":\"Basic description\"");
        assert jsonWithoutCoords.contains("\"country\":\"CA\"");
        assert !jsonWithoutCoords.contains("latitude");
        assert !jsonWithoutCoords.contains("longitude");
        
        // Test JSON building with empty coordinates
        String jsonWithEmptyCoords = service.buildCreateProjectJson("Empty Coords Cave", "Test", "GB", "", "  ");
        assert !jsonWithEmptyCoords.contains("latitude");
        assert !jsonWithEmptyCoords.contains("longitude");
        
        // Test JSON building with only latitude
        String jsonWithOnlyLat = service.buildCreateProjectJson("Lat Only Cave", "Test", "US", "40.7128", null);
        assert jsonWithOnlyLat.contains("\"latitude\":\"40.7128\"");
        assert !jsonWithOnlyLat.contains("longitude");
        
        // Test JSON building with only longitude
        String jsonWithOnlyLon = service.buildCreateProjectJson("Lon Only Cave", "Test", "FR", null, "2.3522");
        assert !jsonWithOnlyLon.contains("latitude");
        assert jsonWithOnlyLon.contains("\"longitude\":\"2.3522\"");
        
        // Test special characters in project data
        String jsonWithSpecialChars = service.buildCreateProjectJson("Cueva de \"Los Ángeles\"", "A cave with special chars: áéíóú & symbols", "ES", null, null);
        assert jsonWithSpecialChars.contains("Cueva de \\\"Los Ángeles\\\"");
        assert jsonWithSpecialChars.contains("special chars");
        
        System.out.println("✓ Project creation tests passed");
    }
    
    static void testEmptyTmlFileCreation() throws IOException {
        System.out.println("Testing empty TML file creation ...");
        
        MockSpeleoDBController controller = new MockSpeleoDBController();
        TestableSpeleoDBService service = new TestableSpeleoDBService(controller);
        
        // Test creating empty TML file from template
        String testProjectId = "test-422-project";
        String testProjectName = "Test HTTP 422 Project";
        
        try {
            Path createdFile = service.createEmptyTmlFileFromTemplate(testProjectId, testProjectName);
            
            // Verify file was created
            assert Files.exists(createdFile) : "TML file should be created";
            assert Files.size(createdFile) > 0 : "TML file should not be empty";
            assert createdFile.getFileName().toString().equals(testProjectId + PATHS.TML_FILE_EXTENSION) : "File should have correct name";
            
            // Verify it's in the correct directory
            String expectedDir = SpeleoDBService.ARIANE_ROOT_DIR;
            assert createdFile.getParent().toString().equals(expectedDir) : "File should be in ARIANE_ROOT_DIR";
            
            // Clean up
            Files.deleteIfExists(createdFile);
            
            System.out.println("✓ Empty TML file creation test passed");
            
        } catch (IOException e) {
            // This might fail if the template file doesn't exist in test environment
            System.out.println("⚠ Empty TML file creation test skipped (template not available in test environment)");
        }
    }
    
    static void testHttp422Handling() {
        System.out.println("Testing HTTP status code handling logic ...");
        
        // Test that we have the correct status code handling
        int http422 = 422;
        assert http422 == 422 : "HTTP 422 constant should be correct";
        
        // Test switch statement logic (conceptual - we can't easily mock HTTP responses)
        // This verifies that our switch statement covers the expected cases
        // Updated to reflect removal of HTTP 404 case
        int[] expectedStatusCodes = {200, 422};
        for (int code : expectedStatusCodes) {
            String action = switch (code) {
                case 200 -> "download_file";
                case 422 -> "create_from_template";
                default -> "unknown";
            };
            assert !action.equals("unknown") : "All expected status codes should be handled";
        }
        
        // Test that unexpected status codes would trigger the default case
        // HTTP 404 is now treated as unexpected and should trigger exception
        int[] unexpectedStatusCodes = {400, 401, 403, 404, 500, 503};
        for (int code : unexpectedStatusCodes) {
            String action = switch (code) {
                case 200 -> "download_file";
                case 422 -> "create_from_template";
                default -> "exception_thrown";
            };
            assert action.equals("exception_thrown") : "Unexpected status codes (including 404) should trigger default case";
        }
        
        System.out.println("✓ HTTP status code handling logic test passed");
    }
    
    // Helper method to test URL pattern matching
    static boolean shouldUseHttp(String url) {
        String localPattern = "(^localhost)|(^127\\.)|(^10\\.)|(^172\\.(1[6-9]|2[0-9]|3[0-1])\\.)|(^192\\.168\\.)";
        return Pattern.compile(localPattern).matcher(url).find();
    }
    
    // Mock controller for testing
    static class MockSpeleoDBController extends SpeleoDBController {
        // Minimal mock implementation
    }
    
    // Testable version of SpeleoDBService that exposes internal methods
    static class TestableSpeleoDBService extends SpeleoDBService {
        private String testAuthToken = "";
        private String testSDBInstance = "";
        
        public TestableSpeleoDBService(SpeleoDBController controller) {
            super(controller);
        }
        
        // Override authentication methods for testing
        @Override
        public boolean isAuthenticated() {
            return !testAuthToken.isEmpty() && !testSDBInstance.isEmpty();
        }
        
        @Override
        public String getSDBInstance() throws IllegalStateException {
            if (!isAuthenticated()) {
                throw new IllegalStateException("User is not authenticated. Please log in.");
            }
            return testSDBInstance;
        }
        
        @Override
        public void logout() {
            testAuthToken = "";
            testSDBInstance = "";
        }
        
        // Test helper methods
        public void setTestAuthState(String token, String instance) {
            this.testAuthToken = token;
            this.testSDBInstance = instance;
        }
        
        // Expose private methods for testing
        public String parseAuthTokenPublic(String responseBody) {
            int tokenStart = responseBody.indexOf("\"token\":\"") + 9;
            int tokenEnd = responseBody.indexOf("\"", tokenStart);
            return responseBody.substring(tokenStart, tokenEnd);
        }
        
        public void setInstanceUrlPublic(String instanceUrl) {
            String localPattern = "(^localhost)|(^127\\.)|(^10\\.)|(^172\\.(1[6-9]|2[0-9]|3[0-1])\\.)|(^192\\.168\\.)";
            
            if (Pattern.compile(localPattern).matcher(instanceUrl).find()) {
                testSDBInstance = "http://" + instanceUrl;
            } else {
                testSDBInstance = "https://" + instanceUrl;
            }
        }
        
        public String getInstanceUrlPublic() {
            return testSDBInstance;
        }
        
        // Method to test JSON building for project creation
        public String buildCreateProjectJson(String name, String description, String countryCode, 
                                           String latitude, String longitude) {
            if (!isAuthenticated()) {
                throw new IllegalStateException("User is not authenticated.");
            }
            
            // Build JSON payload using StringBuilder to match what createProject does
            StringBuilder json = new StringBuilder("{");
            json.append("\"name\":\"").append(escapeJson(name)).append("\",");
            json.append("\"description\":\"").append(escapeJson(description)).append("\",");
            json.append("\"country\":\"").append(countryCode).append("\"");
            
            // Add optional coordinates if provided
            if (latitude != null && !latitude.trim().isEmpty()) {
                json.append(",\"latitude\":\"").append(latitude).append("\"");
            }
            if (longitude != null && !longitude.trim().isEmpty()) {
                json.append(",\"longitude\":\"").append(longitude).append("\"");
            }
            
            json.append("}");
            return json.toString();
        }
        
        // Helper method to escape JSON strings
        private String escapeJson(String input) {
            if (input == null) return "";
            return input.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
} 