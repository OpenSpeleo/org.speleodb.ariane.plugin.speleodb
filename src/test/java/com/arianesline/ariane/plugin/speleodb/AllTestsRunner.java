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
@SuiteDisplayName("SpeleoDB Plugin Test Suite")
@SelectClasses({
    // Enum and data structure tests
    SpeleoDBAccessLevelTest.class,
    
    // Service layer tests
    HTTPRequestMultipartBodyTest.class,
    SpeleoDBServiceTest.class,
    SpeleoDBServiceSimpleTest.class,
    
    // Controller tests
    SpeleoDBControllerTest.class,
    SpeleoDBControllerStateTest.class,
    
    // Plugin tests
    SpeleoDBPluginTest.class,
    SpeleoDBPluginExtendedTest.class
})
class AllTestsRunner {
    // This class serves as a test suite runner
    // No additional code needed - JUnit 5 handles everything
} 