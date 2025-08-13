package org.speleodb.ariane.plugin.speleodb;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

@DisplayName("SpeleoDBModals Internal Styling Tests")
class SpeleoDBModalsInternalTest {

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

    @Test
    @DisplayName("applyMaterialDesignStyling should cap dialog sizes")
    void shouldCapDialogSizes() throws Exception {
        Method m = SpeleoDBModals.class.getDeclaredMethod("applyMaterialDesignStyling", DialogPane.class);
        m.setAccessible(true);

        final int[] values = new int[4];
        final boolean[] flags = new boolean[2];
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        Platform.runLater(() -> {
            Dialog<Void> d = new Dialog<>();
            DialogPane pane = d.getDialogPane();
            try {
                m.invoke(null, pane);
                values[0] = (int) pane.getMaxWidth();
                values[1] = (int) pane.getMaxHeight();
                flags[0] = pane.getMinWidth() > 0;
                flags[1] = pane.getMinHeight() > 0;
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        });

        latch.await(2, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(SpeleoDBConstants.DIMENSIONS.INFO_DIALOG_PREF_WIDTH + 100, values[0]);
        assertEquals(600, values[1]);
        assertTrue(flags[0]);
        assertTrue(flags[1]);
    }

    @Test
    @DisplayName("convertColorToRgba should convert hex and handle invalid input")
    void shouldConvertColorSafely() throws Exception {
        Method m = SpeleoDBModals.class.getDeclaredMethod("convertColorToRgba", String.class, double.class);
        m.setAccessible(true);

        String ok = (String) m.invoke(null, "#112233", 0.5);
        assertEquals("rgba(17,34,51,0.5)", ok);

        String bad = (String) m.invoke(null, "#ZZZZZZ", 0.5);
        assertEquals("rgba(0,0,0,0.5)", bad);

        String passthrough = (String) m.invoke(null, "blue", 0.5);
        assertEquals("blue", passthrough);
    }

    @Test
    @DisplayName("calculateTextWidth should scale with characters")
    void shouldCalculateTextWidth() throws Exception {
        Method m = SpeleoDBModals.class.getDeclaredMethod("calculateTextWidth", String.class, boolean.class);
        m.setAccessible(true);

        double width = (double) m.invoke(null, "Hello", true);
        // 5 * 11.0 = 55.0 for bold approximation
        assertEquals(55.0, width, 0.001);
    }
}


