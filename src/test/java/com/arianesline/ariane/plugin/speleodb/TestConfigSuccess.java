package com.arianesline.ariane.plugin.speleodb;

/**
 * Simple demo to test successful configuration loading when .env file is present
 */
public class TestConfigSuccess {
    public static void main(String[] args) {
        System.out.println("=== Testing .env File Success Case ===");
        System.out.println("This should succeed and show configuration status ...");
        System.out.println();
        
        try {
            // This should now work since .env file exists
            TestEnvironmentConfig.printConfigStatus();
            System.out.println();
            System.out.println("✅ SUCCESS: Configuration loaded successfully!");
            
        } catch (Exception e) {
            System.out.println("❌ ERROR: Unexpected failure:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
} 