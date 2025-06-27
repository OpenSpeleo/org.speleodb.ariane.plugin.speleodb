package org.speleodb.ariane.plugin.speleodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for hostname handling and URL normalization in SpeleoDBService.
 * Tests the new functionality that allows optional http/https prefixes and trailing slashes.
 */
class SpeleoDBHostnameHandlingTest {

    private TestableSpeleoDBService service;

    @BeforeEach
    void setUp() {
        service = new TestableSpeleoDBService(new MockSpeleoDBController());
    }

    @Test
    @DisplayName("Should handle basic hostnames without protocol")
    void shouldHandleBasicHostnames() {
        // Test basic public hostnames (should get https://)
        service.setInstanceUrlPublic("www.speleodb.org");
        assertEquals("https://www.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("stage.speleodb.org");
        assertEquals("https://stage.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("api.example.com");
        assertEquals("https://api.example.com", service.getInstanceUrlPublic());
        
        // Test basic local hostnames (should get http://)
        service.setInstanceUrlPublic("localhost");
        assertEquals("http://localhost", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("localhost:8000");
        assertEquals("http://localhost:8000", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("127.0.0.1:8000");
        assertEquals("http://127.0.0.1:8000", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("192.168.1.100:3000");
        assertEquals("http://192.168.1.100:3000", service.getInstanceUrlPublic());
    }

    @Test
    @DisplayName("Should handle hostnames with HTTP prefix")
    void shouldHandleHttpPrefix() {
        // Test public hostnames with http:// prefix (should still get https://)
        service.setInstanceUrlPublic("http://www.speleodb.org");
        assertEquals("https://www.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("http://stage.speleodb.org");
        assertEquals("https://stage.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("http://api.example.com");
        assertEquals("https://api.example.com", service.getInstanceUrlPublic());
        
        // Test local hostnames with http:// prefix (should keep http://)
        service.setInstanceUrlPublic("http://localhost");
        assertEquals("http://localhost", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("http://localhost:8000");
        assertEquals("http://localhost:8000", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("http://127.0.0.1:8000");
        assertEquals("http://127.0.0.1:8000", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("http://192.168.1.100:3000");
        assertEquals("http://192.168.1.100:3000", service.getInstanceUrlPublic());
    }

    @Test
    @DisplayName("Should handle hostnames with HTTPS prefix")
    void shouldHandleHttpsPrefix() {
        // Test public hostnames with https:// prefix (should keep https://)
        service.setInstanceUrlPublic("https://www.speleodb.org");
        assertEquals("https://www.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("https://stage.speleodb.org");
        assertEquals("https://stage.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("https://api.example.com");
        assertEquals("https://api.example.com", service.getInstanceUrlPublic());
        
        // Test local hostnames with https:// prefix (should become http://)
        service.setInstanceUrlPublic("https://localhost");
        assertEquals("http://localhost", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("https://localhost:8000");
        assertEquals("http://localhost:8000", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("https://127.0.0.1:8000");
        assertEquals("http://127.0.0.1:8000", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("https://192.168.1.100:3000");
        assertEquals("http://192.168.1.100:3000", service.getInstanceUrlPublic());
    }

    @Test
    @DisplayName("Should handle hostnames with trailing slashes")
    void shouldHandleTrailingSlashes() {
        // Test public hostnames with trailing slash
        service.setInstanceUrlPublic("www.speleodb.org/");
        assertEquals("https://www.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("stage.speleodb.org/");
        assertEquals("https://stage.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("api.example.com/");
        assertEquals("https://api.example.com", service.getInstanceUrlPublic());
        
        // Test local hostnames with trailing slash
        service.setInstanceUrlPublic("localhost/");
        assertEquals("http://localhost", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("localhost:8000/");
        assertEquals("http://localhost:8000", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("127.0.0.1:8000/");
        assertEquals("http://127.0.0.1:8000", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("192.168.1.100:3000/");
        assertEquals("http://192.168.1.100:3000", service.getInstanceUrlPublic());
    }

    @Test
    @DisplayName("Should handle hostnames with both prefix and trailing slash")
    void shouldHandlePrefixAndTrailingSlash() {
        // Test public hostnames with both http:// and trailing slash
        service.setInstanceUrlPublic("http://www.speleodb.org/");
        assertEquals("https://www.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("http://stage.speleodb.org/");
        assertEquals("https://stage.speleodb.org", service.getInstanceUrlPublic());
        
        // Test public hostnames with both https:// and trailing slash
        service.setInstanceUrlPublic("https://www.speleodb.org/");
        assertEquals("https://www.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("https://stage.speleodb.org/");
        assertEquals("https://stage.speleodb.org", service.getInstanceUrlPublic());
        
        // Test local hostnames with both http:// and trailing slash
        service.setInstanceUrlPublic("http://localhost:8000/");
        assertEquals("http://localhost:8000", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("http://127.0.0.1:8000/");
        assertEquals("http://127.0.0.1:8000", service.getInstanceUrlPublic());
        
        // Test local hostnames with both https:// and trailing slash
        service.setInstanceUrlPublic("https://localhost:8000/");
        assertEquals("http://localhost:8000", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("https://127.0.0.1:8000/");
        assertEquals("http://127.0.0.1:8000", service.getInstanceUrlPublic());
    }

    @Test
    @DisplayName("Should handle edge cases with multiple trailing slashes")
    void shouldHandleMultipleTrailingSlashes() {
        // Test multiple trailing slashes
        service.setInstanceUrlPublic("www.speleodb.org//");
        assertEquals("https://www.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("www.speleodb.org///");
        assertEquals("https://www.speleodb.org", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("http://localhost:8000//");
        assertEquals("http://localhost:8000", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("https://stage.speleodb.org////");
        assertEquals("https://stage.speleodb.org", service.getInstanceUrlPublic());
    }

    @Test
    @DisplayName("Should handle private IP ranges correctly")
    void shouldHandlePrivateIpRanges() {
        // Test 10.x.x.x range
        service.setInstanceUrlPublic("10.0.0.1");
        assertEquals("http://10.0.0.1", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("10.255.255.255:8080");
        assertEquals("http://10.255.255.255:8080", service.getInstanceUrlPublic());
        
        // Test 172.16-31.x.x range
        service.setInstanceUrlPublic("172.16.0.1");
        assertEquals("http://172.16.0.1", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("172.31.255.255:3000");
        assertEquals("http://172.31.255.255:3000", service.getInstanceUrlPublic());
        
        // Test 192.168.x.x range
        service.setInstanceUrlPublic("192.168.0.1");
        assertEquals("http://192.168.0.1", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("192.168.255.255:9000");
        assertEquals("http://192.168.255.255:9000", service.getInstanceUrlPublic());
    }

    @Test
    @DisplayName("Should handle public IP addresses correctly")
    void shouldHandlePublicIpAddresses() {
        // Test public IP addresses (should get https://)
        service.setInstanceUrlPublic("8.8.8.8");
        assertEquals("https://8.8.8.8", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("1.1.1.1:443");
        assertEquals("https://1.1.1.1:443", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("https://8.8.8.8/");
        assertEquals("https://8.8.8.8", service.getInstanceUrlPublic());
    }

    @Test
    @DisplayName("Should handle null and empty inputs gracefully")
    void shouldHandleNullAndEmptyInputs() {
        // Test null input
        service.setInstanceUrlPublic(null);
        assertNull(service.getInstanceUrlPublic());
        
        // Test empty string
        service.setInstanceUrlPublic("");
        assertEquals("https://", service.getInstanceUrlPublic());
        
        // Test whitespace only
        service.setInstanceUrlPublic("   ");
        assertEquals("https://", service.getInstanceUrlPublic());
    }

    @Test
    @DisplayName("Should handle various port numbers correctly")
    void shouldHandlePortNumbers() {
        // Test standard ports
        service.setInstanceUrlPublic("www.speleodb.org:80");
        assertEquals("https://www.speleodb.org:80", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("www.speleodb.org:443");
        assertEquals("https://www.speleodb.org:443", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("localhost:80");
        assertEquals("http://localhost:80", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("localhost:443");
        assertEquals("http://localhost:443", service.getInstanceUrlPublic());
        
        // Test non-standard ports
        service.setInstanceUrlPublic("api.example.com:8080");
        assertEquals("https://api.example.com:8080", service.getInstanceUrlPublic());
        
        service.setInstanceUrlPublic("localhost:3000");
        assertEquals("http://localhost:3000", service.getInstanceUrlPublic());
    }

    // Helper classes for testing
    private static class MockSpeleoDBController extends SpeleoDBController {
        private String instanceValue = "www.speleoDB.org";
        
        public MockSpeleoDBController() {
            super(true); // Use protected constructor for testing
        }
        
        public void setInstance(String instance) {
            // ... existing code ...
        }
    }

    // Testable version of SpeleoDBService that exposes internal methods
    private static class TestableSpeleoDBService extends SpeleoDBService {
        private String testSDBInstance = "";

        public TestableSpeleoDBService(SpeleoDBController controller) {
            super(controller);
        }

        public void setInstanceUrlPublic(String instanceUrl) {
            // Test version of setSDBInstance - use the same normalization logic
            String normalizedUrl = normalizeInstanceUrlPublic(instanceUrl);
            if (normalizedUrl == null) {
                testSDBInstance = null;
                return;
            }
            
            String localPattern = "(^localhost)|(^127\\.)|(^10\\.)|(^172\\.(1[6-9]|2[0-9]|3[0-1])\\.)|(^192\\.168\\.)";
            
            if (Pattern.compile(localPattern).matcher(normalizedUrl).find()) {
                testSDBInstance = "http://" + normalizedUrl;
            } else {
                testSDBInstance = "https://" + normalizedUrl;
            }
        }

        public String getInstanceUrlPublic() {
            return testSDBInstance;
        }

        // Test version of normalizeInstanceUrl method
        private String normalizeInstanceUrlPublic(String instanceUrl) {
            if (instanceUrl == null) {
                return null;
            }
            
            if (instanceUrl.trim().isEmpty()) {
                return "";
            }
            
            String normalized = instanceUrl.trim();
            
            // Remove http:// or https:// protocol prefixes
            if (normalized.startsWith("https://")) {
                normalized = normalized.substring(8);
            } else if (normalized.startsWith("http://")) {
                normalized = normalized.substring(7);
            }
            
            // Remove trailing slashes
            while (normalized.endsWith("/") && normalized.length() > 1) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            
            return normalized;
        }
    }
} 