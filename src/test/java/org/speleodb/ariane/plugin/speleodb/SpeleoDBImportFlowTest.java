package org.speleodb.ariane.plugin.speleodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;

/**
 * Tests for Import from Local Disk flow focusing on ordering:
 * - Upload must occur BEFORE loading the project
 * - On upload failure, project must NOT be loaded
 */
class SpeleoDBImportFlowTest {

    @BeforeAll
    static void initFX() throws Exception {
        try {
            if (!Platform.isFxApplicationThread()) {
                Platform.startup(() -> {});
                Thread.sleep(100);
            }
        } catch (IllegalStateException e) {
            // Toolkit already initialized - this is fine in test suites
            if (!e.getMessage().contains("Toolkit already initialized")) {
                throw e;
            }
        }
    }

    private SpeleoDBController controller;
    private SpeleoDBPlugin testPlugin;
    private ProgressIndicator progress;
    private Button uploadBtn;

    @BeforeEach
    void setup() throws Exception {
        // Use a fresh controller instance via protected constructor for isolation
        controller = new SpeleoDBController(true);

        // Minimal UI nodes to prevent NPEs
        progress = new ProgressIndicator();
        uploadBtn = new Button();

        setPrivateField(controller, "serverProgressIndicator", progress);
        setPrivateField(controller, "uploadButton", uploadBtn);

        // Assign a test plugin with an executor service and overridable loadSurvey
        testPlugin = new SpeleoDBPlugin() {
            @Override
            public void loadSurvey(File file) {
                super.loadSurvey(file); // use existing signaling behavior
            }
        };
        controller.parentPlugin = testPlugin;

        // Set a current project
        JsonObject project = Json.createObjectBuilder()
            .add("id", "test-project-import")
            .add("name", "Import Flow Project")
            .add("permission", "ADMIN")
            .build();
        setPrivateField(controller, "currentProject", project);
    }

    @Test
    @DisplayName("Should upload before loading project on successful import")
    void shouldUploadBeforeLoadOnSuccess() throws Exception {
        AtomicInteger stage = new AtomicInteger(0); // 0 = start, 1 = uploaded, 2 = loaded
        CountDownLatch loadedLatch = new CountDownLatch(1);

        // Fake SpeleoDBService that records upload stage
        SpeleoDBService fakeService = new SpeleoDBService(controller) {
            @Override
            public void uploadProject(String message, jakarta.json.JsonObject project) {
                stage.compareAndSet(0, 1);
            }
        };
        setPrivateField(controller, "speleoDBService", fakeService);

        // Override plugin loadSurvey to record ordering and signal latch
        SpeleoDBPlugin orderingPlugin = new SpeleoDBPlugin() {
            @Override
            public void loadSurvey(File file) {
                assertThat(stage.get()).as("Upload should be done before loadSurvey").isEqualTo(1);
                stage.set(2);
                loadedLatch.countDown();
            }
        };
        controller.parentPlugin = orderingPlugin;

        // Create a temp .tml file to import
        File tempTml = File.createTempFile("import-order", ".tml");
        tempTml.deleteOnExit();
        Files.writeString(tempTml.toPath(), "<tml/>");

        // Call private performImportUploadAndLoad via reflection
        Method method = SpeleoDBController.class.getDeclaredMethod("performImportUploadAndLoad", File.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, tempTml, "test message");

        // Wait for FX thread post-upload load to run
        assertThat(loadedLatch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(stage.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should not load project when upload fails")
    void shouldNotLoadWhenUploadFails() throws Exception {
        AtomicInteger stage = new AtomicInteger(0); // 0 = start, 1 = uploaded, 99 = loaded (should not)
        CountDownLatch doneLatch = new CountDownLatch(1);

        SpeleoDBService failingService = new SpeleoDBService(controller) {
            @Override
            public void uploadProject(String message, jakarta.json.JsonObject project) throws Exception {
                throw new Exception("Simulated upload failure");
            }
        };
        setPrivateField(controller, "speleoDBService", failingService);

        SpeleoDBPlugin detectingPlugin = new SpeleoDBPlugin() {
            @Override
            public void loadSurvey(File file) {
                stage.set(99); // should not be called on failure
                doneLatch.countDown();
            }
        };
        controller.parentPlugin = detectingPlugin;

        File tempTml = File.createTempFile("import-fail", ".tml");
        tempTml.deleteOnExit();
        Files.writeString(tempTml.toPath(), "<tml/>");

        Method method = SpeleoDBController.class.getDeclaredMethod("performImportUploadAndLoad", File.class, String.class);
        method.setAccessible(true);

        try {
            method.invoke(controller, tempTml, "msg");
        } catch (Exception ignored) {
            // expected: wrapped exception due to upload failure triggers FX error handling
        }

        // Allow any FX tasks to run; ensure loadSurvey was not called
        Thread.sleep(200);
        assertThat(stage.get()).isNotEqualTo(99);
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}


