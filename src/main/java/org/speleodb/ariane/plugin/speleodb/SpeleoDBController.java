package org.speleodb.ariane.plugin.speleodb;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.ANIMATIONS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.AccessLevel;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.BUTTON_TYPES;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.DIALOGS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.DIMENSIONS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.HTTP_STATUS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.JSON_FIELDS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.MESSAGES;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.MISC;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.NETWORK;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.PATHS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.PREFERENCES;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.STYLES;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.SortMode;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.URLS;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Controller for managing the SpeleoDB user interface.
 * Delegates server communication to SpeleoDBService.
 */
public class SpeleoDBController implements Initializable {
    // Singleton instance - eagerly initialized
    private static final SpeleoDBController instance = new SpeleoDBController();
    
    /**
     * Gets the singleton instance of SpeleoDBController.
     * 
     * @return the singleton instance
     */
    public static SpeleoDBController getInstance() {
        return instance;
    }
    
    /**
     * Resets the singleton instance state.
     * Since we use eager initialization, we can't null the instance,
     * but we can reset its state for testing purposes.
     */
    protected static void resetInstance() {
        // Clear the controller state for tests
        instance.currentProject = null;
        instance.cachedProjectList = null;
        if (instance.speleoDBService != null) {
            instance.speleoDBService.logout();
        }
    }
    
    public SpeleoDBPlugin parentPlugin;
    public Button signupButton;
    public TextArea serverTextArea;

    // UI Components (Injected by FXML)
    @FXML
    AnchorPane speleoDBAnchorPane;
    @FXML
    private Button connectionButton;
    @FXML
    private Button unlockButton;
    @FXML
    private Button uploadButton;
    @FXML
    private ListView<Button> projectListView;
    @FXML
    private Button createNewProjectButton;
    @FXML
    private Button refreshProjectsButton;
    @FXML
    private Button sortByNameButton;
    @FXML
    private Button sortByDateButton;
    @FXML
    private PasswordField oauthtokenPasswordField;
    @FXML
    private PasswordField passwordPasswordField;
    @FXML
    public ProgressIndicator serverProgressIndicator;
    @FXML
    private TextArea pluginUILogArea;
    @FXML
    private TextField emailTextField;
    @FXML
    private TextField instanceTextField;
    @FXML
    private TextField uploadMessageTextField;
    @FXML
    private TitledPane aboutTitlePane;
    @FXML
    TitledPane actionsTitlePane;
    @FXML
    private TitledPane projectsTitlePane;
    @FXML
    private WebView aboutWebView;
    @FXML
    private Button resetButton;
    @FXML
    private Label versionLabel;

    // SpeleoDBService instance for handling server communication.
    private SpeleoDBService speleoDBService;

    // Note: Constants moved to SpeleoDBConstants class
    
    // Centralized logger instance
    private static final SpeleoDBLogger logger = SpeleoDBLogger.getInstance();

    // Internal Controller Data
    private JsonObject currentProject = null;
    
    // Sorting state
    private SortMode currentSortMode = SortMode.BY_NAME; // Default to sort by name
    
    // Store the original project data for sorting without API calls
    private JsonArray cachedProjectList = null;
    
    // Track running animations to stop them during cleanup
    private final List<Timeline> runningAnimations = new ArrayList<>();
    
    /**
     * Helper method to create and track Timeline animations for cleanup
     */
    private Timeline createTrackedTimeline(KeyFrame... keyFrames) {
        Timeline timeline = new Timeline(keyFrames);
        synchronized (runningAnimations) {
            runningAnimations.add(timeline);
        }
        timeline.setOnFinished(e -> {
            synchronized (runningAnimations) {
                runningAnimations.remove(timeline);
            }
        });
        return timeline;
    }
    


    /**
     * Shows a confirmation modal using the centralized modal system.
     * 
     * @param title the dialog title
     * @param message the main message to display
     * @param positiveText text for the positive button (e.g., "Yes")
     * @param negativeText text for the negative button (e.g., "No")
     * @return true if user clicked positive button, false otherwise
     */
    private boolean showConfirmationModal(String title, String message, String positiveText, String negativeText) {
        return SpeleoDBModals.showConfirmation(title, message, positiveText, negativeText);
    }

    /**
     * Private constructor for singleton pattern.
     * SpeleoDBService initialization moved to initialize() method.
     */
    private SpeleoDBController() {
        // Service initialization moved to initialize() to avoid 'this' escape
    }
    
    /**
     * Protected constructor for test subclasses only.
     * This allows test classes to extend SpeleoDBController for mocking
     * while maintaining the singleton pattern in production code.
     * 
     * @param testOnly marker parameter to distinguish from private constructor
     */
    protected SpeleoDBController(boolean testOnly) {
        // Service initialization moved to initialize() to avoid 'this' escape
        // This constructor is only for test subclasses
    }

    // ========================= UTILITY FUNCTIONS ========================= //

    /**
     * Appends a message directly to the UI log area.
     * This method should only be called from the JavaFX Application Thread.
     * Used by the centralized logging system in SpeleoDBPlugin.
     */
    public void appendToUILog(String message) {
        if (pluginUILogArea != null) {
            pluginUILogArea.appendText(message);
        }
    }



    /**
     * Shows a success animation overlay with customizable message.
     * 
     * @param message the success message to display (optional, defaults to "Success")
     */
    private void showSuccessAnimation(String message) {
        SpeleoDBTooltips.showSuccess(message);
    }

    /**
     * Shows an error animation overlay with customizable message.
     * Enhanced with a more prominent red ERROR styling.
     * 
     * @param message the error message to display (optional, defaults to "ERROR")
     */
    private void showErrorAnimation(String message) {
        SpeleoDBTooltips.showError(message);
    }

    /**
     * Shows an error animation with the default "ERROR" message.
     */
    private void showErrorAnimation() {
        showErrorAnimation(null);
    }

    /**
     * Checks if the provided project ID is consistent with the current file and updates it if needed.
     *
     * @param project A JsonObject representing the project metadata.
     */
    private void checkAndUpdateSpeleoDBId(JsonObject project) {
        parentPlugin.executorService.execute(() -> {
            try {

                String SDB_projectId = project.getString("id");
                String SDB_mainCaveFileId = parentPlugin.getSurvey().getExtraData();


                if (SDB_mainCaveFileId == null || SDB_mainCaveFileId.isEmpty()) {
                    logger.info("Adding SpeleoDB ID: " + SDB_projectId);
                    speleoDBService.updateFileSpeleoDBId(SDB_projectId);
                    parentPlugin.getSurvey().setExtraData(SDB_projectId);
                    return;
                }

                if (!SDB_mainCaveFileId.equals(SDB_projectId)) {
                    logger.info("Incoherent File ID detected.");
                    logger.info("\t- Previous Value: " + SDB_mainCaveFileId);
                    logger.info("\t- New Value: " + SDB_projectId);
                    parentPlugin.getSurvey().setExtraData(SDB_projectId);
                    logger.info("SpeleoDB ID updated successfully.");
                }


            } catch (Exception e) {
                logger.info("Error checking/updating SpeleoDB ID: " + e.getMessage());
            }
        });
    }

    // ===================== NETWORK ERROR DETECTION ======================= //
    
    /**
     * Safely extracts a meaningful error message from an exception.
     * Handles cases where getMessage() returns null by falling back to exception class name.
     * 
     * @param exception the exception to extract message from
     * @return a non-null error message
     */
    private String getSafeErrorMessage(Exception exception) {
        if (exception == null) return "Unknown error";
        
        String message = exception.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        
        // Fall back to exception class name if message is null or empty
        return exception.getClass().getSimpleName();
    }
    
    /**
     * Analyzes an exception to determine if it's a server offline error.
     * Detects various network connectivity issues that indicate the server is unreachable.
     * 
     * @param exception the exception to analyze
     * @return true if the error indicates the server is offline/unreachable
     */
    private boolean isServerOfflineError(Exception exception) {
        if (exception == null) return false;
        
        String message = exception.getMessage();
        if (message == null) message = "";
        message = message.toLowerCase();
        
        // Check exception type
        String exceptionType = exception.getClass().getSimpleName().toLowerCase();
        
        // Connection refused, host unreachable, network errors
        return message.contains("connection refused") ||
               message.contains("no route to host") ||
               message.contains("host unreachable") ||
               message.contains("network is unreachable") ||
               message.contains("connection reset") ||
               message.contains("unknown host") ||
               message.contains("name resolution failed") ||
               exceptionType.contains("connectexception") ||
               exceptionType.contains("unknownhostexception") ||
               exceptionType.contains("noroutetohostexception");
    }
    
    /**
     * Analyzes an exception to determine if it's a timeout error.
     * Detects various timeout scenarios during network operations.
     * 
     * @param exception the exception to analyze
     * @return true if the error indicates a timeout occurred
     */
    private boolean isTimeoutError(Exception exception) {
        if (exception == null) return false;
        
        String message = exception.getMessage();
        if (message == null) message = "";
        message = message.toLowerCase();
        
        // Check exception type
        String exceptionType = exception.getClass().getSimpleName().toLowerCase();
        
        // Various timeout conditions
        return message.contains("timed out") ||
               message.contains("timeout") ||
               message.contains("read timeout") ||
               message.contains("connect timeout") ||
               message.contains("operation timeout") ||
               exceptionType.contains("timeout") ||
               exceptionType.contains("sockettimeoutexception") ||
               exceptionType.contains("httptimeoutexception") ||
               exceptionType.contains("interruptedexception");
    }
    
    /**
     * Creates an appropriate error message based on the type of network error.
     * Provides user-friendly messages for different error scenarios.
     * 
     * @param exception the exception that occurred
     * @param operation the operation that was being performed (e.g., "Connection", "Upload")
     * @return a user-friendly error message
     */
    private String getNetworkErrorMessage(Exception exception, String operation) {
        if (isServerOfflineError(exception)) {
            return """
                   Can't reach server - Please check:
                   \u2022 Server is online and accessible
                   \u2022 Network connection is working
                   \u2022 Server URL is correct
                   \u2022 Firewall isn't blocking the connection""";
        } else if (isTimeoutError(exception)) {
            return """
                   Request timed out - Server may be:
                   \u2022 Overloaded or slow to respond
                   \u2022 Experiencing network issues
                   \u2022 Temporarily unavailable
                   Try again in a few moments""";
        } else {
            // Generic network error
            return operation + " failed: " + exception.getMessage();
        }
    }

    // ====================== INPUT VALIDATION METHODS ====================== //
    
    /**
     * Validates OAuth token format using regex pattern.
     * OAuth tokens should be exactly 40 hexadecimal characters (0-9, a-f).
     * 
     * @param token the OAuth token to validate
     * @return true if token matches the expected format, false otherwise
     */
    private boolean validateOAuthToken(String token) {
        if (token == null) {
            return false;
        }
        
        // Remove any whitespace and convert to lowercase for validation
        String cleanToken = token.trim().toLowerCase();
        
        // Regex pattern: exactly 40 hexadecimal characters
        String oauthPattern = "^[a-f0-9]{40}$";
        
        boolean isValid = cleanToken.matches(oauthPattern);
        
        if (!isValid) {
            logger.info("OAuth token format validation: FAILED - Expected 40 hex characters, got: " + 
                      cleanToken.length() + " characters");
        }
        
        return isValid;
    }

    // ==================== USER PREFERENCES' MANAGEMENT =================== //

    /**
     * Gets the preferences node for this controller.
     * Uses test-specific preferences when running in test mode to avoid affecting user preferences.
     */
    protected Preferences getPreferencesNode() {
        // Check if we're running in test mode using the constant from SpeleoDBConstants
        if (SpeleoDBConstants.TEST_MODE) {
            // Use test-specific preferences node to avoid affecting user's real preferences
            return Preferences.userRoot().node(PREFERENCES.TEST_PREFERENCES_NODE);
        } else {
            // Use normal preferences for production
            return Preferences.userNodeForPackage(SpeleoDBController.class);
        }
    }

    private void loadPreferences() {
        Preferences prefs = getPreferencesNode();
        emailTextField.setText(prefs.get(PREFERENCES.PREF_EMAIL, MISC.EMPTY_STRING));
        passwordPasswordField.setText(prefs.get(PREFERENCES.PREF_PASSWORD, MISC.EMPTY_STRING));
        oauthtokenPasswordField.setText(prefs.get(PREFERENCES.PREF_OAUTH_TOKEN, MISC.EMPTY_STRING));
        instanceTextField.setText(prefs.get(PREFERENCES.PREF_INSTANCE, PREFERENCES.DEFAULT_INSTANCE));
    }

    /**
     * Saves user preferences based on the current UI state.
     * Validates OAuth token format before saving to prevent invalid tokens from being persisted.
     * Always saves credentials.
     */
    private void savePreferences() {
        Preferences prefs = getPreferencesNode();

        // Save email and instance
        prefs.put(PREFERENCES.PREF_EMAIL, emailTextField.getText());
        prefs.put(PREFERENCES.PREF_INSTANCE, instanceTextField.getText());
            
        String oauthToken = oauthtokenPasswordField.getText();
        String password = passwordPasswordField.getText();

        if (oauthToken != null && !oauthToken.trim().isEmpty()) {
            // If OAuth token is provided, save it and remove password
            prefs.put(PREFERENCES.PREF_PASSWORD, password);
            
            if (validateOAuthToken(oauthToken)) {
                prefs.put(PREFERENCES.PREF_OAUTH_TOKEN, oauthToken);
                logger.info(MESSAGES.OAUTH_TOKEN_SAVED);
            } else {
                oauthtokenPasswordField.setText("");
                prefs.remove(PREFERENCES.PREF_OAUTH_TOKEN);
                logger.error(MESSAGES.OAUTH_TOKEN_INVALID_NOT_SAVED);
            }

        } else {
            // Remove OAuth token if field is empty
            prefs.remove(PREFERENCES.PREF_OAUTH_TOKEN);

            // Save password only if not empty, otherwise remove it
            if (password != null && !password.trim().isEmpty()) {
                prefs.put(PREFERENCES.PREF_PASSWORD, password);
                logger.info(MESSAGES.PASSWORD_SAVED);
            } else {
                // Don't save invalid token, but log the issue
                prefs.remove(PREFERENCES.PREF_PASSWORD);
                logger.error("Password fieldis empty.");
            }
        }
    }



