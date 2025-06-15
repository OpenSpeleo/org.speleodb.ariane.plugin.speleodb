package com.arianesline.ariane.plugin.speleodb;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SpeleoDBAccessLevel enum.
 * Tests enum values, ordering, and basic enum operations.
 */
@DisplayName("SpeleoDB Access Level Tests")
class SpeleoDBAccessLevelTest {

    @Nested
    @DisplayName("Enum Values")
    class EnumValuesTests {
        
        @Test
        @DisplayName("Should have three access levels")
        void shouldHaveThreeAccessLevels() {
            SpeleoDBAccessLevel[] levels = SpeleoDBAccessLevel.values();
            assertThat(levels).hasSize(3);
        }
        
        @Test
        @DisplayName("Should contain ADMIN access level")
        void shouldContainAdminAccessLevel() {
            assertThat(SpeleoDBAccessLevel.ADMIN)
                .isNotNull()
                .hasToString("ADMIN");
        }
        
        @Test
        @DisplayName("Should contain READ_AND_WRITE access level")
        void shouldContainReadAndWriteAccessLevel() {
            assertThat(SpeleoDBAccessLevel.READ_AND_WRITE)
                .isNotNull()
                .hasToString("READ_AND_WRITE");
        }
        
        @Test
        @DisplayName("Should contain READ_ONLY access level")
        void shouldContainReadOnlyAccessLevel() {
            assertThat(SpeleoDBAccessLevel.READ_ONLY)
                .isNotNull()
                .hasToString("READ_ONLY");
        }
    }
    
    @Nested
    @DisplayName("Enum Operations")
    class EnumOperationsTests {
        
        @Test
        @DisplayName("Should support valueOf operation")
        void shouldSupportValueOfOperation() {
            assertThat(SpeleoDBAccessLevel.valueOf("ADMIN"))
                .isEqualTo(SpeleoDBAccessLevel.ADMIN);
            assertThat(SpeleoDBAccessLevel.valueOf("READ_AND_WRITE"))
                .isEqualTo(SpeleoDBAccessLevel.READ_AND_WRITE);
            assertThat(SpeleoDBAccessLevel.valueOf("READ_ONLY"))
                .isEqualTo(SpeleoDBAccessLevel.READ_ONLY);
        }
        
        @Test
        @DisplayName("Should support ordinal values")
        void shouldSupportOrdinalValues() {
            assertThat(SpeleoDBAccessLevel.ADMIN.ordinal()).isEqualTo(0);
            assertThat(SpeleoDBAccessLevel.READ_AND_WRITE.ordinal()).isEqualTo(1);
            assertThat(SpeleoDBAccessLevel.READ_ONLY.ordinal()).isEqualTo(2);
        }
        
        @Test
        @DisplayName("Should support name() method")
        void shouldSupportNameMethod() {
            assertThat(SpeleoDBAccessLevel.ADMIN.name()).isEqualTo("ADMIN");
            assertThat(SpeleoDBAccessLevel.READ_AND_WRITE.name()).isEqualTo("READ_AND_WRITE");
            assertThat(SpeleoDBAccessLevel.READ_ONLY.name()).isEqualTo("READ_ONLY");
        }
        
        @Test
        @DisplayName("Should support compareTo operation")
        void shouldSupportCompareToOperation() {
            assertThat(SpeleoDBAccessLevel.ADMIN.compareTo(SpeleoDBAccessLevel.READ_ONLY))
                .isLessThan(0);
            assertThat(SpeleoDBAccessLevel.READ_ONLY.compareTo(SpeleoDBAccessLevel.ADMIN))
                .isGreaterThan(0);
            assertThat(SpeleoDBAccessLevel.READ_AND_WRITE.compareTo(SpeleoDBAccessLevel.READ_AND_WRITE))
                .isEqualTo(0);
        }
    }
    
    @Nested
    @DisplayName("Permission Level Semantics")
    class PermissionSemanticsTests {
        
        @Test
        @DisplayName("Should maintain proper permission hierarchy")
        void shouldMaintainProperPermissionHierarchy() {
            // ADMIN should be the highest level (ordinal 0)
            assertThat(SpeleoDBAccessLevel.ADMIN.ordinal())
                .isLessThan(SpeleoDBAccessLevel.READ_AND_WRITE.ordinal());
            
            // READ_AND_WRITE should be higher than READ_ONLY
            assertThat(SpeleoDBAccessLevel.READ_AND_WRITE.ordinal())
                .isLessThan(SpeleoDBAccessLevel.READ_ONLY.ordinal());
        }
        
        @Test
        @DisplayName("Should allow checking if level allows writing")
        void shouldAllowCheckingIfLevelAllowsWriting() {
            // This test demonstrates how the enum could be used for permission checks
            assertThat(allowsWriting(SpeleoDBAccessLevel.ADMIN)).isTrue();
            assertThat(allowsWriting(SpeleoDBAccessLevel.READ_AND_WRITE)).isTrue();
            assertThat(allowsWriting(SpeleoDBAccessLevel.READ_ONLY)).isFalse();
        }
        
        @Test
        @DisplayName("Should allow checking if level allows admin operations")
        void shouldAllowCheckingIfLevelAllowsAdminOperations() {
            assertThat(allowsAdmin(SpeleoDBAccessLevel.ADMIN)).isTrue();
            assertThat(allowsAdmin(SpeleoDBAccessLevel.READ_AND_WRITE)).isFalse();
            assertThat(allowsAdmin(SpeleoDBAccessLevel.READ_ONLY)).isFalse();
        }
    }
    
    // Helper methods to simulate permission checking logic
    private boolean allowsWriting(SpeleoDBAccessLevel level) {
        return level != SpeleoDBAccessLevel.READ_ONLY;
    }
    
    private boolean allowsAdmin(SpeleoDBAccessLevel level) {
        return level == SpeleoDBAccessLevel.ADMIN;
    }
} 