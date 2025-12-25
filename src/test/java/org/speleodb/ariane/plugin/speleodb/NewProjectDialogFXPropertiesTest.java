package org.speleodb.ariane.plugin.speleodb;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.DialogPane;

@DisplayName("NewProjectDialog FX Properties Tests")
class NewProjectDialogFXPropertiesTest {

    @BeforeAll
    static void initFX() {
        try {
            if (!Platform.isFxApplicationThread()) {
                Platform.startup(() -> {});
                // Allow the platform to finish booting
                Thread.sleep(100);
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Dialog should be non-resizable and size-capped")
    void dialogShouldBeNonResizableAndSizeCapped() throws InterruptedException {
        AtomicReference<NewProjectDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            ref.set(new NewProjectDialog());
            latch.countDown();
        });

        boolean started = latch.await(2, TimeUnit.SECONDS);
        assertEquals(true, started, "FX task timed out");

        NewProjectDialog dialog = ref.get();
        assertNotNull(dialog);
        assertFalse(dialog.isResizable(), "Dialog should be non-resizable");

        DialogPane pane = dialog.getDialogPane();
        assertNotNull(pane);
        assertEquals(SpeleoDBConstants.DIMENSIONS.DIALOG_PREF_WIDTH + 100, (int) pane.getMaxWidth());
        assertEquals(600, (int) pane.getMaxHeight());
    }
}