    // ==================== UI INITIALIZATION FUNCTIONS ==================== //

    /**
     * Determines if the application is running in debug mode.
     * Checks multiple sources: debug properties file, system properties, and environment variables.
     */
    private boolean isDebugMode() {
        // First, check for debug.properties file in resources
        try {
            var debugPropsStream = getClass().getResourceAsStream("/debug.properties");
            if (debugPropsStream != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(debugPropsStream);
                if (Boolean.parseBoolean(props.getProperty("debug.mode", "false"))) {
                    return true;
                }
            }
        } catch (IOException e) {
            // If loading properties fails, continue with other checks
        }
        
        // Check system property
        if (Boolean.parseBoolean(System.getProperty("speleodb.debug.mode", "false"))) {
            return true;
        }

        return Boolean.parseBoolean(System.getenv("SPELEODB_DEBUG_MODE"));
    }

    /**
     * Sets up default UI configurations.
     */
    private void setupUI() {
        actionsTitlePane.setVisible(false);
        projectsTitlePane.setVisible(false);
        createNewProjectButton.setDisable(true); // Disabled until authenticated
        refreshProjectsButton.setDisable(true); // Disabled until authenticated
        serverProgressIndicator.setVisible(false);
        connectionButton.setText(DIALOGS.BUTTON_CONNECT);
        
        // Set prompt text for upload message field
        uploadMessageTextField.setPromptText(DIALOGS.PROMPT_UPLOAD_MESSAGE);
        
        // Initial state: CONNECT and SIGNUP buttons visible 50/50
        javafx.scene.layout.GridPane.setColumnSpan(connectionButton, 1); // Span only 1 column (45%)
        signupButton.setVisible(true);
        
        // Use localhost URL when in debug mode, production URL otherwise
        aboutWebView.getEngine().load(URLS.WEBVIEW);
        
        // Hide webview scrollbars whenever they appear using ListChangeListener
        aboutWebView.getChildrenUnmodifiable().addListener((javafx.collections.ListChangeListener.Change<? extends javafx.scene.Node> change) -> {
            java.util.Set<javafx.scene.Node> scrollBars = aboutWebView.lookupAll(".scroll-bar");
            for (javafx.scene.Node scroll : scrollBars) {
                scroll.setVisible(false);
            }
        });

        // Ensure the About pane is expanded by default in the Accordion
        // This must be done after all UI setup to override Accordion's default behavior
        Platform.runLater(() -> {
            aboutTitlePane.setExpanded(true);
        });

        // Configure TextArea without scrollbars using CSS
        pluginUILogArea.setWrapText(true);
        pluginUILogArea.getStyleClass().add("no-scrollbar-textarea");
        
        pluginUILogArea.textProperty().addListener((ObservableValue<?> observable, Object oldValue, Object newValue) -> {
            // This will scroll to the bottom - use Double.MIN_VALUE to scroll to the top
            pluginUILogArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    /**
     * Sets up keyboard shortcuts for the application.
     */
    private void setupKeyboardShortcuts() {
        // Create Ctrl+S (Windows/Linux) / Cmd+S (Mac) key combination
        // SHORTCUT_DOWN automatically maps to Ctrl on Windows/Linux and Cmd on Mac
        KeyCombination saveKeyCombination = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
        
        // Add key event filter to the main anchor pane
        speleoDBAnchorPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (saveKeyCombination.match(event)) {
                // Only handle the shortcut if user has an active project lock
                if (hasActiveProjectLock()) {
                    event.consume(); // Prevent default behavior

                    String projectName = currentProject.getString("name");
                    SpeleoDBModals.showSaveModal(projectName, this::uploadProjectWithMessage, speleoDBAnchorPane);
                }
                // If no active lock, let the key event pass through (don't consume it)
            }
        });
    }

    /**
     * Sets up the version display label.
     * Initializes the version label with the current plugin version from constants.
     */
    private void setupVersionDisplay() {
        if (versionLabel != null) {
            versionLabel.setText(SpeleoDBConstants.VERSION_DISPLAY);
        }
    }

    /**
     * Sets up early window close handling to intercept shutdown before application shutdown begins.
     * This ensures the confirmation dialog appears immediately when user clicks X, not during app shutdown.
     */
    private void setupShutdownHook() {
        
        // Wait for the scene to be available, then set up the window close handler
        Platform.runLater(() -> {
            if (speleoDBAnchorPane.getScene() != null && speleoDBAnchorPane.getScene().getWindow() != null) {
                javafx.stage.Stage stage = (javafx.stage.Stage) speleoDBAnchorPane.getScene().getWindow();
                
                // Early window close handler - intercept shutdown before JavaFX application shutdown
                stage.setOnCloseRequest(event -> {
                    // Check if we have an active project lock that needs confirmation
                    if (hasActiveProjectLock()) {
                        // Consume the event to prevent default close behavior
                        event.consume();
                        
                        // Handle lock release confirmation immediately
                        Platform.runLater(() -> {
                            try {
                                String projectName = getCurrentProjectName();
                                boolean shouldReleaseLock = parentPlugin.showShutdownConfirmation(projectName);
                                
                                if (shouldReleaseLock) {
                                    // User chose to release lock - do it now before shutdown
                                    try {
                                        releaseCurrentProjectLock();
                                        logger.info("Project lock released before application shutdown.");
                                    } catch (IOException | InterruptedException | URISyntaxException e) {
                                        logger.info("Error releasing project lock during shutdown: " + e.getMessage());
                                        // Continue with shutdown even if lock release fails
                                    }
                                }
                                
                                // Close the window directly instead of calling Platform.exit()
                                // This prevents deadlock during JavaFX shutdown
                                stage.close();
                                
                            } catch (Exception e) {
                                System.err.println("Error during shutdown confirmation: " + e.getMessage());
                                // On error, proceed with shutdown to avoid hanging
                                stage.close();
                            }
                        });
                    } else {
                        // No active lock - proceed with normal shutdown
                        // Let the default close behavior happen
                    }
                });
                
            } else {
                // Scene not ready yet, try again later
                Platform.runLater(this::setupShutdownHook);
            }
        });
    }

    /**
     * Initializes the controller, setting up default UI states and preferences.
     *
     * @param location  the location of the FXML file.
     * @param resources additional resources, such as localized strings.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        speleoDBService = new SpeleoDBService(this);
        
        // Connect this controller to the centralized logger for UI console integration
        logger.setUIController(this);
        
        // Initialize logging
        logger.debug("SpeleoDBController initializing");
        logger.info("Ariane version: " + SpeleoDBConstants.ARIANE_VERSION);
        
        setupUI();
        setupKeyboardShortcuts();
        setupVersionDisplay();
        setupShutdownHook();
        loadPreferences();
        
        logger.debug("SpeleoDBController initialization complete");
        
        // Initialize sorting button styles (default to sort by name)
        updateSortButtonStyles();
        
        // Pre-warm modal system for instant display
        SpeleoDBModals.preWarmModalSystem();
        
        // Schedule information popup
        scheduleInformationPopup();
        
        // Initialize tooltip system when scene is available
        Platform.runLater(() -> {
            if (speleoDBAnchorPane != null && speleoDBAnchorPane.getScene() != null) {
                SpeleoDBTooltips.initialize(speleoDBAnchorPane.getScene());
            }
        });
    }


    // ========================= UI EVENT FUNCTIONS ======================== //

    // ********************* Authentication Management ********************* //

    /**
     * Establishes a connection to SpeleoDB using the provided credentials.
     * Validates the OAuth token format before attempting connection.
     * Updates the UI state based on the connection result.
     */
    private void connectToSpeleoDB() {
        String email = emailTextField.getText();
        String password = passwordPasswordField.getText();
        String oauthToken = oauthtokenPasswordField.getText();
        String sdb_instance = instanceTextField.getText();

        // Validate that user has provided authentication credentials
        boolean hasOAuthToken = oauthToken != null && !oauthToken.trim().isEmpty();
        boolean hasEmailPassword = (email != null && !email.trim().isEmpty()) && 
                                  (password != null && !password.trim().isEmpty());
        
        if (!hasOAuthToken && !hasEmailPassword) {
            SpeleoDBModals.showError(
                "Invalid SpeleoDB Credentials", 
                """
                Please provide authentication credentials to connect to SpeleoDB.
                
                You must provide either:
                • An OAuth Token (40-character hex string)
                • Both Email and Password
                
                Please fill in the required fields and try again.
                """
            );
            return;
        }

        // Validate OAuth token format if provided
        if (hasOAuthToken) {
            if (!validateOAuthToken(oauthToken)) {
                SpeleoDBModals.showError(
                    "Invalid OAuth Token", 
                    """
                    OAuth token must be exactly 40 hexadecimal characters (0-9, a-f).
                    
                    Please check your token format and try again.
                    """
                );
                return;
            }
        }

        logger.info("Connecting to " + sdb_instance);
        serverProgressIndicator.setVisible(true);

        parentPlugin.executorService.execute(() -> {
            try {
                speleoDBService = new SpeleoDBService(this);
                speleoDBService.authenticate(email, password, oauthToken, sdb_instance);
                logger.info("Connected successfully.");
                
                // Always save preferences on successful connection
                savePreferences();

                Platform.runLater(() -> {
                    projectsTitlePane.setVisible(true);
                    projectsTitlePane.setExpanded(true);
                    createNewProjectButton.setDisable(false);
                    refreshProjectsButton.setDisable(false);
                    
                    // Update UI state for connected mode
                    connectionButton.setText("DISCONNECT");
                    javafx.scene.layout.GridPane.setColumnSpan(connectionButton, 3); // Span all 3 columns (100%)
                    signupButton.setVisible(false);
                    
                    // Actions pane will be shown only when a project is loaded (handled in downloadAndLoadProject)
                    
                    // Disable connection form fields while connected
                    setConnectionFormEnabled(false);
                    
                    serverProgressIndicator.setVisible(false);
                });

                listProjects();

            } catch (Exception e) {
                String errorMessage = getNetworkErrorMessage(e, "Connection");
                logger.info("Connection failed: " + getSafeErrorMessage(e));
                
                Platform.runLater(() -> {
                    if (isServerOfflineError(e)) {
                        showErrorAnimation("Can't reach server");
                        SpeleoDBModals.showError("Server Offline", errorMessage);
                    } else if (isTimeoutError(e)) {
                        showErrorAnimation("Request timed out");
                        SpeleoDBModals.showError("Connection Timeout", errorMessage);
                    } else {
                        showErrorAnimation("Connection Failed: " + getSafeErrorMessage(e));
                    }
                    
                    serverProgressIndicator.setVisible(false);
                });
            }
        });
    }

    /**
     * Disconnects from SpeleoDB and updates the UI state.
     * Clears the current project, updates button states, and hides project-related UI elements.
     */
    private void disconnectFromSpeleoDB() {
        String SDB_instance = speleoDBService != null ? speleoDBService.getSDBInstance() : "SpeleoDB";
        
        if (speleoDBService != null) {
            speleoDBService.logout();
        }
        
        currentProject = null;
        
        // Clear cached project list and UI
        cachedProjectList = null;
        projectListView.getItems().clear();
        
        // Update UI state for disconnected mode
        connectionButton.setText("CONNECT");
        javafx.scene.layout.GridPane.setColumnSpan(connectionButton, 1); // Span only 1 column (45%)
        signupButton.setVisible(true);
        
        actionsTitlePane.setVisible(false);
        projectsTitlePane.setVisible(false);
        createNewProjectButton.setDisable(true);
        refreshProjectsButton.setDisable(true);
        
        // Re-enable connection form fields
        setConnectionFormEnabled(true);
        
        // Clear only password and OAuth token from preferences
        Preferences prefs = getPreferencesNode();
        prefs.remove(PREFERENCES.PREF_PASSWORD);
        prefs.remove(PREFERENCES.PREF_OAUTH_TOKEN);
        
        // Clear only password and OAuth token from UI (leave email and instance)
        passwordPasswordField.clear();
        oauthtokenPasswordField.clear();
        
        logger.info("Disconnected from " + SDB_instance + " and cleared password/OAuth token");
    }
    
    /**
     * Enables or disables the connection form fields.
     * When connected, fields are disabled to prevent changes during active session.
     * When disconnected, fields are enabled to allow user input.
     * 
     * @param enabled true to enable the fields, false to disable them
     */
    private void setConnectionFormEnabled(boolean enabled) {
        emailTextField.setDisable(!enabled);
        passwordPasswordField.setDisable(!enabled);
        oauthtokenPasswordField.setDisable(!enabled);
        instanceTextField.setDisable(!enabled);
        resetButton.setDisable(!enabled);
    }

    /**
     * Sets the loading state for the entire UI, disabling/enabling all interactive elements.
     * This provides comprehensive visual feedback during loading operations.
     * 
     * @param loading true to disable UI elements (loading state), false to enable them
     */
    private void setUILoadingState(boolean loading) {
        // Project-related controls
        projectListView.setDisable(loading);
        createNewProjectButton.setDisable(loading);
        refreshProjectsButton.setDisable(loading);
        sortByNameButton.setDisable(loading);
        sortByDateButton.setDisable(loading);
        
        // Project action controls (only disable if they're currently enabled)
        if (loading) {
            uploadButton.setDisable(true);
            unlockButton.setDisable(true);
        } else {
            // Re-enable based on current project state
            boolean hasLock = hasActiveProjectLock();
            uploadButton.setDisable(!hasLock);
            unlockButton.setDisable(!hasLock);
        }
        
        // Connection controls (only if not authenticated)
        if (speleoDBService == null || !speleoDBService.isAuthenticated()) {
            connectionButton.setDisable(loading);
            setConnectionFormEnabled(!loading);
        }
        
        // Titled panes for visual feedback
        projectsTitlePane.setDisable(loading);
        actionsTitlePane.setDisable(loading);
    }
    
    /**
     * Resets the connection form to default values.
     * Clears email, password, and OAuth token fields, and resets instance to default.
     * Only available when not connected to prevent accidental data loss.
     */
    @FXML
    public void onResetConnectionForm(ActionEvent actionEvent) {
        // Only allow reset when not connected
        if (speleoDBService != null && speleoDBService.isAuthenticated()) {
            showErrorAnimation("Cannot reset form while connected");
            return;
        }
        
        // Show confirmation dialog
        boolean confirmed = showConfirmationModal(
            "Reset Connection Form",
            "This will clear all connection form fields and reset the instance to default.\n\nAre you sure you want to continue?",
            "Reset",
            "Cancel"
        );
        
        if (confirmed) {
            // Reset all form fields
            emailTextField.setText("");
            passwordPasswordField.setText("");
            oauthtokenPasswordField.setText("");
            instanceTextField.setText(PREFERENCES.DEFAULT_INSTANCE);
            
            // Clear all saved preferences when resetting form
            Preferences prefs = getPreferencesNode();
            prefs.remove(PREFERENCES.PREF_INSTANCE);
            prefs.remove(PREFERENCES.PREF_EMAIL);
            prefs.remove(PREFERENCES.PREF_PASSWORD);
            prefs.remove(PREFERENCES.PREF_OAUTH_TOKEN);
            
            logger.info("Connection form reset to defaults and cleared all preferences");
            showSuccessAnimation("Form reset successfully");
        }
    }

    /**
     * Handles the connection/disconnection to/from SpeleoDB.
     * Toggles between connecting and disconnecting states based on current authentication status.
     */
    @FXML
    public void onHandleAuthentication(ActionEvent actionEvent) throws URISyntaxException, IOException, InterruptedException {
        if (speleoDBService.isAuthenticated()) {
            disconnectFromSpeleoDB();
        } else {
            connectToSpeleoDB();
        }
    }


    // ************************* Project Management ************************ //

    // -------------------------- Project Creation ------------------------- //
    // Note: Once a project is created:
    //   - immediately acquire mutex
    //   - create an empty TML in Ariane, wait on "first save" to do first commit. 

    // -------------------------- Project Listing -------------------------- //

    /**
     * Creates a Button to represent a SpeleoDB project with specific actions.
     *
     * @param card        A VBox containing project details for display.
     * @param projectItem A JsonObject representing the project metadata.
     * @return A Button configured with the given card and actions.
     */
    private Button createProjectButton(VBox card, JsonObject projectItem) {
        // Initialize the button and set its graphic to the provided card.
        Button button = new Button();
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setGraphic(card);
        
        // Make the button scale with the ListView width
        button.setMaxWidth(Double.MAX_VALUE);
        button.prefWidthProperty().bind(projectListView.widthProperty().subtract(20)); // 20px for scrollbar/padding

        // Define the action handler for button clicks.
        button.setOnAction(event -> handleProjectCardClickAction(event, projectItem));

        // Store project metadata as user data for later retrieval.
        button.setUserData(projectItem);
        return button;
    }

    /**
     * Creates a UI card for a project.
     *
     * @param projectItem A JsonObject containing project metadata.
     * @return A VBox representing the project card.
     */
    private VBox createProjectCard(JsonObject projectItem) {
        VBox card = new VBox();

        /* Setting the name of the project */
        String name = projectItem.getString("name");
        Text nameText = new Text(name);
        nameText.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, Font.getDefault().getSize()));
        card.getChildren().add(nameText);
        card.getChildren().add(new Text(projectItem.getString("permission")));

