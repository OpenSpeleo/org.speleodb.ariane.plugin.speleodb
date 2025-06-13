package com.arianesline.ariane.plugin.speleodb;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * JUnit 5 Test Suite for all SpeleoDB Plugin tests.
 * 
 * Run this class to execute all tests in the correct order.
 * Individual test classes can also be run independently.
 */
@Suite
@SuiteDisplayName("SpeleoDB Plugin Test Suite")
@SelectClasses({
    HTTPRequestMultipartBodyTest.class,
    SpeleoDBServiceTest.class,
    SpeleoDBControllerTest.class,
    SpeleoDBControllerStateTest.class,
    SpeleoDBPluginTest.class
})
class AllTestsRunner {
    // This class serves as a test suite runner
    // No additional code needed - JUnit 5 handles everything
} 