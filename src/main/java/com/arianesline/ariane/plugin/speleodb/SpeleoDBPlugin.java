package com.arianesline.ariane.plugin.speleodb;


import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.arianesline.ariane.plugin.api.DataServerCommands;
import com.arianesline.ariane.plugin.api.DataServerPlugin;
import com.arianesline.ariane.plugin.api.PluginInterface;
import com.arianesline.ariane.plugin.api.PluginType;
import com.arianesline.ariane.plugin.speleodb.SpeleoDBConstants.DEBUG;
import com.arianesline.ariane.plugin.speleodb.SpeleoDBConstants.NETWORK;
import com.arianesline.ariane.plugin.speleodb.SpeleoDBConstants.SHUTDOWN;
import com.arianesline.cavelib.api.CaveSurveyInterface;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class SpeleoDBPlugin implements DataServerPlugin {

    public static final int TIMEOUT = NETWORK.DEFAULT_TIMEOUT_MILLIS;
    private final StringProperty commandProperty = new SimpleStringProperty();
    private CaveSurveyInterface survey;
    private File surveyFile;
    private final AtomicBoolean lock = new AtomicBoolean(false);
    /* Executor Service for Background Tasks */
    public final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // Mark as daemon thread to allow JVM shutdown
        t.setName(DEBUG.SPELEODB_WORKER_THREAD_NAME);
        return t;
    });
    
    // Reference to the active controller for shutdown handling
    private SpeleoDBController activeController = null;
    private Stage pluginStage = null;
    
    /* Pre-warmed UI Component */
    private Alert preWarmedShutdownModal = null;
    
    // Fast button types to avoid repeated instantiation
    private static final ButtonType FAST_RELEASE_LOCK = new ButtonType(SHUTDOWN.BUTTON_YES_RELEASE_LOCK);
    private static final ButtonType FAST_KEEP_LOCK = new ButtonType(SHUTDOWN.BUTTON_NO_KEEP_LOCK);

    @Override
    public synchronized File getSurveyFile() {
        return surveyFile;
    }

    @Override
    public synchronized void setSurveyFile(File file) {
        surveyFile = file;

    }

    @Override
    public StringProperty getCommandProperty() {
        return commandProperty;
    }

    @Override
    public synchronized CaveSurveyInterface getSurvey() {
        return survey;
    }

    @Override
    public synchronized void setSurvey(CaveSurveyInterface survey) {
        this.survey = survey;
        lock.set(false);
    }

    /**
     * Pre-warms the shutdown modal system to eliminate first-time display lag.
     * Called during plugin initialization to prepare JavaFX Alert system.
     */
    private void preWarmShutdownModal() {
        Platform.runLater(() -> {
            if (preWarmedShutdownModal == null) {
                preWarmedShutdownModal = new Alert(Alert.AlertType.CONFIRMATION);
                preWarmedShutdownModal.setTitle(SHUTDOWN.DIALOG_TITLE);
                preWarmedShutdownModal.setHeaderText(SHUTDOWN.DIALOG_HEADER);
                preWarmedShutdownModal.setContentText("");
                preWarmedShutdownModal.getButtonTypes().setAll(FAST_RELEASE_LOCK, FAST_KEEP_LOCK);
            }
        });
    }

    /**
     * Shows a confirmation dialog asking the user if they want to release the project lock before shutdown.
     * OPTIMIZED: Uses pre-warmed modal + pre-created buttons for instant display.
     * 
     * @param projectName the name of the project that has an active lock
     * @return true if the user wants to release the lock, false otherwise
     */
    public boolean showShutdownConfirmation(String projectName) {
        // Use pre-warmed modal for maximum speed, fallback to new modal if needed
        Alert alert = (preWarmedShutdownModal != null) ? preWarmedShutdownModal : new Alert(Alert.AlertType.CONFIRMATION);
        
        // Configure the modal (minimal operations for speed)
        if (alert != preWarmedShutdownModal) {
            alert.setTitle(SHUTDOWN.DIALOG_TITLE);
            alert.setHeaderText(SHUTDOWN.DIALOG_HEADER);
            alert.getButtonTypes().setAll(FAST_RELEASE_LOCK, FAST_KEEP_LOCK);
        }
        
        // Build message efficiently using StringBuilder with pre-allocated capacity
        StringBuilder message = new StringBuilder(200);
        message.append(SHUTDOWN.MESSAGE_PREFIX)
               .append(projectName)
               .append(SHUTDOWN.MESSAGE_SUFFIX)
               .append(SHUTDOWN.MESSAGE_OPTIONS);
        
        alert.setContentText(message.toString());
        
        // Show and get result (optimized path)
        return alert.showAndWait()
                   .map(response -> response == FAST_RELEASE_LOCK)
                   .orElse(false);
    }

    /**
     * Gets or creates the single shared controller instance.
     * Uses lazy initialization to ensure only one controller exists.
     */
    private SpeleoDBController getOrCreateController() {
        if (activeController == null) {
            activeController = new SpeleoDBController();
            activeController.parentPlugin = this;
        }
        return activeController;
    }
    
    /**
     * Loads the FXML UI using the shared controller instance.
     * This ensures the same controller is used whether called from showUI() or getUINode().
     */
    private Parent loadUIWithSharedController() {
        SpeleoDBController controller = getOrCreateController();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/SpeleoDB.fxml"));
        fxmlLoader.setController(controller);
        
        try {
            Parent root = fxmlLoader.load();
            
            // Pre-warm shutdown modal for instant display (only once)
            preWarmShutdownModal();
            
            return root;
        } catch (IOException e) {
            System.err.println("Error loading FXML for SpeleoDB UI: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void showUI() {
        Parent root = loadUIWithSharedController();
        if (root != null) {
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));
            stage.setTitle("SpeleoDB");
            stage.show();
        }
    }

    @Override
    public Node getUINode() {
        return loadUIWithSharedController();
    }

    @Override
    public Image getIcon() {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png")));
    }

    @Override
    public void closeUI() {
        // Plugin cleanup during application shutdown
        // NOTE: No confirmation dialogs here - they're handled by the window close handler
        // This prevents app crashes during the JavaFX shutdown sequence
        
        if (activeController != null) {
            // Just log the current state, don't show any dialogs
            if (activeController.hasActiveProjectLock()) {
                String projectName = activeController.getCurrentProjectName();
                activeController.logMessageFromPlugin("Application shutting down with active lock on: " + projectName);
                activeController.logMessageFromPlugin("Lock will be released automatically when connection times out.");
            }
            
            // Cleanup resources to prevent shutdown hangs
            activeController.cleanup();
        }
        
        // Clear references
        activeController = null;
        pluginStage = null;
        
        // Shutdown the executor service quickly during application shutdown
        try {
            executorService.shutdown(); // Disable new tasks from being submitted
            if (!executorService.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                // Cancel currently executing tasks
                executorService.shutdownNow();
                // Wait briefly for tasks to respond to being cancelled
                if (!executorService.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    System.err.println("SpeleoDB executor did not terminate cleanly");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }


    public void saveSurvey() {
        commandProperty.set(DataServerCommands.SAVE.name());
        commandProperty.set(DataServerCommands.DONE.name());
    }

    public void loadSurvey(File file) {
        lock.set(true);
        survey = null;
        surveyFile = file;
        commandProperty.set(DataServerCommands.LOAD.name());
        var start = LocalDateTime.now();
        while (lock.get() && Duration.between(start, LocalDateTime.now()).toMillis() < TIMEOUT) {
            try {
                Thread.sleep(10); // Yield CPU to prevent 100% CPU usage
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        lock.set(false);
        commandProperty.set(DataServerCommands.DONE.name());
    }

    @Override
    public PluginInterface getInterfaceType() {
        return PluginInterface.LEFT_TAB;
    }

    @Override
    public String getName() {
        return "SpeleoDB";
    }

    @Override
    public PluginType getType() {
        return PluginType.DATASERVER;
    }

    @Override
    public void showSettings() {

    }

}