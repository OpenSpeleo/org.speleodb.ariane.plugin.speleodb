package com.arianesline.ariane.plugin.speleodb;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * JUnit 5 Test Suite for all SpeleoDB Plugin tests.
 * 
 * Run this class to execute all tests in the correct order.
 * Individual test classes can also be run independently.
 * 
 * Note: SpeleoDBControllerTest contains comprehensive JavaFX controller tests
 * but may require special JavaFX initialization in some environments.
 */
@Suite
@SuiteDisplayName("SpeleoDB Plugin Complete Test Suite")
@SelectClasses({
    // Enum and data structure tests
    SpeleoDBAccessLevelTest.class,
    
    // Service layer tests - Core functionality
    HTTPRequestMultipartBodyTest.class,
    SpeleoDBServiceTest.class,
    SpeleoDBServiceSimpleTest.class,
    
    // Service layer tests - Advanced coverage (FIXED)
    SpeleoDBServiceAdvancedTest.class,
    
    // Controller tests - Core functionality
    SpeleoDBControllerTest.class,
    SpeleoDBControllerStateTest.class,
    
    // Controller tests - Utility coverage (FIXED)
    SpeleoDBControllerUtilityTest.class,
    
    // Dialog tests
    NewProjectDialogTest.class,
    
    // Plugin tests
    SpeleoDBPluginTest.class,
    SpeleoDBPluginExtendedTest.class
})
public class AllTestsRunner {
    // This class serves as a test suite runner
    // No additional code needed - JUnit 5 handles everything
    
    /**
     * Fixed Issues:
     * - Removed overly complex mock implementations that caused compilation issues
     * - Simplified test logic to focus on actual coverage gaps
     * - Fixed import statements and proper Java conventions
     * - Used proper testable subclasses instead of complex mocking
     * - Added thread safety tests with reasonable concurrency levels
     * - Focused on actual utility methods and edge cases that improve coverage
     */
} 