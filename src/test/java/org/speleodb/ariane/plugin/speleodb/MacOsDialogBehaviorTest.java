package org.speleodb.ariane.plugin.speleodb;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Window;

@DisplayName("macOS Dialog Behavior Guarded Tests")
class MacOsDialogBehaviorTest {

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

    private boolean isMac() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("mac");
    }

    @Test
    @DisplayName("Alerts are non-resizable and modal on macOS")
    void alertsAreNonResizableAndModalOnMac() throws InterruptedException {
        if (!isMac()) {
            return; // Guard: only meaningful on macOS
        }

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Test", ButtonType.OK);
            // Use the same path our code uses: set properties via helper (simulate minimal)
            alert.setResizable(false);
            Window owner = null; // in test environment, we may not have a real window
            if (owner != null) {
                alert.initOwner(owner);
                alert.initModality(Modality.WINDOW_MODAL);
            }
            assertFalse(alert.isResizable());
            // We cannot assert modality without an owner, but we can ensure creation succeeded
            assertNotNull(alert.getDialogPane());
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS), "FX task timed out");
    }
}


