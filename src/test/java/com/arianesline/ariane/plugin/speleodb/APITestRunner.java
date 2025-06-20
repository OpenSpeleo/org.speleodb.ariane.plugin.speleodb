package com.arianesline.ariane.plugin.speleodb;

import java.time.Duration;

import jakarta.json.JsonArray;

/**
 * Simple API test runner for basic connectivity testing
 * Provides quick verification that SpeleoDB API configuration is working
 * FAILS IMMEDIATELY if .env file is missing or invalid!
 */
public class APITestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== SpeleoDB API Test Runner ===");
        System.out.println("Starting API test suite ...");
        System.out.println();
        
        try {
            // CRITICAL: Validate environment configuration IMMEDIATELY
            // This will throw a clear RuntimeException if .env file is missing or invalid
            TestEnvironmentConfig.printConfigStatus();
            
            // Check if API testing is enabled
            if (!TestEnvironmentConfig.isApiTestEnabled()) {
                System.out.println("❌ API testing is disabled in configuration.");
                System.out.println("💡 Set API_TEST_ENABLED=true in your .env file to enable testing.");
                System.exit(1);
            }
            
            // Check if required configuration is present
            if (!TestEnvironmentConfig.hasRequiredConfig()) {
                System.out.println("❌ Required configuration is missing or invalid.");
                System.out.println("💡 Please check your .env file configuration.");
                System.exit(1);
            }
            
            System.out.println("✅ Configuration looks good. Running tests ...");
            System.out.println();
            
            // Initialize service
            SpeleoDBController mockController = new SpeleoDBController();
            SpeleoDBService service = new SpeleoDBService(mockController);
            
            // Run basic connectivity test
            runBasicConnectivityTest(service);
            
            System.out.println();
            System.out.println("🎉 Basic connectivity test PASSED!");
            System.out.println("Your SpeleoDB API configuration is working correctly.");
            
        } catch (RuntimeException e) {
            System.err.println();
            System.err.println("💥 CRITICAL ERROR: API Test Runner failed!");
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println("🔧 Please fix the configuration errors above and try again.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println();
            System.err.println("💥 UNEXPECTED ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Run basic connectivity test
     */
    private static void runBasicConnectivityTest(SpeleoDBService service) throws Exception {
        System.out.println("Running basic connectivity test ...");
        
        // Test authentication
        System.out.print("  Testing authenticatio ... ");
        long authStart = System.currentTimeMillis();
        
        String oauthToken = TestEnvironmentConfig.get(TestEnvironmentConfig.SPELEODB_OAUTH_TOKEN);
        String email = TestEnvironmentConfig.get(TestEnvironmentConfig.SPELEODB_EMAIL);
        String password = TestEnvironmentConfig.get(TestEnvironmentConfig.SPELEODB_PASSWORD);
        String instanceUrl = TestEnvironmentConfig.get(TestEnvironmentConfig.SPELEODB_INSTANCE_URL);
        
        if (oauthToken != null && !oauthToken.isEmpty()) {
            service.authenticate(null, null, oauthToken, instanceUrl);
        } else {
            service.authenticate(email, password, null, instanceUrl);
        }
        
        long authTime = System.currentTimeMillis() - authStart;
        System.out.println("✅ PASSED (" + authTime + "ms)");
        
        // Test project list API
        System.out.print("  Testing project list API ... ");
        long listStart = System.currentTimeMillis();
        
        JsonArray projects = service.listProjects();
        
        long listTime = System.currentTimeMillis() - listStart;
        System.out.println("✅ PASSED (" + listTime + "ms)");
        System.out.println("    Found " + projects.size() + " projects");
        
        // Logout
        service.logout();
        System.out.println("  ✅ Logged out successfully");
    }
    
    /**
     * Format duration for human-readable output
     */
    private static String formatDuration(Duration duration) {
        long millis = duration.toMillis();
        
        if (millis >= 1000) {
            return String.format("%.2fs", millis / 1000.0);
        } else {
            return String.format("%dms", millis);
        }
    }
} 