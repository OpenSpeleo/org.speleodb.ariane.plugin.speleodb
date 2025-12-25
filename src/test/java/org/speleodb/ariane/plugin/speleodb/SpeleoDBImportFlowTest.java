package org.speleodb.ariane.plugin.speleodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
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

        // Assign a test plugin and ensure LOAD events set a dummy survey in tests
        testPlugin = new SpeleoDBPlugin();
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

        // Listen for LOAD command to record ordering and signal latch
        SpeleoDBPlugin orderingPlugin = new SpeleoDBPlugin();
        orderingPlugin.getCommandProperty().addListener((obs, o, n) -> {
            if (n != null && n.equals(com.arianesline.ariane.plugin.api.DataServerCommands.LOAD.name())) {
                assertThat(stage.get()).as("Upload should be done before load").isEqualTo(1);
                // Set survey to release async lock path
                orderingPlugin.setSurvey(new com.arianesline.cavelib.api.CaveSurveyInterface() {
                    @Override public String getExtraData() { return ""; }
                    @Override public void setExtraData(String data) {}
                    @Override public java.util.ArrayList<com.arianesline.cavelib.api.SurveyDataInterface> getSurveyDataInterface() { return new java.util.ArrayList<>(); }
                    @Override public String getDescription() { return ""; }
                    @Override public String getGeoCoding() { return ""; }
                    @Override public String getCaveName() { return ""; }
                    @Override public String getUnit() { return ""; }
                    @Override public void setUnit(String unit) {}
                    @Override public Boolean getUseMagneticAzimuth() { return false; }
                    @Override public int addStation(String sectionname, String explorer, java.time.LocalDate datetime, String nomstation,
                                              double direction, double length, double depthin, double depth, int fromid, int toid, String type, javafx.scene.paint.Color color,
                                              double longitude, double latitude,
                                              double up, double down, double left, double right, String comment, String profiletype, boolean islocked) { return 0; }
                });
                stage.set(2);
                loadedLatch.countDown();
            }
        });
        controller.parentPlugin = orderingPlugin;

        // Create a temp .tml file to import
        File tempTml = File.createTempFile("import-order", ".tml");
        tempTml.deleteOnExit();
        Files.writeString(tempTml.toPath(), "<tml/>");

        // Call private performImportUploadAndLoad via reflection
        Method method = SpeleoDBController.class.getDeclaredMethod("performImportUploadAndLoad", File.class, String.class);
        method.setAccessible(true);
        method.invoke(controller, tempTml, "test message");
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

        SpeleoDBPlugin detectingPlugin = new SpeleoDBPlugin();
        detectingPlugin.getCommandProperty().addListener((obs, o, n) -> {
            if (n != null && n.equals(com.arianesline.ariane.plugin.api.DataServerCommands.LOAD.name())) {
                stage.set(99); // should not be emitted on failure
                doneLatch.countDown();
            }
        });
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

        // Allow any FX tasks to run; ensure LOAD was not emitted
        Thread.sleep(200);
        assertThat(stage.get()).isNotEqualTo(99);
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
