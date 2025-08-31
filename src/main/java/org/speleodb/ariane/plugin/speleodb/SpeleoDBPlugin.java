package org.speleodb.ariane.plugin.speleodb;


import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.DEBUG;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.NETWORK;

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
    
    // ==================== CENTRALIZED LOGGING SYSTEM ====================
    
    /**
     * Centralized logger instance - used directly without wrapper methods
     */
    private static final SpeleoDBLogger logger = SpeleoDBLogger.getInstance();

    // ==================== PLUGIN IMPLEMENTATION ====================

    /**
     * Default constructor for SpeleoDBPlugin.
     */
    public SpeleoDBPlugin() {
    }

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
     * Shows a confirmation dialog asking the user if they want to release the project lock before shutdown.
     * 
     * @param projectName the name of the project that has an active lock
     * @return true if the user wants to release the lock, false otherwise
     */
    public boolean showShutdownConfirmation(String projectName) {
        // Deprecated behavior: always release without prompting. Keep method for compatibility.
        return true;
    }

    /**
     * Loads the FXML UI using the singleton controller instance.
     * This ensures the same controller is used whether called from showUI() or getUINode().
     */
    private Parent loadUIWithSharedController() {
        SpeleoDBController controller = SpeleoDBController.getInstance();
        controller.parentPlugin = this; // Set the parent plugin reference
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/SpeleoDB.fxml"));
        fxmlLoader.setController(controller);
        
        try {
            Parent root = fxmlLoader.load();
            
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
        
        SpeleoDBController controller = SpeleoDBController.getInstance();
        
        // Just log the current state, don't show any dialogs
        if (controller.hasActiveProjectLock()) {
            String projectName = controller.getCurrentProjectName();
            logger.info("Application shutting down with active lock on: " + projectName);
            logger.info("Lock will be released automatically when connection times out.");
        }
        
        // Cleanup resources to prevent shutdown hangs
        controller.cleanup();
        
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
        
        // Shutdown centralized logging system last
        logger.info("SpeleoDB Plugin shutting down");
        logger.shutdown();
    }


    public void saveSurvey() {
        commandProperty.set(DataServerCommands.SAVE.name());
        commandProperty.set(DataServerCommands.DONE.name());
    }

    public void loadSurvey(File file) {
        // Use the executor service to avoid blocking the calling thread
        executorService.execute(() -> {
            // Set up for loading on JavaFX thread
            Platform.runLater(() -> {
                lock.set(true);  // Set lock before starting
                survey = null;
                surveyFile = file;
                commandProperty.set(DataServerCommands.LOAD.name());
            });

            // Wait for the survey to be loaded (polling with timeout)
            var start = java.time.LocalDateTime.now();
            while (lock.get() && java.time.Duration.between(start, java.time.LocalDateTime.now()).toMillis() < TIMEOUT) {
                try {
                    Thread.sleep(50); // Check every 50ms instead of 10ms to reduce CPU usage
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Survey loading interrupted");
                    break;
                }
            }
            
            // Check if we timed out
            if (lock.get()) {
                logger.error("Timeout while loading survey: " + file.getName());
                lock.set(false); // Reset lock on timeout
            } else {
                logger.info("Survey loaded successfully: " + file.getName());
            }
            
            // Clean up on JavaFX thread
            Platform.runLater(() -> {
                commandProperty.set(DataServerCommands.DONE.name());
            });
        });
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