        /* Setting the mutex state of the project */
        JsonValue mutex = projectItem.get("active_mutex");
        if (mutex.getValueType() == JsonValue.ValueType.NULL) {
            card.getChildren().add(new Text("Not Locked"));
        } else {
            JsonObject mutexObj = mutex.asJsonObject();
            card.getChildren().add(new Text("Locked"));
            card.getChildren().add(new Text("by " + mutexObj.getString("user")));

            String creationDate = mutexObj.getString("creation_date");
            LocalDateTime creationDateTime = LocalDateTime.parse(creationDate.substring(0, creationDate.lastIndexOf('.')));
            card.getChildren().add(new Text("on " + creationDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))));

            String modifiedDate = mutexObj.getString("modified_date");
            LocalDateTime modifiedDateTime = LocalDateTime.parse(modifiedDate.substring(0, modifiedDate.lastIndexOf('.')));
            card.getChildren().add(new Text("(mod) " + modifiedDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))));
        }
        
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    /**
     * Handles the response from the project listing request and updates the UI.
     *
     * @param projectList A JsonArray containing the list of projects.
     */
    private void handleProjectListResponse(JsonArray projectList) {
        logger.info("Project listing successful on " + speleoDBService.getSDBInstance());
        
        // Cache the project data for sorting without API calls
        cachedProjectList = projectList;
        
        // Use the shared method to rebuild the project list
        rebuildProjectListFromCache();
    }
    
    /**
     * Shared method to rebuild the project list from cached data.
     * Used by both refresh and sort operations to ensure consistent behavior.
     */
    private void rebuildProjectListFromCache() {
        if (cachedProjectList == null) {
            logger.info("No cached project data available");
            return;
        }
        
        Platform.runLater(() -> {
            // Clean up property bindings to prevent memory leaks
            for (Button button : projectListView.getItems()) {
                button.prefWidthProperty().unbind();
            }
            projectListView.getItems().clear();
            
            // Convert JsonArray to List for sorting
            List<JsonObject> projects = new ArrayList<>();
            for (JsonValue jsonValue : cachedProjectList) {
                projects.add(jsonValue.asJsonObject());
            }
            
            // Sort projects based on current sort mode
            if (currentSortMode == SortMode.BY_NAME) {
                projects.sort(Comparator.comparing(project -> 
                    project.getString("name", "").toLowerCase()));
                logger.info("Projects sorted by name (A-Z)");
            } else { // BY_DATE
                projects.sort(Comparator.comparing((JsonObject project) -> 
                    project.getString("modified_date", "")).reversed()); // Most recent first
                logger.info("Projects sorted by modified_date (newest first)");
            }
            
            // Create UI elements for sorted projects
            for (JsonObject projectItem : projects) {
                VBox card = createProjectCard(projectItem);
                Button button = createProjectButton(card, projectItem);
                projectListView.getItems().add(button);
            }
            
            // Update button styles to reflect current sort mode
            updateSortButtonStyles();
            
            logger.info("Project list rebuilt with " + projects.size() + " projects");
        });
    }

    private void listProjects() {
        logger.info("Listing Projects on " + speleoDBService.getSDBInstance());
        
        // Show projects loading indicator and disable UI
        Platform.runLater(() -> {
            serverProgressIndicator.setVisible(true);
            setUILoadingState(true);
        });

        parentPlugin.executorService.execute(() -> {
            try {
                JsonArray projectList = speleoDBService.listProjects();
                handleProjectListResponse(projectList);
            } catch (Exception e) {
                String errorMessage = getNetworkErrorMessage(e, "Project listing");
                logger.info("Failed to list projects: " + getSafeErrorMessage(e));
                
                if (isServerOfflineError(e)) {
                    showErrorAnimation("Can't reach server");
                    SpeleoDBModals.showError("Server Offline", errorMessage);
                } else if (isTimeoutError(e)) {
                    showErrorAnimation("Request timed out");
                    SpeleoDBModals.showError("Request Timeout", errorMessage);
                } else {
                    showErrorAnimation("Failed to Load Projects");
                }
            } finally {
                Platform.runLater(() -> {
                    serverProgressIndicator.setVisible(false);
                    setUILoadingState(false);
                });
            }
        });
    }
    
    /**
     * Handles the refresh projects button click event.
     * Refreshes the project listing with visual feedback to the user.
     */
    @FXML
    public void onRefreshProjects(ActionEvent actionEvent) {
        if (!speleoDBService.isAuthenticated()) {
            logger.info("Cannot refresh projects: Not authenticated");
            return;
        }
        
        logger.info("User requested project list refresh");
        
        // Provide visual feedback and disable UI
        refreshProjectsButton.setDisable(true);
        refreshProjectsButton.setText("Refreshing ...");
        serverProgressIndicator.setVisible(true);
        setUILoadingState(true);
        
        parentPlugin.executorService.execute(() -> {
            try {
                JsonArray projectList = speleoDBService.listProjects();
                handleProjectListResponse(projectList);
                Platform.runLater(() -> logger.info("Project list refreshed successfully"));
            } catch (Exception e) {
                String errorMessage = getNetworkErrorMessage(e, "Project refresh");
                logger.info("Failed to refresh projects: " + e.getMessage());
                
                if (isServerOfflineError(e)) {
                    showErrorAnimation("Can't reach server");
                    SpeleoDBModals.showError("Server Offline", errorMessage);
                } else if (isTimeoutError(e)) {
                    showErrorAnimation("Request timed out");
                    SpeleoDBModals.showError("Request Timeout", errorMessage);
                } else {
                    showErrorAnimation("Failed to Refresh Projects");
                }
            } finally {
                Platform.runLater(() -> {
                    serverProgressIndicator.setVisible(false);
                    setUILoadingState(false);
                    refreshProjectsButton.setDisable(false);
                    refreshProjectsButton.setText("Refresh Projects");
                });
            }
        });
    }
    
    /**
     * Handles the sort by name button click event.
     * Uses the shared rebuildProjectListFromCache method.
     */
    @FXML
    public void onSortByName(ActionEvent actionEvent) {
        if (!speleoDBService.isAuthenticated()) {
            logger.info("Cannot sort projects: Not authenticated");
            return;
        }
        
        logger.info("User requested sort by name");
        currentSortMode = SortMode.BY_NAME;
        rebuildProjectListFromCache();
    }
    
    /**
     * Handles the sort by date button click event.
     * Uses the shared rebuildProjectListFromCache method.
     */
    @FXML
    public void onSortByDate(ActionEvent actionEvent) {
        if (!speleoDBService.isAuthenticated()) {
            logger.info("Cannot sort projects: Not authenticated");
            return;
        }
        
        logger.info("User requested sort by date");
        currentSortMode = SortMode.BY_DATE;
        rebuildProjectListFromCache();
    }
    
    /**
     * Updates the visual style of sorting buttons based on current sort mode
     */
    private void updateSortButtonStyles() {
        if (currentSortMode == SortMode.BY_NAME) {
            sortByNameButton.getStyleClass().clear();
            sortByNameButton.getStyleClass().add("sort-button-active");
            sortByDateButton.getStyleClass().clear();
            sortByDateButton.getStyleClass().add("sort-button");
        } else {
            sortByNameButton.getStyleClass().clear();
            sortByNameButton.getStyleClass().add("sort-button");
            sortByDateButton.getStyleClass().clear();
            sortByDateButton.getStyleClass().add("sort-button-active");
        }
    }

    /**
     * Handles the create new project button click event.
     * Shows a dialog to collect project information.
     */
    @FXML
    public void onCreateNewProject(ActionEvent actionEvent) {
        if (!speleoDBService.isAuthenticated()) {
            logger.info("Cannot create new project: Not authenticated");
            return;
        }
        
        // Show the new project dialog
        NewProjectDialog dialog = new NewProjectDialog();
        var result = dialog.showAndWait();
        
        if (result.isPresent()) {
            NewProjectDialog.ProjectData projectData = result.get();
            logger.info("Creating new project: " + projectData.getName());
            
            // Disable the button while creating project and show loading state
            createNewProjectButton.setDisable(true);
            createNewProjectButton.setText("Creating ...");
            serverProgressIndicator.setVisible(true);
            setUILoadingState(true);
            
            parentPlugin.executorService.execute(() -> {
                try {
                    // Create the project via API
                    JsonObject createdProject = speleoDBService.createProject(
                        projectData.getName(),
                        projectData.getDescription(), 
                        projectData.getCountryCode(),
                        projectData.getLatitude(),
                        projectData.getLongitude()
                    );
                    
                    logger.info("Project '" + projectData.getName() + "' created successfully!");
                    logger.info("Project ID: " + createdProject.getString("id"));
                    
                    // Use centralized lock acquisition with UI integration
                    acquireProjectLockWithUI(createdProject, "project creation",
                        () -> {
                            // Success callback: Set up project files and load survey
                            parentPlugin.executorService.execute(() -> {
                                try {
                                    logger.info("Setting up new project files ...");
                                    
                                    // Create an empty TML file for the new project using shared service method
                                    String projectId = createdProject.getString("id");
                                    Path emptyTmlFile = speleoDBService.createEmptyTmlFileFromTemplate(projectId, projectData.getName());
                                    
                                    // Load the survey file and update SpeleoDB ID through normal flow
                                    parentPlugin.loadSurvey(emptyTmlFile.toFile());
                                    checkAndUpdateSpeleoDBId(createdProject);
                                    
                                    Platform.runLater(() -> {
                                        showSuccessAnimation("Project created and locked for editing!");
                                        listProjects();
                                    });
                                    
                                } catch (IOException e) {
                                    logger.info("Error setting up new project: " + getSafeErrorMessage(e));
                                    Platform.runLater(() -> {
                                        showErrorAnimation("Project created but setup failed");
                                        listProjects();
                                    });
                                }
                            });
                        },
                        () -> {
                            // Failure callback: Project created but lock failed
                            Platform.runLater(() -> {
                                listProjects();
                            });
                        },
                        true // Show modal dialogs for project creation
                    );
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        String errorMessage = getNetworkErrorMessage(e, "Project creation");
                        logger.info("Failed to create project: " + e.getMessage());
                        
                        if (isServerOfflineError(e)) {
                            showErrorAnimation("Can't reach server");
                            SpeleoDBModals.showError("Server Offline", errorMessage);
                        } else if (isTimeoutError(e)) {
                            showErrorAnimation("Request timed out");
                            SpeleoDBModals.showError("Creation Timeout", errorMessage);
                        } else {
                            showErrorAnimation("Project Creation Failed");
                        }
                    });
                } finally {
                    Platform.runLater(() -> {
                        serverProgressIndicator.setVisible(false);
                        setUILoadingState(false);
                        createNewProjectButton.setDisable(false);
                        createNewProjectButton.setText("Create New Project");
                    });
                }
            });
        } else {
            logger.info("New project creation cancelled");
        }
    }
    


    // -------------------------- Project Opening -------------------------- //

    private void clickSpeleoDBProject(ActionEvent e) throws URISyntaxException, IOException, InterruptedException {
        var project = (JsonObject) ((Button) e.getSource()).getUserData();
        String projectName = project.getString("name");
        String permissionString = project.getString("permission", "READ_ONLY");
        
        // Convert string permission to enum
        AccessLevel permission;
        try {
            permission = AccessLevel.valueOf(permissionString);
        } catch (IllegalArgumentException ex) {
            // Default to READ_ONLY if permission string is invalid
            permission = AccessLevel.READ_ONLY;
            logger.info("Invalid permission '" + permissionString + "' for project " + projectName + ", defaulting to READ-only");
        }
        
        logger.info("Opening project: " + projectName + " (Permission: " + permission + ")");
        
        // Check if this is a project that can be locked (ADMIN or READ_AND_WRITE)
        if (canAcquireLock(permission)) {
            // Attempt to acquire lock first for writable projects
            logger.info("Attempting to acquire lock for project: " + projectName);
            
            Platform.runLater(() -> {
                acquireProjectLockWithUI(project, "project opening",
                    () -> {
                        // Success callback: Lock acquired, download and load with write access
                        logger.info("Lock acquired successfully. Opening with write access.");
                        downloadAndLoadProject(project, true);
                    },
                    () -> {
                        // Failure callback: Lock acquisition failed, show red tooltip and open read-only
                        logger.info("Failed to acquire lock for project - opening as read-only");
                        showErrorAnimation("Lock not available - opening read-only");
                        showLockFailurePopup(project);
                        downloadAndLoadProject(project, false);
                    },
                    true // Show modal dialogs for project opening
                );
            });
        } else {
            // READ_ONLY permission - skip lock acquisition and proceed directly
            logger.info("Opening read-only project: " + projectName);
            showSuccessAnimation("Opening (read-only)");
            showReadOnlyPermissionPopup(project);
            downloadAndLoadProject(project, false);
        }
    }
    
    /**
     * Gets the access level for a project from its permission field.
     * 
     * @param project the project JSON object
     * @return the AccessLevel enum value
     */
    private AccessLevel getProjectAccessLevel(JsonObject project) {
        String permissionString = project.getString("permission", "READ_ONLY");
        try {
            return AccessLevel.valueOf(permissionString);
        } catch (IllegalArgumentException ex) {
            // Default to READ_ONLY if permission string is invalid
            logger.info("Invalid permission '" + permissionString + "' for project " + 
                      project.getString("name") + ", defaulting to read-only");
            return AccessLevel.READ_ONLY;
        }
    }
    
    /**
     * Checks if a project access level allows locking (ADMIN or READ_AND_WRITE).
     * 
     * @param accessLevel the access level to check
     * @return true if the access level allows locking, false otherwise
     */
    private boolean canAcquireLock(AccessLevel accessLevel) {
        return (accessLevel == AccessLevel.ADMIN) || (accessLevel == AccessLevel.READ_AND_WRITE);
    }
    
    /**
     * Shows a popup informing the user that the project was opened in read-only mode
     * due to insufficient permissions, and how to get write access.
     * 
     * @param project the project that was opened in read-only mode
     */
    private void showReadOnlyPermissionPopup(JsonObject project) {
        Platform.runLater(() -> {
            String projectName = project.getString("name");
            String title = "Read-Only Access";
            String message = "Project \"" + projectName + "\" was opened in READ-ONLY mode.\n\n" +
                           "You do not have permission to modify this project.\n\n" +
                           "To get write access, please contact the project administrator.";
            
            SpeleoDBModals.showInfo(title, message);
        });
    }
    
    /**
     * Shows a popup informing the user that the project was opened in read-only mode
     * because the lock is owned by someone else, and how to get write access.
     * 
     * @param project the project that was opened in read-only mode
     */
    private void showLockFailurePopup(JsonObject project) {
        Platform.runLater(() -> {
            String projectName = project.getString("name");
            String title = "Read-Only Access";
            String message;
            
            // Check if the project has lock information
            JsonValue mutex = project.get("active_mutex");
            
            if (mutex != null && mutex.getValueType() != JsonValue.ValueType.NULL) {
                JsonObject mutexObj = mutex.asJsonObject();
                String lockOwner = mutexObj.getString("user", "unknown user");
                String lockDate = mutexObj.getString("creation_date", "unknown time");
                
                message = "Project \"" + projectName + "\" was opened in READ-ONLY mode.\n\n" +
                         "The project is currently locked by: " + lockOwner + "\n" +
                         "Lock acquired: " + formatLockDate(lockDate) + "\n\n" +
                         "To modify this project, please contact `" + lockOwner + "` to release the lock.";
            } else {
                // Generic lock failure message
                message = "Project \"" + projectName + "\" was opened in READ-ONLY mode.\n\n" +
                         "Unable to acquire project lock at this time.\n\n" +
                         "Please try again later or contact the project administrator.";
            }
            
            SpeleoDBModals.showInfo(title, message);
        });
    }
    
    /**
     * Formats a lock date string for display in user messages.
     * 
     * @param lockDate the raw lock date string from the server
     * @return a formatted date string for display
     */
    private String formatLockDate(String lockDate) {
        try {
            // Parse and format the lock date for better readability
            String dateStr = lockDate.substring(0, lockDate.lastIndexOf('.'));
            LocalDateTime lockDateTime = LocalDateTime.parse(dateStr);
            return lockDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        } catch (Exception e) {
            // Return original string if parsing fails
            return lockDate;
        }
    }
    
    /**
     * Downloads and loads a project with unified logic for both read-only and writable projects.
     * 
     * @param project the project to download and load
     * @param hasWriteAccess whether the user has write access (lock acquired) or read-only access
     */
    private void downloadAndLoadProject(JsonObject project, boolean hasWriteAccess) {
        String projectName = project.getString("name");
        
        // Update UI based on write access
        Platform.runLater(() -> {
            // Clear upload message when project is opened
            uploadMessageTextField.clear();

            if (hasWriteAccess) {
                // Show actions pane for writable projects
                actionsTitlePane.setVisible(true);
                actionsTitlePane.setExpanded(true);
                actionsTitlePane.setText("Actions on `" + projectName + "`.");
                uploadButton.setDisable(false);
                unlockButton.setDisable(false);
                currentProject = project;
            } else {
                // Hide actions pane for read-only projects
                actionsTitlePane.setVisible(false);
                actionsTitlePane.setExpanded(false);
                uploadButton.setDisable(true);
                unlockButton.setDisable(true);
                currentProject = null; // Don't set current project for read-only
            }
        });
        
        // Download and load project (same logic for both read-only and writable)
        parentPlugin.executorService.execute(() -> {
            try {
                // Download project
                logger.info("Downloading project: " + projectName);
                Path tml_filepath = speleoDBService.downloadProject(project);

                if (Files.exists(tml_filepath)) {
                    // Load the project
                    String loadingMessage = hasWriteAccess ? "Loading project file ..." : "Loading read-only project file...";
                    Platform.runLater(() ->logger.info(loadingMessage));
                    
                    try {
                        parentPlugin.loadSurvey(tml_filepath.toFile());
                        Platform.runLater(() -> {
                            String successMessage = hasWriteAccess ? 
                                "Project loaded successfully: " + projectName :
                                "Read-only project loaded successfully: " + projectName;
                            logger.info(successMessage);
                            checkAndUpdateSpeleoDBId(project);
                            
                            // Refresh project listing
                            listProjects();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            String errorMessage = hasWriteAccess ? 
                                "Failed to load project: " + getSafeErrorMessage(e) :
                                "Failed to load read-only project: " + getSafeErrorMessage(e);
                            logger.info(errorMessage);

                            showErrorAnimation(errorMessage);
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        logger.info("Downloaded file not found: " + tml_filepath);
                        showErrorAnimation("Failed to download project file");
                    });
                }
            } catch (IOException | InterruptedException | URISyntaxException e) {
                Platform.runLater(() -> {
                    String errorMessage = "Failed to download project: " + getSafeErrorMessage(e);
                    logger.info(errorMessage);
                    showErrorAnimation(errorMessage);
                });
            }

            Platform.runLater(() -> {
                // Once everything is done, hide the progress indicator and set the UI to not loading
                serverProgressIndicator.setVisible(false);
                setUILoadingState(false);
            });
        });
    }

    /**
     * Handles the action performed when a project button is clicked.
     *
     * @param event       The ActionEvent triggered by the button click.
     * @param projectItem The JsonObject containing project metadata.
     */
    private void handleProjectCardClickAction(ActionEvent event, JsonObject projectItem) {
        // Prevent double click and show loading
        setUILoadingState(true);
        serverProgressIndicator.setVisible(true);

        try {
            String selectedProjectName = projectItem.getString("name");
            String selectedProjectId = projectItem.getString("id");
            
            // Check if user is trying to switch to a different project while having an active lock
            if (hasActiveProjectLock()) {
                String currentProjectId = currentProject.getString("id");
                
                // If clicking on the same project, proceed normally
                if (currentProjectId.equals(selectedProjectId)) {
                    logger.info("Selected project: " + selectedProjectName);
                    clickSpeleoDBProject(event);
                    return;
                }
                
                // Different project selected - show confirmation dialog
                logger.info("Attempting to switch from locked project: " + getCurrentProjectName() + 
                          " to: " + selectedProjectName);
                
                boolean shouldSwitch = showProjectSwitchConfirmation(selectedProjectName);
                
                if (!shouldSwitch) {
                    logger.info("User cancelled project switch. Staying on: " + getCurrentProjectName());
                    Platform.runLater(() -> {
                        setUILoadingState(false);
                        serverProgressIndicator.setVisible(false);
                    });
                    return;
                }
                
                // User confirmed switch - release current lock first
                logger.info("User confirmed project switch. Releasing lock on: " + getCurrentProjectName());
                
                // Use centralized lock release with UI integration
                releaseProjectLockWithUI(currentProject, "project switch", 
                    () -> {
                        // Success callback: proceed with new project selection
                        try {
                            logger.info("Proceeding with new project selection: " + selectedProjectName);
                            clickSpeleoDBProject(event);
                        } catch (IOException | InterruptedException | URISyntaxException e) {
                            logger.info("Error switching to new project: " + getSafeErrorMessage(e));
                            Platform.runLater(() -> {
                                setUILoadingState(false);
                                serverProgressIndicator.setVisible(false);
                            });
                        }
                    },
                    () -> {
                        // Failure callback: re-enable project list
                        logger.info("Cannot switch projects - failed to release current lock");
                        Platform.runLater(() -> {
                            setUILoadingState(false);
                            serverProgressIndicator.setVisible(false);
                        });
                    },
                    false // Don't show modal dialogs for project switch
                );
                
                return;
            }
            
            // No active lock - proceed normally
            logger.info("Selected project: " + selectedProjectName);
            clickSpeleoDBProject(event);

        } catch (IOException | InterruptedException | URISyntaxException e) {
            logger.info("Error handling project action: " + e.getMessage());
            Platform.runLater(() -> {
                setUILoadingState(false);
                serverProgressIndicator.setVisible(false);
            });
        }

    }

    // ---------------------- Project Mutex Management --------------------- //

    /**
     * Shows project switch confirmation - uses the ultra-fast modal.
     */
    private boolean showProjectSwitchConfirmation(String newProjectName) {
        String message = "You have a lock on project \"" + currentProject.getString("name") + "\".\n\n" +
                        "To open \"" + newProjectName + "\", you need to release your current lock.\n\n" +
                        "Do you want to continue?";
        return showConfirmationModal("Switch Project", message, "Yes", "No");
    }

    /**
     * Shows unlock confirmation - uses the ultra-fast modal.
     */
    private boolean showUnlockConfirmation() {
        String message = "Are you sure you want to unlock project \"" + currentProject.getString("name") + "\"?\n\n" +
                        "This will allow other users to edit the project.";
        return showConfirmationModal("Unlock Project", message, "Yes", "No");
    }
    
    /**
     * Shows release lock confirmation - uses the ultra-fast modal.
     */
    private boolean showReleaseLockConfirmation() {
        String message = "Do you want to release the lock on project \"" + currentProject.getString("name") + "\"?\n\n" +
                        "This will allow other users to edit the project.";
        return showConfirmationModal("Release Lock", message, "Yes", "No");
    }

    /**
     * Handles the "Unlock Project" button click event for SpeleoDB.
     */
    @FXML
    public void onUnlockSpeleoDB(ActionEvent actionEvent) throws IOException, InterruptedException, URISyntaxException {
        
        // Show confirmation popup before unlocking
        boolean shouldUnlock = showUnlockConfirmation();
        
        if (!shouldUnlock) {
            logger.info("User cancelled unlock operation.");
            return;
        }

        logger.info("Unlocking project " + currentProject.getString("name"));
        serverProgressIndicator.setVisible(true);
        uploadButton.setDisable(true);
        unlockButton.setDisable(true);

        // Use centralized lock release with UI integration
        releaseProjectLockWithUI(currentProject, "manual unlock", 
            null, // No additional success callback needed
            null, // No additional failure callback needed  
            true  // Show modal dialogs for unlock operation
        );
        
        // Always re-enable UI controls after operation
        Platform.runLater(() -> {
            serverProgressIndicator.setVisible(false);
            uploadButton.setDisable(false);
            unlockButton.setDisable(false);
        });
    }

    // --------------------- Project Saving and Upload --------------------- //

    /**
     * Shared method to upload a project with a given commit message.
     * 
     * @param commitMessage the commit message for the upload
     */
    private void uploadProjectWithMessage(String commitMessage) {
        parentPlugin.saveSurvey();

        logger.info("Uploading project " + currentProject.getString("name") + "  ...");
        
        serverProgressIndicator.setVisible(true);
        uploadButton.setDisable(true);
        unlockButton.setDisable(true);

        parentPlugin.executorService.execute(() -> {
            try {
                speleoDBService.uploadProject(commitMessage, currentProject);
                logger.info("Upload successful.");
                
                // Clear the upload message text field after successful upload
                Platform.runLater(() -> {
                    uploadMessageTextField.clear();
                });
                
                // Show success celebration dialog with random GIF, then handle lock release
                Platform.runLater(() -> {
                    showSuccessCelebrationDialog(() -> {
                        // This callback is executed after the celebration dialog closes
                        boolean shouldReleaseLock = showReleaseLockConfirmation();
                        
                        if (shouldReleaseLock) {
                            logger.info("User chose to release the write lock.");
                            
                            // Use centralized lock release with UI integration
                            releaseProjectLockWithUI(currentProject, "post-upload", 
                                null, // No additional success callback needed
                                null, // No additional failure callback needed
                                true  // Show modal dialogs for post-upload release
                            );
                        } else {
                            logger.info("User chose to keep the write lock.");
                        }
                    });
                });
                
            } catch (Exception e) {
                String errorMessage = getNetworkErrorMessage(e, "Upload");
                logger.info("Upload failed: " + getSafeErrorMessage(e));
                
                if (isServerOfflineError(e)) {
                    showErrorAnimation("Can't reach server");
                    SpeleoDBModals.showError("Server Offline", errorMessage);
                } else if (isTimeoutError(e)) {
                    showErrorAnimation("Upload timed out");
                    SpeleoDBModals.showError("Upload Timeout", errorMessage);
                } else {
                    showErrorAnimation("Upload Failed: " + getSafeErrorMessage(e));
                }
            } finally {
                Platform.runLater(() -> {
                    serverProgressIndicator.setVisible(false);
                    uploadButton.setDisable(false);
                });
            }
        });
    }

    /**
     * Handles the "Save Project" button click event for SpeleoDB.
     */
    @FXML
    public void onUploadSpeleoDB(ActionEvent actionEvent) throws IOException, URISyntaxException, InterruptedException {
        String message = uploadMessageTextField.getText();
        if (message.isEmpty()) {
            logger.info(MESSAGES.UPLOAD_MESSAGE_EMPTY);
            SpeleoDBModals.showError(DIALOGS.TITLE_UPLOAD_MESSAGE_REQUIRED, MESSAGES.UPLOAD_MESSAGE_EMPTY);
            return;
        }
        
        uploadProjectWithMessage(message);
    }
    
    /**
     * Handles the "Reload Project" button click event for SpeleoDB.
     * Reloads the current project from disk after confirming with the user.
     */
    @FXML
    public void onReloadProject(ActionEvent actionEvent) {
        // Ensure we have a current project
        if (currentProject == null) {
            logger.info("No project is currently loaded to reload");
            showErrorAnimation("No project to reload");
            return;
        }
        
        String projectName = currentProject.getString("name");
        
        // Show confirmation dialog following material design
        String message = "Are you sure you want to reload the project \"" + projectName + "\"?\n\n" +
                        "⚠️ WARNING: Any unsaved modifications will be lost!\n\n" +
                        "• All changes since last save will be discarded\n" +
                        "• The project will be reloaded from disk\n" +
                        "• This action cannot be undone";
        
        boolean shouldReload = showConfirmationModal("Reload Project", message, "Reload", "Cancel");
        
        if (!shouldReload) {
            logger.info("User cancelled reload operation for project: " + projectName);
            return;
        }
        
        // Show progress indicator
        serverProgressIndicator.setVisible(true);
        uploadButton.setDisable(true);
        unlockButton.setDisable(true);
        
        // Wait for spinner to actually be rendered before starting background work
        Platform.runLater(() -> {
            // Double-ensure the spinner has time to render by using another Platform.runLater
            Platform.runLater(() -> {
                
                // Execute reload in background thread after UI has had time to render
                parentPlugin.executorService.execute(() -> {
            try {
                String projectId = currentProject.getString("id");
                Path tmlFilePath = Paths.get(SpeleoDBService.ARIANE_ROOT_DIR + File.separator + projectId + PATHS.TML_FILE_EXTENSION);
                
                if (!Files.exists(tmlFilePath)) {
                    Platform.runLater(() -> {
                        logger.info("Project file not found on disk: " + tmlFilePath);
                        showErrorAnimation("Project file not found");
                        SpeleoDBModals.showError("Reload Failed", "The project file could not be found on disk.\n\nPath: " + tmlFilePath);
                        
                        // Hide progress indicator on error
                        serverProgressIndicator.setVisible(false);
                        uploadButton.setDisable(false);
                        unlockButton.setDisable(false);
                    });
                    return;
                }
                
                logger.info("Reloading project from disk: " + projectName);
                
                // Load the project using the existing loadSurvey method
                try {
                    parentPlugin.loadSurvey(tmlFilePath.toFile());
                    
                    Platform.runLater(() -> {
                        logger.info("Project reloaded successfully: " + projectName);
                        showSuccessAnimation("Project Reloaded!");
                        
                        // Update SpeleoDB ID if needed
                        checkAndUpdateSpeleoDBId(currentProject);
                        
                        // Clear upload message field as we've reloaded
                        uploadMessageTextField.clear();
                    });
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        String errorMessage = "Failed to reload project: " + getSafeErrorMessage(e);
                        logger.info(errorMessage);
                        showErrorAnimation("Reload failed");
                        SpeleoDBModals.showError("Reload Failed", "Failed to reload the project from disk.\n\nError: " + getSafeErrorMessage(e));
                        
                        // Hide progress indicator on error
                        serverProgressIndicator.setVisible(false);
                        uploadButton.setDisable(false);
                        unlockButton.setDisable(false);
                    });
                }
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    String errorMessage = "Error during project reload: " + getSafeErrorMessage(e);
                    logger.info(errorMessage);
                    showErrorAnimation("Reload error");
                    SpeleoDBModals.showError("Reload Error", "An unexpected error occurred during reload.\n\nError: " + getSafeErrorMessage(e));
                    
                    // Hide progress indicator on error
                    serverProgressIndicator.setVisible(false);
                    uploadButton.setDisable(false);
                    unlockButton.setDisable(false);
                });
            } finally {
                Platform.runLater(() -> {
                    // Re-enable UI controls
                    serverProgressIndicator.setVisible(false);
                    uploadButton.setDisable(false);
                    unlockButton.setDisable(false);
                });
            }
                });
            });
        });
    }

    public void onSignupSpeleoDB(ActionEvent actionEvent) {
        try {
            String sdb_instance = instanceTextField.getText().trim();
            if (sdb_instance.isEmpty()) {
                sdb_instance = PREFERENCES.DEFAULT_INSTANCE;
            }
            
            String protocol = isDebugMode() ? "http" : "https";
            String signupUrl = protocol + "://" + sdb_instance + "/signup/";
            
            java.awt.Desktop.getDesktop().browse(new java.net.URI(signupUrl));
            logger.info("Opening signup page: " + signupUrl);
        } catch (IOException | URISyntaxException e) {
            logger.info("Failed to open signup page: " + getSafeErrorMessage(e));
            showErrorAnimation();
        }
    }
    
    /**
     * Handles the "Learn About" button click event to open the SpeleoDB website.
     */
    @FXML
    public void onLearnAbout(ActionEvent actionEvent) {
        try {
            String speleoBDUrl = "https://" + PREFERENCES.DEFAULT_INSTANCE;
            
            // Open URL in default browser
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(speleoBDUrl));
                    logger.info("Opened SpeleoDB website in browser: " + speleoBDUrl);
                } else {
                    logger.info("Browser not supported on this system");
                }
            } else {
                logger.info("Desktop operations not supported on this system");
            }
        } catch (IOException | URISyntaxException e) {
            logger.info("Failed to open SpeleoDB website: " + getSafeErrorMessage(e));
        }
    }
    
    // ===================== SHUTDOWN SUPPORT METHODS ===================== //
    
    /**
     * Checks if there is currently an active project with a lock.
     * 
     * @return true if there is an active project, false otherwise
     */
    public boolean hasActiveProjectLock() {
        return currentProject != null;
    }
    
    /**
     * Gets the name of the currently active project.
     * 
     * @return the project name, or null if no project is active
     */
    public String getCurrentProjectName() {
        return currentProject != null ? currentProject.getString("name") : null;
    }
    
    /**
     * Releases the lock on the currently active project.
     * This method is called during application shutdown.
     * 
     * @throws IOException if there's an error releasing the lock
     * @throws InterruptedException if the operation is interrupted
     * @throws URISyntaxException if there's a URI syntax error
     */
    public void releaseCurrentProjectLock() throws IOException, InterruptedException, URISyntaxException {
        if (currentProject != null) {
            // Use centralized lock release for shutdown - simple version without UI updates
            LockReleaseResult result = releaseProjectLock(currentProject, "application shutdown");
            
            if (result.hasError()) {
                // Re-throw exception for shutdown handling
                throw new IOException("Failed to release lock during shutdown: " + result.getMessage(), result.getError());
            }
        }
    }
    
    // ====================== CENTRALIZED LOCK MANAGEMENT ====================== //
    
    /**
     * Result of a lock acquisition attempt with detailed information
     */
    public static class LockResult {
        private final boolean acquired;
        private final String message;
        private final JsonObject project;
        private final Exception error;
        
        private LockResult(boolean acquired, String message, JsonObject project, Exception error) {
            this.acquired = acquired;
            this.message = message;
            this.project = project;
            this.error = error;
        }
        
        public static LockResult success(JsonObject project, String message) {
            return new LockResult(true, message, project, null);
        }
        
        public static LockResult failure(JsonObject project, String message) {
            return new LockResult(false, message, project, null);
        }
        
        public static LockResult error(JsonObject project, String message, Exception error) {
            return new LockResult(false, message, project, error);
        }
        
        public boolean isAcquired() { return acquired; }
        public String getMessage() { return message; }
        public JsonObject getProject() { return project; }
        public Exception getError() { return error; }
        public boolean hasError() { return error != null; }
    }
    
    /**
     * Result of a lock release attempt with detailed information
     */
    public static class LockReleaseResult {
        private final boolean released;
        private final String message;
        private final JsonObject project;
        private final Exception error;
        
        private LockReleaseResult(boolean released, String message, JsonObject project, Exception error) {
            this.released = released;
            this.message = message;
            this.project = project;
            this.error = error;
        }
        
        public static LockReleaseResult success(JsonObject project, String message) {
            return new LockReleaseResult(true, message, project, null);
        }
        
        public static LockReleaseResult failure(JsonObject project, String message) {
            return new LockReleaseResult(false, message, project, null);
        }
        
        public static LockReleaseResult error(JsonObject project, String message, Exception error) {
            return new LockReleaseResult(false, message, project, error);
        }
        
        public boolean isReleased() { return released; }
        public String getMessage() { return message; }
        public JsonObject getProject() { return project; }
        public Exception getError() { return error; }
        public boolean hasError() { return error != null; }
    }
    
    /**
     * Centralized lock release with comprehensive error handling and logging.
     * This method consolidates all lock release logic to eliminate duplication.
     * 
     * @param project the project to release lock for
     * @param context description of the operation context (e.g., "project switch", "unlock", "post-upload")
     * @return LockReleaseResult with detailed information about the operation
     */
    private LockReleaseResult releaseProjectLock(JsonObject project, String context) {
        if (project == null) {
            return LockReleaseResult.failure(null, "No project provided for lock release");
        }
        
        String projectName = project.getString("name");
        logger.info("Releasing lock for project '" + projectName + "' (context: " + context + ")");
        
        try {
            boolean success = speleoDBService.releaseProjectMutex(project);
            
            if (success) {
                String successMessage = "Successfully released lock on: " + projectName;
                logger.info(successMessage);
                return LockReleaseResult.success(project, successMessage);
            } else {
                String failureMessage = "Failed to release lock for " + projectName;
                logger.info(failureMessage);
                return LockReleaseResult.failure(project, failureMessage);
            }
            
        } catch (IOException | InterruptedException | URISyntaxException e) {
            String errorMessage = "Error releasing lock for " + projectName + ": " + getSafeErrorMessage(e);
            logger.info(errorMessage);
            return LockReleaseResult.error(project, errorMessage, e);
        }
    }
    
    /**
     * Centralized lock release with UI integration and comprehensive error handling.
     * Handles all UI updates, animations, and modal displays based on the release result.
     * 
     * @param project the project to release lock for
     * @param context description of the operation context
     * @param onSuccess callback to execute on successful lock release (optional)
     * @param onFailure callback to execute on failed lock release (optional)
     * @param showModals whether to show error modals for network issues
     */
    private void releaseProjectLockWithUI(JsonObject project, String context, 
                                        Runnable onSuccess, Runnable onFailure, boolean showModals) {
        
        parentPlugin.executorService.execute(() -> {
            LockReleaseResult result = releaseProjectLock(project, context);
            
            Platform.runLater(() -> {
                if (result.isReleased()) {
                    // Success: Clear current project and update UI
                    currentProject = null;
                    
                    // Update UI state
                    actionsTitlePane.setVisible(false);
                    actionsTitlePane.setExpanded(false);
                    projectsTitlePane.setExpanded(true);
                    
                    // Show success animation
                    showSuccessAnimation("Lock Released");
                    
                    // Execute success callback
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    
                    // Refresh project list
                    parentPlugin.executorService.execute(() -> {
                        try {
                            listProjects();
                        } catch (Exception e) {
                            logger.info("Error refreshing project list after lock release: " + getSafeErrorMessage(e));
                        }
                    });
                    
                } else if (result.hasError()) {
                    // Network/Exception error: Handle with appropriate animations and modals
                    Exception error = result.getError();
                    String networkErrorMessage = getNetworkErrorMessage(error, "Lock release");
                    
                    if (isServerOfflineError(error)) {
                        showErrorAnimation("Can't reach server");
                        if (showModals) {
                            SpeleoDBModals.showError("Server Offline", networkErrorMessage);
                        }
                    } else if (isTimeoutError(error)) {
                        showErrorAnimation("Lock release timed out");
                        if (showModals) {
                            SpeleoDBModals.showError("Lock Release Timeout", networkErrorMessage);
                        }
                    } else {
                        showErrorAnimation("Failed to Release Lock");
                        if (showModals) {
                            SpeleoDBModals.showError("Lock Release Error", result.getMessage());
                        }
                    }
                    
                    // Execute failure callback
                    if (onFailure != null) {
                        onFailure.run();
                    }
                    
                } else {
                    // Service-level failure (returned false)
                    showErrorAnimation("Failed to Release Lock");
                    
                    // Execute failure callback
                    if (onFailure != null) {
                        onFailure.run();
                    }
                }
            });
        });
    }
    
    /**
     * Simple boolean wrapper for lock release operations.
     * Use this when you only need to know if the release succeeded or failed.
     * 
     * @param project the project to release lock for
     * @param context description of the operation context
     * @return true if lock was successfully released, false otherwise
     */
    private boolean tryReleaseProjectLock(JsonObject project, String context) {
        return releaseProjectLock(project, context).isReleased();
    }
    
    /**
     * Centralized lock acquisition with comprehensive error handling and logging.
     * This method consolidates all lock acquisition logic to eliminate duplication.
     * 
     * @param project the project to acquire lock for
     * @param context description of the operation context (e.g., "project creation", "project opening")
     * @return LockResult with detailed information about the operation
     */
    private LockResult acquireProjectLock(JsonObject project, String context) {
        String projectName = project.getString("name");
        String projectId = project.getString("id");
        
        try {
            // Check if project allows editing
            String permission = project.getString("permission", "");
            if (AccessLevel.READ_ONLY.name().equals(permission)) {
                String message = "Project '" + projectName + "' is read-only - no lock needed";
                logger.info("🔒 " + message);
                return LockResult.failure(project, message);
            }
            
            logger.info("🔄 Acquiring lock for " + context + ": " + projectName);
        
            boolean lockAcquired = speleoDBService.acquireOrRefreshProjectMutex(project);
            
            if (lockAcquired) {
                String successMessage = "✓ Lock acquired successfully for " + context + ": " + projectName;
                logger.info(successMessage);
                return LockResult.success(project, successMessage);
            } else {
                String failureMessage = "⚠️ Failed to acquire lock for " + context + ": " + projectName;
                logger.info(failureMessage);
                return LockResult.failure(project, failureMessage);
            }
            
        } catch (Exception e) {
            String errorMessage = "❌ Error acquiring lock for " + context + ": " + projectName + " - " + getSafeErrorMessage(e);
            logger.info(errorMessage);
            return LockResult.error(project, errorMessage, e);
        }
    }
    
    /**
     * Centralized lock acquisition with UI integration and comprehensive error handling.
     * Handles all UI updates, animations, and modal displays based on the acquisition result.
     * 
     * @param project the project to acquire lock for
     * @param context description of the operation context
     * @param onSuccess callback to execute on successful lock acquisition (optional)
     * @param onFailure callback to execute on failed lock acquisition (optional)
     * @param showModals whether to show error modals for network issues
     */
    private void acquireProjectLockWithUI(JsonObject project, String context, 
                                         Runnable onSuccess, Runnable onFailure, boolean showModals) {
        parentPlugin.executorService.execute(() -> {
            LockResult result = acquireProjectLock(project, context);
            
            Platform.runLater(() -> {
                if (result.isAcquired()) {
                    // Success: Set as current project and enable UI controls
                    currentProject = project;
                    
                    // Update UI state for editing
                    actionsTitlePane.setVisible(true);
                    actionsTitlePane.setExpanded(true);
                    actionsTitlePane.setText("Actions on `" + project.getString("name") + "`.");
                    uploadButton.setDisable(false);
                    unlockButton.setDisable(false);
                    
                    // Show success animation
                    showSuccessAnimation("Lock Acquired");
                    
                    // Execute success callback
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    
                } else if (result.hasError()) {
                    // Network/Exception error: Handle with appropriate animations and modals
                    Exception error = result.getError();
                    String networkErrorMessage = getNetworkErrorMessage(error, "Lock acquisition");
                    
                    if (isServerOfflineError(error)) {
                        showErrorAnimation("Can't reach server");
                        if (showModals) {
                            SpeleoDBModals.showError("Server Offline", networkErrorMessage);
                        }
                    } else if (isTimeoutError(error)) {
                        showErrorAnimation("Lock acquisition timed out");
                        if (showModals) {
                            SpeleoDBModals.showError("Lock Acquisition Timeout", networkErrorMessage);
                        }
                    } else {
                        showErrorAnimation("Failed to Acquire Lock");
                        if (showModals) {
                            SpeleoDBModals.showError("Lock Acquisition Error", result.getMessage());
                        }
                    }
                    
                    // Disable editing controls
                    uploadButton.setDisable(true);
                    unlockButton.setDisable(true);
                    
                    // Execute failure callback
                    if (onFailure != null) {
                        onFailure.run();
                    }
                    
                } else {
                    // Service-level failure (returned false) - e.g., read-only project, already locked
                    String failureReason = result.getMessage();
                    
                    if (failureReason.contains("read-only")) {
                        showSuccessAnimation("Project opened (read-only)");
                    } else {
                        showErrorAnimation("Lock not available");
                    }
                    
                    // Disable editing controls for read-only or locked projects
                    uploadButton.setDisable(true);
                    unlockButton.setDisable(true);
                    
                    // Execute failure callback
                    if (onFailure != null) {
                        onFailure.run();
                    }
                }
            });
        });
    }

    /**
     * Cleanup method to properly close resources and prevent shutdown hangs.
     * Should be called before the controller is destroyed.
     */
    public void cleanup() {
        logger.debug("Starting SpeleoDBController cleanup");
        
        // Stop all running animations to prevent memory leaks
        synchronized (runningAnimations) {
            for (Timeline timeline : runningAnimations) {
                try {
                    timeline.stop();
                } catch (Exception e) {
                    logger.error("Error stopping animation during cleanup", e);
                }
            }
            runningAnimations.clear();
        }
        
        // Clear field references
        currentProject = null;
        cachedProjectList = null;
        
        // Cleanup tooltips
        SpeleoDBTooltips.cleanup();
        
        logger.info("Controller cleanup completed");
        
        // Disconnect UI controller from logger to prevent null pointer exceptions during shutdown
        logger.disconnectUIController();
        
        // Delegate logging shutdown to the plugin's centralized system
        // No need to shutdown file logger here since it's managed by the plugin
    }

    /**
     * Creates or gets a dedicated overlay pane for animations that's guaranteed to be on top.
     * This pane is mouse-transparent and positioned to cover the entire scene.
     */
    private javafx.scene.layout.Pane getOrCreateAnimationOverlay() {
        try {
            // Try to get the scene root
            if (speleoDBAnchorPane.getScene() != null) {
                javafx.scene.Parent root = speleoDBAnchorPane.getScene().getRoot();
                
                // Look for existing overlay
                if (root instanceof javafx.scene.layout.Pane rootPane) {
                    for (javafx.scene.Node child : rootPane.getChildren()) {
                        if (child.getId() != null && child.getId().equals("animationOverlay")) {
                            return (javafx.scene.layout.Pane) child;
                        }
                    }
                    
                    // Create new overlay pane
                    javafx.scene.layout.Pane overlay = new javafx.scene.layout.Pane();
                    overlay.setId("animationOverlay");
                    overlay.setMouseTransparent(true);
                    overlay.setStyle("-fx-background-color: transparent;");
                    
                    // Size to match scene
                    overlay.prefWidthProperty().bind(speleoDBAnchorPane.getScene().widthProperty());
                    overlay.prefHeightProperty().bind(speleoDBAnchorPane.getScene().heightProperty());
                    
                    // Add to root and bring to front
                    rootPane.getChildren().add(overlay);
                    overlay.toFront();
                    return overlay;
                }
            }
        } catch (Exception e) {
            logger.error("Error creating animation overlay", e);
        }
        
        // Fallback to main pane
        return speleoDBAnchorPane;
    }

    /**
     * Creates a highly visible debug animation for testing visibility issues.
     * This uses bright orange styling and larger dimensions.
     */
    public void showDebugAnimation() {        
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::showDebugAnimation);
            return;
        }
        
        if (speleoDBAnchorPane == null) {
            logger.warn("Cannot show debug animation - speleoDBAnchorPane is null");
            return;
        }
        
        try {
            // Create highly visible debug label
            Label debugLabel = new Label("🔥 DEBUG ANIMATION TEST 🔥");
            debugLabel.setStyle(STYLES.DEBUG_ANIMATION_STYLE);
            debugLabel.setWrapText(false);
            debugLabel.setAlignment(javafx.geometry.Pos.CENTER);
            debugLabel.setMinWidth(300);
            debugLabel.setMaxWidth(600);
            
            // Get overlay and position
            javafx.scene.layout.Pane animationPane = getOrCreateAnimationOverlay();
            
            // Center positioning
            Platform.runLater(() -> {
                try {
                    double paneWidth = animationPane.getWidth();
                    if (paneWidth <= 0 && speleoDBAnchorPane.getScene() != null) {
                        paneWidth = speleoDBAnchorPane.getScene().getWidth();
                    }
                    double labelWidth = debugLabel.getBoundsInLocal().getWidth();
                    double leftPosition = Math.max(10, (paneWidth - labelWidth) / 2);
                    debugLabel.setLayoutX(leftPosition);
                    debugLabel.setLayoutY(50); // Slightly lower than normal animations
                } catch (Exception e) {
                    logger.error("Error positioning debug animation", e);
                    debugLabel.setLayoutX(50);
                    debugLabel.setLayoutY(50);
                }
            });
            
            // Add to overlay with maximum visibility
            animationPane.getChildren().add(debugLabel);
            debugLabel.toFront();
            animationPane.toFront();
            
            // Pulsing animation for visibility
            debugLabel.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), debugLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
            // Auto-hide after 8 seconds (longer for debugging)
            Timeline hideTimeline = createTrackedTimeline(
                new KeyFrame(Duration.seconds(8), e -> {
                    try {
                        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), debugLabel);
                        fadeOut.setFromValue(1.0);
                        fadeOut.setToValue(0.0);
                        fadeOut.setOnFinished(event -> {
                            try {
                                animationPane.getChildren().remove(debugLabel);
                            } catch (Exception ex) {
                                logger.error("Error removing debug animation", ex);
                            }
                        });
                        fadeOut.play();
                    } catch (Exception ex) {
                        logger.error("Error during debug animation fadeout", ex);
                    }
                })
            );
            hideTimeline.play();
            
        } catch (Exception e) {
            logger.error("Failed to show debug animation", e);
        }
    }

    /**
     * Shows a success celebration dialog with a random GIF animation.
     * The dialog auto-closes after 5 seconds and includes a manual close button.
     * 
     * @param onCloseCallback callback to execute when the dialog closes (optional, can be null)
     */
    private void showSuccessCelebrationDialog(Runnable onCloseCallback) {
        String randomGifPath = getRandomSuccessGif();
        Window owner = speleoDBAnchorPane.getScene() != null ? 
                      speleoDBAnchorPane.getScene().getWindow() : null;
        
        SpeleoDBModals.showSuccessCelebration(randomGifPath, onCloseCallback, owner);
    }
    
    /**
     * Gets the next success GIF using a rotating index to ensure all GIFs are shown
     * without repetition until all have been displayed.
     * 
     * @return a GIF resource path, or null if no GIFs are available
     */
    private String getRandomSuccessGif() {
        try {
            java.util.List<String> availableGifs = getAvailableSuccessGifs();
            
            if (availableGifs.isEmpty()) {
                logger.debug("No success GIFs found in resources");
                return null;
            }
            
            // Get current index from preferences
            Preferences prefs = getPreferencesNode();
            int currentIndex = prefs.getInt(PREFERENCES.PREF_SUCCESS_GIF_INDEX, 0);
            
            // Ensure index is within bounds
            if (currentIndex >= availableGifs.size()) {
                currentIndex = 0;
            }
            
            String selectedGif = availableGifs.get(currentIndex);
            
            // Update index for next time (rotate through all GIFs)
            int nextIndex = (currentIndex + 1) % availableGifs.size();
            prefs.putInt(PREFERENCES.PREF_SUCCESS_GIF_INDEX, nextIndex);
            
            logger.debug("Selected success GIF: " + selectedGif + " (index " + currentIndex + " of " + availableGifs.size() + ")");
            return selectedGif;
            
        } catch (Exception e) {
            logger.error("Error selecting random success GIF", e);
            return null;
        }
    }
    
    /**
     * Scans the success GIFs directory and returns a sorted list of available GIF file paths.
     * 
     * @return sorted list of GIF resource paths (ensures consistent ordering for rotation)
     */
    private java.util.List<String> getAvailableSuccessGifs() {
        java.util.List<String> gifPaths = new java.util.ArrayList<>();
        
        try {
            // Get the resource URL for the GIFs directory
            java.net.URL resourceUrl = getClass().getResource(PATHS.SUCCESS_GIFS_DIR);
            if (resourceUrl != null) {
                // Handle different resource loading scenarios (JAR vs file system)
                if (resourceUrl.getProtocol().equals("jar")) {
                    // Running from JAR - scan JAR entries
                    scanGifsFromJar(gifPaths);
                } else {
                    // Running from file system - scan directory
                    scanGifsFromFileSystem(resourceUrl, gifPaths);
                }
            } else {
                logger.warn("Success GIFs directory not found: " + PATHS.SUCCESS_GIFS_DIR);
            }
            
            // Sort the paths to ensure consistent ordering for rotation
            java.util.Collections.sort(gifPaths);
            
            logger.info("Found " + gifPaths.size() + " success GIFs in directory (sorted for consistent rotation)");
            
        } catch (Exception e) {
            logger.error("Error scanning for success GIFs", e);
        }
        
        return gifPaths;
    }
    
    /**
     * Scans for GIF files when running from a JAR file.
     */
    private void scanGifsFromJar(java.util.List<String> gifPaths) {
        try {
            // Get the JAR file containing this class
            java.net.URL jarUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
            try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(new java.io.File(jarUrl.toURI()))) {
                java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
                
                String dirPath = PATHS.SUCCESS_GIFS_DIR.substring(1); // Remove leading slash
                
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    
                    // Check if entry is in our GIFs directory and is a .gif file
                    if (entryName.startsWith(dirPath) && entryName.toLowerCase().endsWith(".gif") && !entry.isDirectory()) {
                        String resourcePath = "/" + entryName;
                        gifPaths.add(resourcePath);
                        logger.debug("Found success GIF in JAR: " + resourcePath);
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            logger.error("Error scanning GIFs from JAR", e);
        }
    }
    
    /**
     * Scans for GIF files when running from file system.
     */
    private void scanGifsFromFileSystem(java.net.URL resourceUrl, java.util.List<String> gifPaths) {
        try {
            java.io.File directory = new java.io.File(resourceUrl.toURI());
            java.io.File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".gif"));
            
            if (files != null) {
                for (java.io.File file : files) {
                    String resourcePath = PATHS.SUCCESS_GIFS_DIR + file.getName();
                    gifPaths.add(resourcePath);
                    logger.debug("Found success GIF in filesystem: " + resourcePath);
                }
            }
        } catch (URISyntaxException e) {
            logger.error("Error scanning GIFs from filesystem", e);
        }
    }

    /**
     * Shows a Material Design style information popup with welcome message.
     * This popup appears 3 seconds after the application starts to provide
     * helpful information to new users without being intrusive.
     * First checks for plugin updates, then fetches the latest announcement from the SpeleoDB API.
     */
    private void showInformationPopup() {
        // Fetch data asynchronously to avoid blocking UI
        parentPlugin.executorService.execute(() -> {
            try {
                // Get instance URL
                String instanceUrl = instanceTextField.getText();
                if (instanceUrl == null || instanceUrl.trim().isEmpty()) {
                    instanceUrl = PREFERENCES.DEFAULT_INSTANCE;
                }
                
                // First check for plugin updates
                checkForPluginUpdates(instanceUrl);
                
                // Then check for announcements
                checkForAnnouncements(instanceUrl);
                
            } catch (Exception e) {
                logger.warn("Failed to fetch updates and announcements, error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Processes markdown-style formatting in text for JavaFX display.
     * Converts **bold** text to remove the asterisks (JavaFX doesn't support markdown natively).
     * This is a simple processor for basic formatting.
     * 
     * @param text the text with markdown-style formatting
     * @return processed text suitable for JavaFX Label
     */
    private String processMarkdownForJavaFX(String text) {
        if (text == null) return "";
        
        // Remove markdown bold markers (**text** -> text)
        // Note: JavaFX Label doesn't support rich text, so we just remove the markers
        String processed = text.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        
        return processed;
    }

    /**
     * Extracts the UUID from the announcement JSON data.
     * 
     * @param announcementJson the JsonObject containing the announcement data
     * @return UUID string, or null if not present
     */
    private String getAnnouncementUUID(JsonObject announcementJson) {
        return announcementJson.getString(JSON_FIELDS.UUID, null);
    }

    /**
     * Checks if an announcement has already been displayed.
     * 
     * @param announcementJson the JsonObject containing the announcement data
     * @return true if the announcement has been displayed before, false otherwise
     */
    private boolean hasAnnouncementBeenDisplayed(JsonObject announcementJson) {
        String uuid = getAnnouncementUUID(announcementJson);
        if (uuid == null) {
            logger.warn("Announcement missing UUID field, treating as not displayed");
            return false;
        }
        
        Preferences prefs = getPreferencesNode();
        String displayedUUIDs = prefs.get(PREFERENCES.PREF_DISPLAYED_ANNOUNCEMENTS, "");
        
        boolean wasDisplayed = displayedUUIDs.contains(uuid);
        if (wasDisplayed) {
            logger.debug("Announcement already displayed (UUID: " + uuid + ")");
        }
        return wasDisplayed;
    }

    /**
     * Marks an announcement as displayed by storing its UUID in preferences.
     * 
     * @param announcementJson the JsonObject containing the announcement data
     */
    private void markAnnouncementAsDisplayed(JsonObject announcementJson) {
        String uuid = getAnnouncementUUID(announcementJson);
        if (uuid == null) {
            logger.warn("Cannot mark announcement as displayed - missing UUID field");
            return;
        }
        
        Preferences prefs = getPreferencesNode();
        String displayedUUIDs = prefs.get(PREFERENCES.PREF_DISPLAYED_ANNOUNCEMENTS, "");
        
        if (!displayedUUIDs.contains(uuid)) {
            String updatedUUIDs = displayedUUIDs.isEmpty() ? uuid : displayedUUIDs + "," + uuid;
            prefs.put(PREFERENCES.PREF_DISPLAYED_ANNOUNCEMENTS, updatedUUIDs);
            logger.debug("Marked announcement as displayed (UUID: " + uuid + ")");
        }
    }

    /**
     * Schedules the information popup to appear after a delay.
     * Called during initialization to show helpful information to users.
     */
    private void scheduleInformationPopup() {
        // Create timeline to show popup after delay
        Timeline delayTimeline = createTrackedTimeline(
            new KeyFrame(Duration.seconds(ANIMATIONS.INFO_POPUP_DELAY_SECONDS), 
                        e -> showInformationPopup())
        );
        delayTimeline.play();
        
        logger.debug("Information popup scheduled to appear in " + ANIMATIONS.INFO_POPUP_DELAY_SECONDS + " seconds");
    }

    /**
     * Shows announcements sequentially, one after another.
     * Each dialog waits for the previous one to be closed before showing the next.
     * 
     * @param announcements list of announcements to show
     * @param currentIndex index of the current announcement to show
     */
    private void showAnnouncementsSequentially(List<JsonObject> announcements, int currentIndex) {
        if (currentIndex >= announcements.size()) {
            // All announcements have been shown
            logger.debug("Finished showing " + announcements.size() + " announcements");
            return;
        }
        
        JsonObject announcement = announcements.get(currentIndex);

        String title = announcement.getString(JSON_FIELDS.TITLE, DIALOGS.DEFAULT_ANNOUNCEMENT_TITLE);
        String header = announcement.getString(JSON_FIELDS.HEADER, DIALOGS.DEFAULT_ANNOUNCEMENT_HEADER);

        String message = announcement.getString(JSON_FIELDS.MESSAGE, "");

        if (message.isEmpty()) {
            logger.debug(
                "Skipping announcement " + (currentIndex + 1) + " of " + 
                announcements.size() + ": " + title + " because it has no message"
            );
            return;
        }
        
        logger.debug("Showing announcement " + (currentIndex + 1) + " of " + announcements.size() + ": " + title);
        
        // Create the dialog
        Dialog<Void> infoDialog = createInformationDialog(title, header, message);
        
        // Mark this announcement as displayed
        markAnnouncementAsDisplayed(announcement);
        
        // Set up callback to show next announcement when this one is closed
        infoDialog.setOnHidden(e -> {
            // Show next announcement after a brief delay
            Timeline nextAnnouncementDelay = createTrackedTimeline(
                new KeyFrame(Duration.millis(500), event -> 
                    showAnnouncementsSequentially(announcements, currentIndex + 1))
            );
            nextAnnouncementDelay.play();
        });
        
        // Show the dialog
        infoDialog.show();
    }

    /**
     * Creates an information dialog with the provided title, header, and message.
     * This is extracted from showInformationDialog to allow reuse.
     * 
     * @param title the dialog title
     * @param header the header text to display in bold
     * @param message the message content
     * @return the created dialog
     */
    private Dialog<Void> createInformationDialog(String title, String header, String message) {
        try {
            // Create the dialog with Material Design styling
            Dialog<Void> infoDialog = new Dialog<>();
            infoDialog.setTitle(title);
            infoDialog.setHeaderText(null); // No header for cleaner Material Design look
            
            // Set dialog properties to ensure all content is visible
            DialogPane dialogPane = infoDialog.getDialogPane();
            dialogPane.setMinWidth(DIMENSIONS.INFO_DIALOG_MIN_WIDTH);
            dialogPane.setPrefWidth(DIMENSIONS.INFO_DIALOG_PREF_WIDTH);
            dialogPane.setMinHeight(DIMENSIONS.INFO_DIALOG_MIN_HEIGHT);
            dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
            dialogPane.setMaxWidth(DIMENSIONS.INFO_DIALOG_PREF_WIDTH + 100); // Allow some expansion
            
            // Apply Material Design dialog styling
            dialogPane.setStyle(STYLES.MATERIAL_INFO_DIALOG_STYLE);
            
            // Create content layout with Material Design principles
            VBox content = new VBox();
            content.setStyle(STYLES.MATERIAL_INFO_CONTENT_STYLE);
            content.setMinHeight(Region.USE_PREF_SIZE);
            content.setPrefHeight(Region.USE_COMPUTED_SIZE);
            
            // Create title label with header from API
            Label titleLabel = new Label(header);
            titleLabel.setStyle(STYLES.MATERIAL_INFO_TITLE_STYLE);
            
            // Create message label for plain text content
            Label messageLabel = new Label();
            messageLabel.setStyle(STYLES.MATERIAL_INFO_TEXT_STYLE);
            messageLabel.setWrapText(true);
            messageLabel.setMinWidth(DIMENSIONS.INFO_DIALOG_MIN_WIDTH - 48); // Account for padding
            messageLabel.setPrefWidth(DIMENSIONS.INFO_DIALOG_PREF_WIDTH - 48); // Account for padding
            messageLabel.setMaxWidth(DIMENSIONS.INFO_DIALOG_PREF_WIDTH - 48); // Account for padding
            messageLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
            messageLabel.setMinHeight(Region.USE_PREF_SIZE);
            
            // Process the message to convert markdown-style formatting to JavaFX
            String processedMessage = processMarkdownForJavaFX(message);
            messageLabel.setText(processedMessage);
            
            // Add components to content
            content.getChildren().addAll(titleLabel, messageLabel);
            
            // Set content
            dialogPane.setContent(content);
            
            // Add Material Design button
            ButtonType gotItButton = BUTTON_TYPES.FAST_GOT_IT;
            dialogPane.getButtonTypes().add(gotItButton);
            
            // Style the button with Material Design - delay to ensure button exists
            Platform.runLater(() -> {
                javafx.scene.control.Button button = (javafx.scene.control.Button) dialogPane.lookupButton(gotItButton);
                if (button != null) {
                    // Set initial style
                    button.setStyle(STYLES.MATERIAL_BUTTON_STYLE);
                    
                    // Add hover effects that maintain consistent size and text
                    button.setOnMouseEntered(e -> {
                        if (!button.getStyle().equals(STYLES.MATERIAL_BUTTON_HOVER_STYLE)) {
                            button.setStyle(STYLES.MATERIAL_BUTTON_HOVER_STYLE);
                        }
                    });
                    
                    button.setOnMouseExited(e -> {
                        if (!button.getStyle().equals(STYLES.MATERIAL_BUTTON_STYLE)) {
                            button.setStyle(STYLES.MATERIAL_BUTTON_STYLE);
                        }
                    });
                    
                    // Ensure button text never changes
                    button.setText(DIALOGS.BUTTON_GOT_IT);
                }
            });
            
            // Set owner for proper modal behavior
            if (speleoDBAnchorPane.getScene() != null && speleoDBAnchorPane.getScene().getWindow() != null) {
                infoDialog.initOwner(speleoDBAnchorPane.getScene().getWindow());
            }
            
            // Add fade-in animation for smooth appearance
            dialogPane.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(ANIMATIONS.FADE_IN_DURATION_MILLIS), dialogPane);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            
            // After showing, ensure proper sizing
            Platform.runLater(() -> {
                dialogPane.autosize();
            });
            
            fadeIn.play();
            
            logger.debug("Information popup displayed with Material Design styling and content-based sizing");
            
            return infoDialog;
            
        } catch (Exception e) {
            logger.error("Error creating information popup", e);
            // Return a simple fallback dialog
            Dialog<Void> fallbackDialog = new Dialog<>();
            fallbackDialog.setTitle(title);
            fallbackDialog.setContentText(message);
            fallbackDialog.getDialogPane().getButtonTypes().add(BUTTON_TYPES.FAST_GOT_IT);
            return fallbackDialog;
        }
    }

    /**
     * Checks for plugin updates and handles the update process if available.
     * 
     * @param instanceUrl the SpeleoDB instance URL to check for updates
     */
    private void checkForPluginUpdates(String instanceUrl) {
        try {
            logger.debug(MESSAGES.UPDATE_CHECK_STARTING);
            
            SpeleoDBService tempService = new SpeleoDBService(this);
            JsonArray releases = tempService.fetchPluginReleases(instanceUrl);
            
            if (releases.isEmpty()) {
                logger.info(String.format(MESSAGES.UPDATE_NOT_AVAILABLE, SpeleoDBConstants.VERSION));
                return;
            }
            
            // Find the latest version
            JsonObject latestPluginJson = null;
            String latestPluginVersion = null;
            
            for (JsonValue releaseJsonValue : releases) {
                JsonObject releaseJson = releaseJsonValue.asJsonObject();
                String pluginVersion = releaseJson.getString(JSON_FIELDS.PLUGIN_VERSION, null);
                
                if (pluginVersion != null) {
                    if (latestPluginVersion == null || compareVersions(pluginVersion, latestPluginVersion) > 0) {
                        latestPluginVersion = pluginVersion;
                        latestPluginJson = releaseJson;
                    }
                }
            }
            
            if (latestPluginJson == null || latestPluginVersion == null) {
                // No candidate for update found
                logger.info(String.format(MESSAGES.UPDATE_NOT_AVAILABLE, SpeleoDBConstants.VERSION));
                return;
            }
            
            // Compare with current version
            if (SpeleoDBConstants.VERSION != null && compareVersions(latestPluginVersion, SpeleoDBConstants.VERSION) > 0) {
                logger.info(String.format(MESSAGES.UPDATE_AVAILABLE, latestPluginVersion));
                
                // Download and install the update
                downloadAndInstallUpdate(latestPluginJson, latestPluginVersion);
                
            } else {
                logger.info(String.format(MESSAGES.UPDATE_NOT_AVAILABLE, SpeleoDBConstants.VERSION));
            }
            
        } catch (Exception e) {
            logger.warn(String.format(MESSAGES.UPDATE_CHECK_FAILED, e.getMessage()));
        }
    }
    
    /**
     * Downloads and installs a plugin update.
     * 
     * @param release the JsonObject containing release information
     * @param version the version string of the update
     */
    private void downloadAndInstallUpdate(JsonObject release, String version) {
        try {
            String downloadUrl = release.getString(JSON_FIELDS.DOWNLOAD_URL, null);
            String expectedHash = release.getString(JSON_FIELDS.SHA256_HASH, null);
            String changelog = release.getString(JSON_FIELDS.CHANGELOG, "");
            
            if (downloadUrl == null || expectedHash == null) {
                logger.error("Invalid release data: missing download URL or hash");
                return;
            }
            
            logger.info(String.format(MESSAGES.UPDATE_DOWNLOAD_STARTING, version));
            
            // Download the file
            byte[] fileData = downloadFile(downloadUrl);
            
            // Verify SHA256 hash
            if (!verifyFileHash(fileData, expectedHash)) {
                logger.error(MESSAGES.UPDATE_HASH_VERIFICATION_FAILED);
                return;
            }
            
            // Create plugins directory if it doesn't exist
            Path pluginsDir = Paths.get(PATHS.PLUGINS_DIR);
            Files.createDirectories(pluginsDir);
            
            // Extract filename from download URL to preserve the exact filename
            String fileName;
            URI downloadUri = new URI(downloadUrl);
            String urlPath = downloadUri.getPath();
            fileName = urlPath.substring(urlPath.lastIndexOf('/') + 1);
            
            // Validate extracted filename
            if (fileName.isEmpty() || !fileName.endsWith(PATHS.JAR_FILE_EXTENSION)) {
                throw new IllegalArgumentException("Invalid filename extracted from download URL: " + fileName);
            }
            
            // Save the file to plugins directory with .new extension
            Path targetFile = pluginsDir.resolve(fileName + ".new");
            Files.write(targetFile, fileData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            logger.info(String.format(MESSAGES.UPDATE_INSTALL_SUCCESS, version));
            
            // Show success dialog on UI thread
            Platform.runLater(() -> showUpdateSuccessDialog(version, changelog));
            
        } catch (Exception e) {
            logger.error(String.format(MESSAGES.UPDATE_DOWNLOAD_FAILED, e.getMessage()), e);
        }
    }
    
    /**
     * Downloads a file from the given URL.
     * 
     * @param url the URL to download from
     * @return byte array containing the file data
     * @throws Exception if download fails
     */
    private byte[] downloadFile(String url) throws Exception {
        URI uri = new URI(url);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(java.time.Duration.ofSeconds(NETWORK.DOWNLOAD_TIMEOUT_SECONDS))
                .build();
                
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(NETWORK.CONNECT_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)  // Follow redirects automatically
                .build();
                
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() != HTTP_STATUS.OK) {
            throw new Exception("HTTP " + response.statusCode() + " when downloading update");
        }
        
        return response.body();
    }
    
    /**
     * Verifies the SHA256 hash of file data.
     * 
     * @param fileData the file data to verify
     * @param expectedHash the expected SHA256 hash (hex string)
     * @return true if hash matches, false otherwise
     */
    private boolean verifyFileHash(byte[] fileData, String expectedHash) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileData);
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String actualHash = hexString.toString();
            return actualHash.equalsIgnoreCase(expectedHash);
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to verify file hash: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Deletes plugin jar files matching the specified glob patterns from the plugins directory.
     * 
     * @param patterns glob patterns to match files for deletion (e.g., "*.jar", "plugin-*.jar")
     * @return number of files successfully deleted
     */
    private int deletePluginFiles(String... patterns) {
        int deletedCount = 0;
        Path pluginsDir = Paths.get(PATHS.PLUGINS_DIR);
        
        if (!Files.exists(pluginsDir)) {
            logger.debug("Plugins directory does not exist: " + pluginsDir);
            return 0;
        }
        
        for (String pattern : patterns) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, pattern)) {
                for (Path file : stream) {
                    try {
                        String fileName = file.getFileName().toString();
                        Files.delete(file);
                        logger.debug("Deleted old plugin file: " + fileName);
                        deletedCount++;
                    } catch (IOException e) {
                        logger.warn("Failed to delete plugin file: " + file.getFileName() + " - " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to scan plugins directory with pattern '" + pattern + "': " + e.getMessage());
            }
        }
        
        return deletedCount;
    }
    
    /**
     * Cleans up old plugin files during initialization.
     * Only removes the old filename pattern to prevent conflicts.
     */
    public void cleanupOldPlugins() {
        try {
            // Only delete files with the old naming convention during initialization
            int deletedCount = deletePluginFiles("com.arianesline.ariane.plugin.speleoDB-*.jar");
            
            if (deletedCount > 0) {
                logger.info("Cleaned up " + deletedCount + " old plugin file(s) during initialization");
            }
        } catch (Exception e) {
            logger.warn("Error during plugin cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Compares two version strings in CalVer format (YYYY.MM.DD).
     * Made package-private to allow use by SpeleoDBService for version bounds checking.
     * 
     * @param version1 first version string
     * @param version2 second version string
     * @return positive if version1 > version2, negative if version1 < version2, 0 if equal
     */
    static int compareVersions(String version1, String version2) {
        if (version1 == null && version2 == null) return 0;
        if (version1 == null) return -1;
        if (version2 == null) return 1;
        
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            
            int comparison = Integer.compare(num1, num2);
            if (comparison != 0) {
                return comparison;
            }
        }
        
        return 0;
    }
    
    /**
     * Parses a version part to integer, handling non-numeric parts.
     * 
     * @param part the version part string
     * @return integer value of the part, or 0 if not numeric
     */
    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Shows a success dialog when an update has been installed.
     * 
     * @param version the version that was installed
     * @param changelog the changelog for the update
     */
    private void showUpdateSuccessDialog(String version, String changelog) {
        try {
            Dialog<Void> updateDialog = new Dialog<>();
            updateDialog.setTitle(MESSAGES.UPDATE_DIALOG_TITLE);
            updateDialog.setHeaderText(null);
            
            DialogPane dialogPane = updateDialog.getDialogPane();
            dialogPane.setMinWidth(DIMENSIONS.INFO_DIALOG_MIN_WIDTH);
            dialogPane.setPrefWidth(DIMENSIONS.INFO_DIALOG_PREF_WIDTH);
            dialogPane.setMinHeight(DIMENSIONS.INFO_DIALOG_MIN_HEIGHT);
            dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
            
            dialogPane.setStyle(STYLES.MATERIAL_INFO_DIALOG_STYLE);
            
            VBox content = new VBox();
            content.setStyle(STYLES.MATERIAL_INFO_CONTENT_STYLE);
            
            Label titleLabel = new Label(MESSAGES.UPDATE_DIALOG_HEADER);
            titleLabel.setStyle(STYLES.MATERIAL_INFO_TITLE_STYLE);
            
            Label messageLabel = new Label(String.format(MESSAGES.UPDATE_DOWNLOAD_SUCCESS, version));
            messageLabel.setStyle(STYLES.MATERIAL_INFO_TEXT_STYLE);
            messageLabel.setWrapText(true);
            
            Label changelogLabel = new Label(changelog);
            changelogLabel.setStyle(STYLES.MATERIAL_INFO_TEXT_STYLE);
            changelogLabel.setWrapText(true);
            
            // Add restart warning with material design warning style
            Label restartWarningLabel = new Label(MESSAGES.UPDATE_RESTART_WARNING);
            restartWarningLabel.setStyle(STYLES.MATERIAL_WARNING_TEXT_STYLE);
            restartWarningLabel.setWrapText(true);
            restartWarningLabel.setMinWidth(DIMENSIONS.INFO_DIALOG_MIN_WIDTH - 48); // Account for padding
            restartWarningLabel.setPrefWidth(DIMENSIONS.INFO_DIALOG_PREF_WIDTH - 48); // Account for padding
            restartWarningLabel.setMaxWidth(DIMENSIONS.INFO_DIALOG_PREF_WIDTH - 48); // Account for padding
            
            content.getChildren().addAll(titleLabel, messageLabel, changelogLabel, restartWarningLabel);
            dialogPane.setContent(content);
            
            ButtonType gotItButton = BUTTON_TYPES.FAST_GOT_IT;
            dialogPane.getButtonTypes().add(gotItButton);
            
            Platform.runLater(() -> {
                javafx.scene.control.Button button = (javafx.scene.control.Button) dialogPane.lookupButton(gotItButton);
                if (button != null) {
                    button.setStyle(STYLES.MATERIAL_BUTTON_STYLE);
                    button.setOnMouseEntered(e -> button.setStyle(STYLES.MATERIAL_BUTTON_HOVER_STYLE));
                    button.setOnMouseExited(e -> button.setStyle(STYLES.MATERIAL_BUTTON_STYLE));
                }
            });
            
            if (speleoDBAnchorPane.getScene() != null && speleoDBAnchorPane.getScene().getWindow() != null) {
                updateDialog.initOwner(speleoDBAnchorPane.getScene().getWindow());
            }
            
            updateDialog.show();
            
        } catch (Exception e) {
            logger.error("Error showing update success dialog", e);
        }
    }
    
    /**
     * Checks for announcements and displays them.
     * 
     * @param instanceUrl the SpeleoDB instance URL to check for announcements
     */
    private void checkForAnnouncements(String instanceUrl) {
        try {
            SpeleoDBService tempService = new SpeleoDBService(this);
            JsonArray announcements = tempService.fetchAnnouncements(instanceUrl);
            
            // Filter out announcements that have already been displayed
            List<JsonObject> unshownAnnouncements = new ArrayList<>();
            for (JsonValue item : announcements) {
                JsonObject announcement = item.asJsonObject();
                if (!hasAnnouncementBeenDisplayed(announcement)) {
                    unshownAnnouncements.add(announcement);
                }
            }
            
            if (!unshownAnnouncements.isEmpty()) {
                // Show announcements sequentially
                Platform.runLater(() -> showAnnouncementsSequentially(unshownAnnouncements, 0));
            } else {
                // No new announcements to show
                logger.debug("No new announcements to display");
            }
            
        } catch (Exception e) {
            logger.warn("Failed to fetch announcements, error: " + e.getMessage());
        }
    }



}
