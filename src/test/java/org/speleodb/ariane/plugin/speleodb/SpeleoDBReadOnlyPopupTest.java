package org.speleodb.ariane.plugin.speleodb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;

@DisplayName("Read-only popup stability tests")
class SpeleoDBReadOnlyPopupTest {

    @BeforeAll
    static void initFX() throws InterruptedException {
        try {
            if (!Platform.isFxApplicationThread()) {
                Platform.startup(() -> {});
                Thread.sleep(100);
            }
        } catch (IllegalStateException e) {
            if (!e.getMessage().contains("Toolkit already initialized")) {
                throw e;
            }
        }
    }

    @Test
    @DisplayName("Read-only access level is correctly identified")
    void readOnlyAccessLevelFromString() {
        SpeleoDBConstants.AccessLevel level = SpeleoDBConstants.AccessLevel.fromString("READ_ONLY");
        assertThat(level).isEqualTo(SpeleoDBConstants.AccessLevel.READ_ONLY);
    }

    @Test
    @DisplayName("Null permission defaults to READ_ONLY")
    void nullPermissionDefaultsToReadOnly() {
        SpeleoDBConstants.AccessLevel level = SpeleoDBConstants.AccessLevel.fromString(null);
        assertThat(level).isEqualTo(SpeleoDBConstants.AccessLevel.READ_ONLY);
    }
}
