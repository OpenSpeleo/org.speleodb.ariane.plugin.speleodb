package com.arianesline.ariane.plugin.speleodb;

/**
 * Test runner that executes all unit tests.
 */
public class AllTestsRunner {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Running All SpeleoDB Plugin Tests ===\n");
        
        try {
            // Run HTTPRequestMultipartBody tests
            System.out.println("1. Running HTTPRequestMultipartBody tests...");
            HTTPRequestMultipartBodyTest.main(args);
            System.out.println();
            
            // Run SpeleoDBService tests
            System.out.println("2. Running SpeleoDBService tests...");
            SpeleoDBServiceTest.main(args);
            System.out.println();
            
            // Run SpeleoDBController tests
            System.out.println("3. Running SpeleoDBController tests...");
            SpeleoDBControllerTest.main(args);
            System.out.println();
            
            // Run SpeleoDBController state tests
            System.out.println("4. Running SpeleoDBController state tests...");
            SpeleoDBControllerStateTest.main(args);
            System.out.println();
            
            System.out.println("=== ALL TESTS PASSED SUCCESSFULLY! ===");
            
        } catch (AssertionError e) {
            System.err.println("❌ Test assertion failed: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Test execution failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 