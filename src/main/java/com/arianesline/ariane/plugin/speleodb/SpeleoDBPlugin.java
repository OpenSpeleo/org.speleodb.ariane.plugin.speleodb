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

    public static final int TIMEOUT = 10000;
    private final StringProperty commandProperty = new SimpleStringProperty();
    private CaveSurveyInterface survey;
    private File surveyFile;
    private final AtomicBoolean lock = new AtomicBoolean(false);
    // Executor service for managing background tasks.
    final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // Reference to the active controller for shutdown handling
    private SpeleoDBController activeController = null;
    private Stage pluginStage = null;
    
    // Pre-created button types for shutdown dialog performance optimization
    private static final ButtonType FAST_RELEASE_LOCK = new ButtonType("Yes, Release Lock");
    private static final ButtonType FAST_KEEP_LOCK = new ButtonType("No, Keep Lock");
    
    // Pre-warmed shutdown dialog for instant display
    private Alert preWarmedShutdownModal = null;

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
                preWarmedShutdownModal.setTitle("Application Shutdown");
                preWarmedShutdownModal.setHeaderText("Project Lock Active");
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
    private boolean showShutdownConfirmation(String projectName) {
        // Use pre-warmed modal for maximum speed, fallback to new modal if needed
        Alert alert = (preWarmedShutdownModal != null) ? preWarmedShutdownModal : new Alert(Alert.AlertType.CONFIRMATION);
        
        // Configure the modal (minimal operations for speed)
        if (alert != preWarmedShutdownModal) {
            alert.setTitle("Application Shutdown");
            alert.setHeaderText("Project Lock Active");
            alert.getButtonTypes().setAll(FAST_RELEASE_LOCK, FAST_KEEP_LOCK);
        }
        
        // Build message efficiently using StringBuilder with pre-allocated capacity
        StringBuilder message = new StringBuilder(200);
        message.append("You have an active lock on project \"")
               .append(projectName)
               .append("\".\n\nDo you want to release the lock before closing the application?\n\n")
               .append("• Yes: Release lock (other users can edit)\n")
               .append("• No: Keep lock (will be released when connection times out)");
        
        alert.setContentText(message.toString());
        
        // Show and get result (optimized path)
        return alert.showAndWait()
                   .map(response -> response == FAST_RELEASE_LOCK)
                   .orElse(false);
    }

    @Override
    public void showUI() {
        SpeleoDBController controller = new SpeleoDBController();
        controller.parentPlugin = this;
        this.activeController = controller; // Keep reference for shutdown handling
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/SpeleoDB.fxml"));
        fxmlLoader.setController(controller);
        Parent root1 = null;
        try {
            root1 = fxmlLoader.load();

        } catch (IOException e) {
            System.err.println("Error loading FXML for SpeleoDB UI: " + e.getMessage());
            e.printStackTrace();
        }
        Stage stage = new Stage();
        stage.initStyle(StageStyle.DECORATED);
        stage.setScene(new Scene(root1));
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));
        stage.setTitle("SpeleoDB");
        stage.show();
        
        // Pre-warm shutdown modal for instant display
        preWarmShutdownModal();
    }

    @Override
    public Node getUINode() {
        SpeleoDBController controller = new SpeleoDBController();
        controller.parentPlugin = this;
        this.activeController = controller; // Keep reference for shutdown handling
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/SpeleoDB.fxml"));
        fxmlLoader.setController(controller);
        Parent root1 = null;
        try {
            root1 = fxmlLoader.load();

        } catch (IOException e) {
            System.err.println("Error loading FXML for SpeleoDB UI Node: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Pre-warm shutdown modal for instant display
        preWarmShutdownModal();
        
        return root1;
    }

    @Override
    public Image getIcon() {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png")));
    }

    @Override
    public void closeUI() {
        // Check if there's an active project with a lock before shutting down
        if (activeController != null && activeController.hasActiveProjectLock()) {
            String projectName = activeController.getCurrentProjectName();
            
            // Show confirmation dialog
            boolean shouldReleaseLock = showShutdownConfirmation(projectName);
            
            if (shouldReleaseLock) {
                try {
                    // Release the project lock before shutdown
                    activeController.releaseCurrentProjectLock();
                    activeController.logMessageFromPlugin("Project lock released during application shutdown.");
                } catch (Exception e) {
                    // Log error but continue with shutdown
                    activeController.logMessageFromPlugin("Error releasing project lock during shutdown: " + e.getMessage());
                }
            } else {
                activeController.logMessageFromPlugin("Application closing with active project lock. Lock will timeout automatically.");
            }
        }
        
        // Clear references
        activeController = null;
        pluginStage = null;
        
        // Shutdown the executor service
        executorService.shutdownNow();
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