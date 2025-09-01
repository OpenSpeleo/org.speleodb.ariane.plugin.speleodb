package org.speleodb.ariane.plugin.speleodb;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import javafx.application.Platform;

@DisplayName("Read-only popup stability tests")
class SpeleoDBReadOnlyPopupTest {

    @BeforeAll
    static void initFX() {
        try {
            if (!Platform.isFxApplicationThread()) {
                Platform.startup(() -> {});
                Thread.sleep(100);
            }
        } catch (Exception ignored) {
        }
    }
}


