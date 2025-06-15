package com.arianesline.ariane.plugin.speleodb;


import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import javafx.scene.control.ButtonBar.ButtonData;
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
    private volatile CountDownLatch loadCompletionLatch;
    
    // Executor service for managing background tasks.
    final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // Reference to the active controller for shutdown handling
    private SpeleoDBController activeController = null;
    private Stage pluginStage = null;

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
        // Signal that loading is complete
        if (loadCompletionLatch != null) {
            loadCompletionLatch.countDown();
        }
    }

    /**
     * Shows an error alert to the user when UI loading fails.
     * 
     * @param message the error message to display
     * @param exception the exception that caused the error
     */
    private void showUILoadError(String message, Exception exception) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("SpeleoDB Plugin Error");
            alert.setHeaderText("Failed to Load User Interface");
            alert.setContentText(message + "\n\nDetails: " + exception.getMessage());
            alert.showAndWait();
        });
    }

    /**
     * Creates a fallback UI when FXML loading fails.
     * 
     * @return a simple text node indicating the error
     */
    private Node createFallbackUI() {
        javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(
            "SpeleoDB Plugin failed to load properly.\nPlease check the logs for details."
        );
        errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 20; -fx-font-size: 14;");
        return errorLabel;
    }

    /**
     * Logs an error message. If controller is available, uses the controller's logging system,
     * otherwise falls back to console output.
     * 
     * @param message the message to log
     * @param exception the exception to log (optional)
     */
    private void logError(String message, Exception exception) {
        String fullMessage = message + (exception != null ? ": " + exception.getMessage() : "");
        
        if (activeController != null) {
            activeController.logMessageFromPlugin("ERROR: " + fullMessage);
        } else {
            System.err.println("SpeleoDBPlugin ERROR: " + fullMessage);
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * Creates the UI content with proper error handling.
     * 
     * @return the loaded UI content
     * @throws IOException if FXML loading fails
     */
    private Parent createUIContent() throws IOException {
        SpeleoDBController controller = new SpeleoDBController();
        controller.parentPlugin = this;
        this.activeController = controller;
        
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/SpeleoDB.fxml"));
        fxmlLoader.setController(controller);
        
        return fxmlLoader.load();
    }

    /**
     * Shows a confirmation dialog asking the user if they want to release the project lock before shutdown.
     * 
     * @param projectName the name of the project that has an active lock
     * @return true if the user wants to release the lock, false otherwise
     */
    private boolean showShutdownConfirmation(String projectName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Application Shutdown");
        alert.setHeaderText("Project Lock Active");
        alert.setContentText("You have an active lock on project \"" + projectName + "\".\n\n" +
                            "Do you want to release the lock before closing the application?\n\n" +
                            "• Yes: Release lock (other users can edit)\n" +
                            "• No: Keep lock (will be released when connection times out)");
        
        // Customize button text
        ButtonType yesButton = new ButtonType("Yes, Release Lock");
        ButtonType noButton = new ButtonType("No, Keep Lock");
        alert.getButtonTypes().setAll(yesButton, noButton);
        
        // Show dialog and wait for user response
        return alert.showAndWait()
                   .map(response -> response == yesButton)
                   .orElse(false);
    }

    @Override
    public void showUI() {
        try {
            Parent root = createUIContent();
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));
            stage.setTitle("SpeleoDB");
            stage.show();
            
        } catch (IOException e) {
            logError("Failed to load FXML for UI window", e);
            showUILoadError("The SpeleoDB plugin interface could not be loaded.", e);
            throw new IllegalStateException("UI initialization failed", e);
        }
    }

    @Override
    public Node getUINode() {
        try {
            return createUIContent();
            
        } catch (IOException e) {
            logError("Failed to load FXML for UI node", e);
            // For UI node, return a fallback instead of throwing an exception
            return createFallbackUI();
        }
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

    /**
     * Loads a survey file with proper synchronization instead of spinning wait.
     * 
     * @param file the survey file to load
     */
    public void loadSurvey(File file) {
        lock.set(true);
        survey = null;
        surveyFile = file;
        
        // Create a new latch for this load operation
        loadCompletionLatch = new CountDownLatch(1);
        commandProperty.set(DataServerCommands.LOAD.name());
        
        try {
            // Use proper synchronization instead of spinning wait
            if (!loadCompletionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                logError("Survey loading timed out after " + TIMEOUT + "ms", null);
                throw new TimeoutException("Survey loading timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Survey loading was interrupted", e);
            throw new RuntimeException("Survey loading interrupted", e);
        } catch (TimeoutException e) {
            logError("Survey loading timed out", e);
            throw new RuntimeException("Survey loading failed: timeout", e);
        } finally {
            lock.set(false);
            commandProperty.set(DataServerCommands.DONE.name());
        }
    }

    /**
     * Asynchronous version of loadSurvey for better performance.
     * 
     * @param file the survey file to load
     * @return CompletableFuture that completes when loading is done
     */
    public CompletableFuture<Void> loadSurveyAsync(File file) {
        return CompletableFuture.runAsync(() -> {
            loadSurvey(file);
        }, executorService);
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
        try {
            // Check if JavaFX Platform is available (not in headless environment)
            if (isJavaFXAvailable()) {
                // Create a simple settings dialog for SpeleoDB configuration
                Platform.runLater(() -> {
                    try {
                        showSettingsDialog();
                    } catch (Exception e) {
                        handleSettingsError(e);
                    }
                });
            } else {
                // Headless mode - just log settings information
                showSettingsHeadless();
            }
        } catch (Exception e) {
            handleSettingsError(e);
        }
    }
    
    /**
     * Shows the settings dialog in GUI mode.
     */
    private void showSettingsDialog() {
        Alert settingsDialog = new Alert(Alert.AlertType.INFORMATION);
        settingsDialog.setTitle("SpeleoDB Plugin Settings");
        settingsDialog.setHeaderText("SpeleoDB Configuration");
        
        // Create settings content
        StringBuilder settingsInfo = new StringBuilder();
        settingsInfo.append("Current Settings:\n\n");
        
        if (activeController != null && activeController.speleoDBService.isAuthenticated()) {
            settingsInfo.append("Status: Connected\n");
            settingsInfo.append("Instance: ").append(activeController.speleoDBService.getSDBInstance()).append("\n");
        } else {
            settingsInfo.append("Status: Disconnected\n");
            settingsInfo.append("Instance: Not connected\n");
        }
        
        settingsInfo.append("\nData Directory: ").append(SpeleoDBService.ARIANE_ROOT_DIR).append("\n");
        
        settingsInfo.append("\n• Use the main interface to configure connection settings");
        settingsInfo.append("\n• Preferences are automatically saved when you connect");
        settingsInfo.append("\n• Project files are stored in your home directory");
        
        settingsDialog.setContentText(settingsInfo.toString());
        
        // Add custom buttons
        ButtonType configureButton = new ButtonType("Open Configuration", ButtonData.OK_DONE);
        ButtonType closeButton = new ButtonType("Close", ButtonData.CANCEL_CLOSE);
        settingsDialog.getButtonTypes().setAll(configureButton, closeButton);
        
        // Handle button actions
        Optional<ButtonType> result = settingsDialog.showAndWait();
        if (result.isPresent() && result.get() == configureButton) {
            // Show the main UI for configuration
            showUI();
        }
        
        if (activeController != null) {
            activeController.logMessageFromPlugin("Settings dialog accessed");
        }
    }
    
    /**
     * Shows settings information in headless mode (console output).
     */
    private void showSettingsHeadless() {
        StringBuilder settingsInfo = new StringBuilder();
        settingsInfo.append("SpeleoDB Plugin Settings:\n");
        
        if (activeController != null && activeController.speleoDBService.isAuthenticated()) {
            settingsInfo.append("Status: Connected\n");
            settingsInfo.append("Instance: ").append(activeController.speleoDBService.getSDBInstance()).append("\n");
        } else {
            settingsInfo.append("Status: Disconnected\n");
            settingsInfo.append("Instance: Not connected\n");
        }
        
        settingsInfo.append("Data Directory: ").append(SpeleoDBService.ARIANE_ROOT_DIR).append("\n");
        
        if (activeController != null) {
            activeController.logMessageFromPlugin("Settings accessed (headless mode): " + settingsInfo.toString());
        } else {
            System.out.println("SpeleoDB Plugin: " + settingsInfo.toString());
        }
    }
    
    /**
     * Checks if JavaFX Platform is available (not in headless mode).
     */
    private boolean isJavaFXAvailable() {
        try {
            // Try to access Platform to see if JavaFX is available
            Platform.isImplicitExit();
            return true;
        } catch (Exception | Error e) {
            // JavaFX not available (headless mode or not initialized)
            return false;
        }
    }
    
    /**
     * Handles errors in settings display.
     */
    private void handleSettingsError(Exception e) {
        if (activeController != null) {
            activeController.logMessageFromPlugin("Error showing settings: " + e.getMessage());
        } else {
            System.err.println("SpeleoDB Plugin: Error showing settings: " + e.getMessage());
        }
    }

}