package org.speleodb.ariane.plugin.speleodb;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;
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

    @Test
    @DisplayName("showReadOnlyPermissionPopup executes on FX thread without error")
    void permissionPopupRunsSafely() throws Exception {
        SpeleoDBController controller = SpeleoDBController.getInstance();

        JsonObject project = Json.createObjectBuilder()
                .add("name", "ReadOnly Project")
                .add("permission", "READ_ONLY")
                .build();

        Method m = SpeleoDBController.class.getDeclaredMethod("showReadOnlyPermissionPopup", JsonObject.class);
        m.setAccessible(true);

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            assertDoesNotThrow(() -> m.invoke(controller, project));
            latch.countDown();
        });
        assertTrue(latch.await(2, TimeUnit.SECONDS), "FX task timed out");
    }

    @Test
    @DisplayName("showLockFailurePopup executes on FX thread without error")
    void lockFailurePopupRunsSafely() throws Exception {
        SpeleoDBController controller = SpeleoDBController.getInstance();

        JsonObject project = Json.createObjectBuilder()
                .add("name", "Locked Project")
                .add("permission", "READ_ONLY")
                .add("active_mutex", Json.createObjectBuilder()
                        .add("user", "other@user.com")
                        .add("creation_date", "2024-01-15T10:30:00.000000"))
                .build();

        Method m = SpeleoDBController.class.getDeclaredMethod("showLockFailurePopup", JsonObject.class);
        m.setAccessible(true);

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            assertDoesNotThrow(() -> m.invoke(controller, project));
            latch.countDown();
        });
        assertTrue(latch.await(2, TimeUnit.SECONDS), "FX task timed out");
    }
}


