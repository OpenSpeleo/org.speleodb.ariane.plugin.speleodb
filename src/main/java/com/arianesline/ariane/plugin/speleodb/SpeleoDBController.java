package com.arianesline.ariane.plugin.speleodb;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import static com.arianesline.ariane.plugin.speleodb.SpeleoDBAccessLevel.READ_ONLY;

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
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.util.Duration;

/**
 * Controller for managing the SpeleoDB user interface.
 * Delegates server communication to SpeleoDBService.
 */
public class SpeleoDBController implements Initializable {
    public static AtomicInteger messageIndexCounter = new AtomicInteger(0);
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
    private CheckBox rememberCredentialsCheckBox;
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
    private TextArea serverLog;
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

    // SpeleoDBService instance for handling server communication.
    private SpeleoDBService speleoDBService;

    // Constants for Preferences keys and default values.
    private static final String PREF_EMAIL = "SDB_EMAIL";
    private static final String PREF_PASSWORD = "SDB_PASSWORD";
    private static final String PREF_OAUTH_TOKEN = "SDB_OAUTH_TOKEN";
    private static final String PREF_INSTANCE = "SDB_INSTANCE";
    private static final String PREF_SAVE_CREDS = "SDB_SAVECREDS";
    private static final String DEFAULT_INSTANCE = "www.speleoDB.org";



    // Internal Controller Data
    private JsonObject currentProject = null;
    
    // Sorting state
    public enum SortMode { BY_NAME, BY_DATE }
    private SortMode currentSortMode = SortMode.BY_NAME; // Default to sort by name
    
    // Store the original project data for sorting without API calls
    private JsonArray cachedProjectList = null;
    
    // Pre-warmed modal for instant display (initialized once)
    private Alert preWarmedModal = null;
    
    // Modal performance optimization - pre-created button types
    private static final ButtonType FAST_YES = new ButtonType("Yes");
    private static final ButtonType FAST_NO = new ButtonType("No");
    
    // Pre-warmed save modal for instant display (initialized once)
    private Dialog<String> preWarmedSaveModal = null;
    private TextField preWarmedSaveMessageField = null;
    
    // Save modal performance optimization - pre-created button types
    private static final ButtonType FAST_SAVE = new ButtonType("Save Project", ButtonType.OK.getButtonData());
    private static final ButtonType FAST_CANCEL = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());
    
    /**
     * Pre-warms the modal system to eliminate first-time display lag.
     * Called during initialization to prepare JavaFX Alert system.
     */
    private void preWarmModalSystem() {
        // Create and configure a modal to initialize JavaFX Alert system
        Platform.runLater(() -> {
            if (preWarmedModal == null) {
                preWarmedModal = new Alert(Alert.AlertType.CONFIRMATION);
                preWarmedModal.setTitle("");
                preWarmedModal.setHeaderText(null);
                preWarmedModal.setContentText("");
                preWarmedModal.getButtonTypes().setAll(FAST_YES, FAST_NO);
                
                // Initialize owner to speed up future displays
                try {
                    if (speleoDBAnchorPane.getScene() != null && speleoDBAnchorPane.getScene().getWindow() != null) {
                        preWarmedModal.initOwner(speleoDBAnchorPane.getScene().getWindow());
                    }
                } catch (Exception e) {
                    // Ignore initialization errors - modal will still work
                }
            }
            
            // Pre-warm save modal system
            preWarmSaveModalSystem();
        });
    }
    
    /**
     * Pre-warms the save modal system to eliminate first-time display lag.
     * Creates and configures the save dialog with its UI components for reuse.
     */
    private void preWarmSaveModalSystem() {
        if (preWarmedSaveModal == null) {
            // Create the dialog once
            preWarmedSaveModal = new Dialog<>();
            preWarmedSaveModal.setTitle("Save Project on SpeleoDB");
            preWarmedSaveModal.getDialogPane().getButtonTypes().addAll(FAST_SAVE, FAST_CANCEL);
            
            // Create the content layout once
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 20, 10, 20));
            
            // Set column constraints - column 1 (text field) should grow
            ColumnConstraints col1 = new ColumnConstraints();
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setHgrow(Priority.ALWAYS);
            col2.setFillWidth(true);
            grid.getColumnConstraints().addAll(col1, col2);
            
            Label messageLabel = new Label("What did you change?");
            preWarmedSaveMessageField = new TextField();
            preWarmedSaveMessageField.setText("");
            preWarmedSaveMessageField.setMaxWidth(Double.MAX_VALUE);
            
            grid.add(messageLabel, 0, 0);
            grid.add(preWarmedSaveMessageField, 1, 0);
            
            preWarmedSaveModal.getDialogPane().setContent(grid);
            
            // Set minimum width for the dialog
            preWarmedSaveModal.getDialogPane().setMinWidth(500);
            preWarmedSaveModal.getDialogPane().setPrefWidth(500);
            
            // Set result converter once
            preWarmedSaveModal.setResultConverter(dialogButton -> {
                if (dialogButton == FAST_SAVE) {
                    return preWarmedSaveMessageField.getText();
                }
                return null;
            });
            
            // Initialize owner to speed up future displays
            try {
                if (speleoDBAnchorPane.getScene() != null && speleoDBAnchorPane.getScene().getWindow() != null) {
                    preWarmedSaveModal.initOwner(speleoDBAnchorPane.getScene().getWindow());
                }
            } catch (Exception e) {
                // Ignore initialization errors - modal will still work
            }
        }
    }

    /**
     * ULTRA-FAST universal confirmation modal. Maximum performance optimization.
     * Uses pre-warmed modal + pre-created buttons for instant display.
     * 
     * @param title the dialog title
     * @param message the main message to display
     * @param positiveText text for the "Yes" button (ignored for performance - uses "Yes")
     * @param negativeText text for the "No" button (ignored for performance - uses "No")
     * @return true if user clicked positive button, false otherwise
     */
    private boolean showConfirmationModal(String title, String message, String positiveText, String negativeText) {
        // Use pre-warmed modal for maximum speed, fallback to new modal if needed
        Alert alert = (preWarmedModal != null) ? preWarmedModal : new Alert(Alert.AlertType.CONFIRMATION);
        
        // Configure the modal (minimal operations for speed)
        alert.setTitle(title);
        alert.setContentText(message);
        
        // Use pre-created buttons for maximum performance
        if (alert == preWarmedModal) {
            // Already has FAST_YES and FAST_NO buttons
        } else {
            // Fallback for new modal
            alert.getButtonTypes().setAll(FAST_YES, FAST_NO);
        }
        
        // Show and get result (optimized path)
        return alert.showAndWait()
                   .map(response -> response == FAST_YES)
                   .orElse(false);
    }

    /**
     * Constructor - SpeleoDBService initialization moved to initialize() method.
     */
    public SpeleoDBController() {
        // Service initialization moved to initialize() to avoid 'this' escape
    }

    // ========================= UTILITY FUNCTIONS ========================= //

    /**
     * Logs a message to the server log text area.
     *
     * @param message the message to log.
     */
    private void logMessage(String message) {
        final int messageIndex = messageIndexCounter.incrementAndGet();
        Platform.runLater(() -> {
            serverLog.appendText(messageIndex + "-" + message + System.lineSeparator());
        });
    }

    /**
     * Shows a success animation overlay with customizable message.
     * 
     * @param message the success message to display (optional, defaults to "Success")
     */
    private void showSuccessAnimation(String message) {
        Platform.runLater(() -> {
            // Use default message if none provided
            String displayMessage = (message == null || message.trim().isEmpty()) ? "Success" : message;
            
            // Create temporary success indicator
            Label successLabel = new Label("✓ " + displayMessage);
            successLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                                "-fx-padding: 15 20; -fx-background-radius: 8; -fx-font-size: 16px; " +
                                "-fx-font-weight: bold; -fx-border-radius: 8; " +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 2);");
            
            // Position it in the center-top of the main pane
            AnchorPane.setTopAnchor(successLabel, 20.0);
            AnchorPane.setLeftAnchor(successLabel, 50.0);
            AnchorPane.setRightAnchor(successLabel, 50.0);
            successLabel.setMaxWidth(Double.MAX_VALUE);
            successLabel.setAlignment(javafx.geometry.Pos.CENTER);
            
            speleoDBAnchorPane.getChildren().add(successLabel);
            
            // Animate the success message
            successLabel.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), successLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
            // Auto-hide after 4 seconds
            Timeline hideTimeline = new Timeline(
                new KeyFrame(Duration.seconds(4), e -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(400), successLabel);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setOnFinished(event -> speleoDBAnchorPane.getChildren().remove(successLabel));
                    fadeOut.play();
                })
            );
            hideTimeline.play();
        });
    }

    /**
     * Shows a success animation with the default "Success" message.
     */
    private void showSuccessAnimation() {
        showSuccessAnimation("Success");
    }

    /**
     * Shows an error animation overlay with customizable message.
     * 
     * @param message the error message to display (optional, defaults to "Error")
     */
    private void showErrorAnimation(String message) {
        Platform.runLater(() -> {
            // Use default message if none provided
            String displayMessage = (message == null || message.trim().isEmpty()) ? "Error" : message;
            
            // Create temporary error indicator
            Label errorLabel = new Label("✗ " + displayMessage);
            errorLabel.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; " +
                              "-fx-padding: 15 20; -fx-background-radius: 8; -fx-font-size: 16px; " +
                              "-fx-font-weight: bold; -fx-border-radius: 8; " +
                              "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 2);");
            
            // Position it in the center-top of the main pane
            AnchorPane.setTopAnchor(errorLabel, 20.0);
            AnchorPane.setLeftAnchor(errorLabel, 50.0);
            AnchorPane.setRightAnchor(errorLabel, 50.0);
            errorLabel.setMaxWidth(Double.MAX_VALUE);
            errorLabel.setAlignment(javafx.geometry.Pos.CENTER);
            
            speleoDBAnchorPane.getChildren().add(errorLabel);
            
            // Animate the error message
            errorLabel.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), errorLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
            // Auto-hide after 4 seconds
            Timeline hideTimeline = new Timeline(
                new KeyFrame(Duration.seconds(4), e -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(400), errorLabel);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setOnFinished(event -> speleoDBAnchorPane.getChildren().remove(errorLabel));
                    fadeOut.play();
                })
            );
            hideTimeline.play();
        });
    }

    /**
     * Shows an error animation with the default "Error" message.
     */
    private void showErrorAnimation() {
        showErrorAnimation("Error");
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
                    logMessage("Adding SpeleoDB ID: " + SDB_projectId);
                    speleoDBService.updateFileSpeleoDBId(SDB_projectId);
                    parentPlugin.getSurvey().setExtraData(SDB_projectId);
                    return;
                }

                if (!SDB_mainCaveFileId.equals(SDB_projectId)) {
                    logMessage("Incoherent File ID detected.");
                    logMessage("\t- Previous Value: " + SDB_mainCaveFileId);
                    logMessage("\t- New Value: " + SDB_projectId);
                    parentPlugin.getSurvey().setExtraData(SDB_projectId);
                    logMessage("SpeleoDB ID updated successfully.");
                }


            } catch (Exception e) {
                logMessage("Error checking/updating SpeleoDB ID: " + e.getMessage());
            }
        });
    }

    // ==================== USER PREFERENCES' MANAGEMENT =================== //

    /**
     * Loads user preferences into the UI fields.
     */
    private void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(SpeleoDBController.class);

        rememberCredentialsCheckBox.setSelected(prefs.getBoolean(PREF_SAVE_CREDS, false));
        emailTextField.setText(prefs.get(PREF_EMAIL, ""));

        if (rememberCredentialsCheckBox.isSelected()) {
            passwordPasswordField.setText(prefs.get(PREF_PASSWORD, ""));
            oauthtokenPasswordField.setText(prefs.get(PREF_OAUTH_TOKEN, ""));
        }

        instanceTextField.setText(prefs.get(PREF_INSTANCE, DEFAULT_INSTANCE));
    }

    /**
     * Saves user preferences based on the current UI state.
     */
    private void savePreferences() {
        Preferences prefs = Preferences.userNodeForPackage(SpeleoDBController.class);
        prefs.put(PREF_EMAIL, emailTextField.getText());
        prefs.put(PREF_INSTANCE, instanceTextField.getText());

        if (rememberCredentialsCheckBox.isSelected()) {
            prefs.put(PREF_PASSWORD, passwordPasswordField.getText());
            prefs.put(PREF_OAUTH_TOKEN, oauthtokenPasswordField.getText());
        } else {
            prefs.remove(PREF_PASSWORD);
            prefs.remove(PREF_OAUTH_TOKEN);
        }

        prefs.putBoolean(PREF_SAVE_CREDS, rememberCredentialsCheckBox.isSelected());
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
        connectionButton.setText("CONNECT");
        
        // Initial state: CONNECT and SIGNUP buttons visible 50/50
        javafx.scene.layout.GridPane.setColumnSpan(connectionButton, 1); // Span only 1 column (45%)
        signupButton.setVisible(true);
        
        // Use localhost URL when in debug mode, production URL otherwise
        String aboutUrl;
        boolean isDebugMode = isDebugMode();
        
        if (isDebugMode) {
            aboutUrl = "http://localhost:8000/webview/ariane/";
        } else {
            // On purpose - use the main URL, not the instance one.
            aboutUrl = "https://www.speleoDB.org/webview/ariane/";
        }

        aboutWebView.getEngine().load(aboutUrl);

        // Ensure the About pane is expanded by default in the Accordion
        // This must be done after all UI setup to override Accordion's default behavior
        Platform.runLater(() -> {
            aboutTitlePane.setExpanded(true);
        });

        serverLog.textProperty().addListener((ObservableValue<?> observable, Object oldValue, Object newValue) -> {
            // This will scroll to the bottom - use Double.MIN_VALUE to scroll to the top
            serverLog.setScrollTop(Double.MAX_VALUE);
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
                    showSaveModal();
                }
                // If no active lock, let the key event pass through (don't consume it)
            }
        });
    }
    
    /**
     * Shows a save modal dialog when Ctrl+S / Cmd+S is pressed.
     * OPTIMIZED: Uses pre-warmed dialog with pre-created UI components for instant display.
     */
    private void showSaveModal() {
        // Use pre-warmed save modal for maximum speed, fallback to new modal if needed
        Dialog<String> dialog = (preWarmedSaveModal != null) ? preWarmedSaveModal : createFallbackSaveModal();
        
        // Update dynamic content only
        if (dialog == preWarmedSaveModal) {
            // Reset the text field and update header
            preWarmedSaveMessageField.setText("");
            dialog.setHeaderText("Save Current Project: \"" + currentProject.getString("name") + "\"");
            
            // Set focus on the text field when dialog is shown (only need to set once per show)
            dialog.setOnShown(e -> preWarmedSaveMessageField.requestFocus());
        }
        
        // Show dialog and handle response
        dialog.showAndWait().ifPresent(message -> {
            if (message == null || message.trim().isEmpty()) {
                logMessage("Upload message cannot be empty.");
                showErrorAnimation();
                return;
            }
            
            // Use the shared upload method
            uploadProjectWithMessage(message.trim());
        });
    }
    
    /**
     * Fallback method to create save modal if pre-warming failed.
     * This maintains compatibility but won't be as fast as the pre-warmed version.
     */
    private Dialog<String> createFallbackSaveModal() {
        // Create custom dialog with text field (fallback implementation)
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Save Project on SpeleoDB");
        dialog.setHeaderText("Save Current Project: \"" + currentProject.getString("name") + "\"");
        
        // Set button types
        ButtonType saveButtonType = new ButtonType("Save Project", ButtonType.OK.getButtonData());
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        
        // Create the content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));
        
        // Set column constraints - column 1 (text field) should grow
        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setFillWidth(true);
        grid.getColumnConstraints().addAll(col1, col2);
        
        Label messageLabel = new Label("What did you change?");
        TextField messageField = new TextField();
        messageField.setText("");
        messageField.setMaxWidth(Double.MAX_VALUE);
        
        grid.add(messageLabel, 0, 0);
        grid.add(messageField, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        
        // Set minimum width for the dialog
        dialog.getDialogPane().setMinWidth(500);
        dialog.getDialogPane().setPrefWidth(500);
        
        // Focus on the text field when dialog is shown
        dialog.setOnShown(e -> messageField.requestFocus());
        
        // Convert the result when save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return messageField.getText();
            }
            return null;
        });
        
        return dialog;
    }

    /**
     * Initializes the controller, setting up default UI states and preferences.
     *
     * @param location  the location of the FXML file.
     * @param resources additional resources, such as localized strings.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize service here to avoid 'this' escape in constructor
        this.speleoDBService = new SpeleoDBService(this);
        
        loadPreferences();
        setupUI();
        setupKeyboardShortcuts();
        
        // Initialize sorting button styles (default to sort by name)
        updateSortButtonStyles();
        
        // Pre-warm modal system for instant display (eliminates first-time lag)
        preWarmModalSystem();
        
        // Pre-load countries data for New Project dialog optimization
        NewProjectDialog.preLoadCountriesData();
    }


    // ========================= UI EVENT FUNCTIONS ======================== //

    // ********************* Authentication Management ********************* //

    /**
     * Connects to the SpeleoDB instance using the provided credentials.
     */
    private void connectToSpeleoDB() {

        String email = emailTextField.getText();
        String password = passwordPasswordField.getText();
        String oauthtoken = oauthtokenPasswordField.getText();
        String instance = instanceTextField.getText();

        logMessage("Connecting to " + instance);
        serverProgressIndicator.setVisible(true);

        parentPlugin.executorService.execute(() -> {
            try {
                speleoDBService.authenticate(email, password, oauthtoken, instance);
                logMessage("Connected successfully.");
                savePreferences();

                Platform.runLater(() -> {
                    projectsTitlePane.setVisible(true);
                    projectsTitlePane.setExpanded(true);
                    createNewProjectButton.setDisable(false); // Enable create new project button
                    refreshProjectsButton.setDisable(false); // Enable refresh button
                    connectionButton.setText("DISCONNECT");
                    
                    // When authenticated: DISCONNECT button takes 100% width, SIGNUP button hidden
                    javafx.scene.layout.GridPane.setColumnSpan(connectionButton, 3); // Span all 3 columns
                    signupButton.setVisible(false);
                });

                listProjects();

            } catch (Exception e) {
                logMessage("Connection failed: " + e.getMessage());
                showErrorAnimation();
                Platform.runLater(() -> serverProgressIndicator.setVisible(false));
            }
        });
    }

    /**
     * Disconnects from the SpeleoDB instance.
     */
    private void disconnectFromSpeleoDB() {
        String SDB_instance = speleoDBService.getSDBInstance();
        logMessage("Disconnected from " + SDB_instance);
        speleoDBService.logout();
        projectListView.getItems().clear();
        actionsTitlePane.setVisible(false);
        projectsTitlePane.setVisible(false);
        createNewProjectButton.setDisable(true); // Disable create new project button
        refreshProjectsButton.setDisable(true); // Disable refresh button
        connectionButton.setText("CONNECT");
        
        // When disconnected: CONNECT and SIGNUP buttons visible 50/50
        javafx.scene.layout.GridPane.setColumnSpan(connectionButton, 1); // Span only 1 column (45%)
        signupButton.setVisible(true);
        
        if (!rememberCredentialsCheckBox.isSelected())
            passwordPasswordField.clear();
        logMessage("Disconnected from " + SDB_instance);
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

    // TODO: Add functionality
    //    - TODO: Add the UI part - Form & Button
    //    - TODO: Add the controller code
    //    - TODO: Add the service code
    // Note: Once the project is created:
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
        logMessage("Project listing successful on " + speleoDBService.getSDBInstance());
        
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
            logMessage("No cached project data available");
            return;
        }
        
        Platform.runLater(() -> {
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
                logMessage("Projects sorted by name (A-Z)");
            } else { // BY_DATE
                projects.sort(Comparator.comparing((JsonObject project) -> 
                    project.getString("modified_date", "")).reversed()); // Most recent first
                logMessage("Projects sorted by modified_date (newest first)");
            }
            
            // Create UI elements for sorted projects
            for (JsonObject projectItem : projects) {
                VBox card = createProjectCard(projectItem);
                Button button = createProjectButton(card, projectItem);
                projectListView.getItems().add(button);
            }
            
            // Update button styles to reflect current sort mode
            updateSortButtonStyles();
            
            logMessage("Project list rebuilt with " + projects.size() + " projects");
        });
    }

    private void listProjects() {
        logMessage("Listing Projects on " + speleoDBService.getSDBInstance());

        parentPlugin.executorService.execute(() -> {
            try {
                JsonArray projectList = speleoDBService.listProjects();
                handleProjectListResponse(projectList);
            } catch (Exception e) {
                logMessage("Failed to list projects: " + e.getMessage());
                showErrorAnimation();
            } finally {
                Platform.runLater(() -> serverProgressIndicator.setVisible(false));
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
            logMessage("Cannot refresh projects: Not authenticated");
            return;
        }
        
        logMessage("User requested project list refresh");
        
        // Provide visual feedback
        refreshProjectsButton.setDisable(true);
        refreshProjectsButton.setText("Refreshing...");
        serverProgressIndicator.setVisible(true);
        
        parentPlugin.executorService.execute(() -> {
            try {
                JsonArray projectList = speleoDBService.listProjects();
                handleProjectListResponse(projectList);
                Platform.runLater(() -> logMessage("Project list refreshed successfully"));
            } catch (Exception e) {
                logMessage("Failed to refresh projects: " + e.getMessage());
                showErrorAnimation();
            } finally {
                Platform.runLater(() -> {
                    serverProgressIndicator.setVisible(false);
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
            logMessage("Cannot sort projects: Not authenticated");
            return;
        }
        
        logMessage("User requested sort by name");
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
            logMessage("Cannot sort projects: Not authenticated");
            return;
        }
        
        logMessage("User requested sort by date");
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
            logMessage("Cannot create new project: Not authenticated");
            return;
        }
        
        // Show the new project dialog
        NewProjectDialog dialog = new NewProjectDialog();
        var result = dialog.showAndWait();
        
        if (result.isPresent()) {
            NewProjectDialog.ProjectData projectData = result.get();
            logMessage("Creating new project: " + projectData.name);
            
            // Disable the button while creating project
            createNewProjectButton.setDisable(true);
            createNewProjectButton.setText("Creating...");
            serverProgressIndicator.setVisible(true);
            
            parentPlugin.executorService.execute(() -> {
                try {
                    // Create the project via API
                    JsonObject createdProject = speleoDBService.createProject(
                        projectData.name,
                        projectData.description, 
                        projectData.countryCode,
                        projectData.latitude,
                        projectData.longitude
                    );
                    
                    logMessage("Project '" + projectData.name + "' created successfully!");
                    logMessage("Project ID: " + createdProject.getString("id"));
                    
                    // Use centralized lock acquisition system with async heavy operations
                    parentPlugin.executorService.execute(() -> {
                        LockResult lockResult = acquireProjectLock(createdProject, "project creation");
                        
                        if (lockResult.isAcquired()) {
                            try {
                                // Heavy operations on background thread
                                logMessage("Setting up new project files...");
                                
                                // Create an empty TML file for the new project
                                String projectId = createdProject.getString("id");
                                Path emptyTmlFile = createEmptyTmlFile(projectId, projectData.name);
                                
                                // Load the survey file and update SpeleoDB ID through normal flow
                                parentPlugin.loadSurvey(emptyTmlFile.toFile());
                                checkAndUpdateSpeleoDBId(createdProject);
                                
                                // Update UI on JavaFX thread (lightweight operations only)
                                Platform.runLater(() -> {
                                    // Set as current project and enable UI controls
                                    currentProject = createdProject;
                                    actionsTitlePane.setVisible(true);
                                    actionsTitlePane.setExpanded(true);
                                    actionsTitlePane.setText("Actions on `" + createdProject.getString("name") + "`.");
                                    uploadButton.setDisable(false);
                                    unlockButton.setDisable(false);
                                    
                                    // Show success animation and refresh (lightweight)
                                    showSuccessAnimation("Project created and locked for editing!");
                                    listProjects();
                                });
                                
                            } catch (Exception e) {
                                logMessage("Error setting up new project: " + e.getMessage());
                                Platform.runLater(() -> {
                                    showErrorAnimation("Project created but setup failed");
                                    listProjects();
                                });
                            }
                        } else {
                            // Lock acquisition failed - update UI
                            Platform.runLater(() -> {
                                showSuccessAnimation("Project created (lock acquisition failed)");
                                listProjects();
                            });
                        }
                    });
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        logMessage("Failed to create project: " + e.getMessage());
                        showErrorAnimation();
                    });
                } finally {
                    Platform.runLater(() -> {
                        serverProgressIndicator.setVisible(false);
                        createNewProjectButton.setDisable(false);
                        createNewProjectButton.setText("Create New Project");
                    });
                }
            });
        } else {
            logMessage("New project creation cancelled");
        }
    }
    
    /**
     * Creates an empty TML file for a new project by copying the template file.
     * Uses the same approach as downloading TML files - simple binary file copy.
     * The SpeleoDB ID will be updated through the normal application flow.
     * 
     * @param projectId the SpeleoDB project ID (used for filename)
     * @param projectName the human-readable project name (for logging only)
     * @return Path to the created TML file
     * @throws IOException if file creation fails
     */
    private Path createEmptyTmlFile(String projectId, String projectName) throws IOException {
        Path tmlFilePath = Paths.get(SpeleoDBService.ARIANE_ROOT_DIR + File.separator + projectId + ".tml");
        
        // Ensure the .ariane directory exists
        Files.createDirectories(tmlFilePath.getParent());
        
        // Copy the template file from resources (same as download service approach)
        try (var templateStream = getClass().getResourceAsStream("/tml/empty_project.tml")) {
            if (templateStream == null) {
                throw new IOException("Template file /tml/empty_project.tml not found in resources");
            }
            
            // Simple binary copy - same approach as downloadProject()
            Files.copy(templateStream, tmlFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            logMessage("Created TML file from template: " + tmlFilePath.getFileName());
            return tmlFilePath;
        }
    }

    // -------------------------- Project Opening -------------------------- //

    private void clickSpeleoDBProject(ActionEvent e) throws URISyntaxException, IOException, InterruptedException {
        var project = (JsonObject) ((Button) e.getSource()).getUserData();

        parentPlugin.executorService.execute(() -> {
            try {
                // Step 1: Try to acquire lock (if not read-only)
                LockResult lockResult = acquireProjectLock(project, "project opening");
                
                // Step 2: Download project regardless of lock status
                logMessage("Downloading project: " + project.getString("name"));
                Path tml_filepath = speleoDBService.downloadProject(project);

                if (Files.exists(tml_filepath)) {
                    // Step 3: Load the project
                    parentPlugin.loadSurvey(tml_filepath.toFile());
                    logMessage("Download successful: " + project.getString("name"));
                    currentProject = project;
                    checkAndUpdateSpeleoDBId(project);

                    // Step 4: Update UI based on lock status
                    Platform.runLater(() -> {
                        serverProgressIndicator.setVisible(false);
                        actionsTitlePane.setVisible(true);
                        actionsTitlePane.setExpanded(true);
                        actionsTitlePane.setText("Actions on `" + currentProject.getString("name") + "`.");

                        // Enable/disable controls based on lock acquisition
                        boolean hasLock = lockResult.isAcquired();
                        uploadButton.setDisable(!hasLock);
                        unlockButton.setDisable(!hasLock);
                    });
                    
                    // Step 5: Refresh project listing
                    try {
                        logMessage("Refreshing project listing after project opening...");
                        JsonArray projectList = speleoDBService.listProjects();
                        handleProjectListResponse(projectList);
                    } catch (Exception refreshEx) {
                        logMessage("Error refreshing project listing: " + refreshEx.getMessage());
                    }

                } else {
                    logMessage("Download failed");
                    currentProject = null;

                    Platform.runLater(() -> {
                        serverProgressIndicator.setVisible(false);
                        actionsTitlePane.setVisible(false);
                        actionsTitlePane.setExpanded(false);
                        actionsTitlePane.setText("Actions");
                        uploadButton.setDisable(true);
                        unlockButton.setDisable(true);
                    });
                }
            } catch (IOException | InterruptedException | URISyntaxException ex) {
                logMessage("Error opening project: " + ex.getMessage());
                Platform.runLater(() -> {
                    serverProgressIndicator.setVisible(false);
                    actionsTitlePane.setVisible(false);
                    uploadButton.setDisable(true);
                    unlockButton.setDisable(true);
                });
            } finally {
                Platform.runLater(() -> {
                    projectListView.setDisable(false);
                    serverProgressIndicator.setVisible(false);
                });
            }
        });
    }

    /**
     * Handles the action performed when a project button is clicked.
     *
     * @param event       The ActionEvent triggered by the button click.
     * @param projectItem The JsonObject containing project metadata.
     */
    private void handleProjectCardClickAction(ActionEvent event, JsonObject projectItem) {
        // Prevent double click
        projectListView.setDisable(true);

        try {
            String selectedProjectName = projectItem.getString("name");
            String selectedProjectId = projectItem.getString("id");
            
            // Check if user is trying to switch to a different project while having an active lock
            if (hasActiveProjectLock()) {
                String currentProjectId = currentProject.getString("id");
                
                // If clicking on the same project, proceed normally
                if (currentProjectId.equals(selectedProjectId)) {
                    logMessage("Selected project: " + selectedProjectName);
                    clickSpeleoDBProject(event);
                    return;
                }
                
                // Different project selected - show confirmation dialog
                logMessage("Attempting to switch from locked project: " + getCurrentProjectName() + 
                          " to: " + selectedProjectName);
                
                boolean shouldSwitch = showProjectSwitchConfirmation(selectedProjectName);
                
                if (!shouldSwitch) {
                    logMessage("User cancelled project switch. Staying on: " + getCurrentProjectName());
                    Platform.runLater(() -> projectListView.setDisable(false));
                    return;
                }
                
                // User confirmed switch - release current lock first
                logMessage("User confirmed project switch. Releasing lock on: " + getCurrentProjectName());
                
                parentPlugin.executorService.execute(() -> {
                    try {
                        if (speleoDBService.releaseProjectMutex(currentProject)) {
                            logMessage("Successfully released lock on: " + getCurrentProjectName());
                            currentProject = null;
                            
                            Platform.runLater(() -> {
                                actionsTitlePane.setVisible(false);
                                actionsTitlePane.setExpanded(false);
                                
                                // Now proceed with the new project selection
                                try {
                                    logMessage("Proceeding with new project selection: " + selectedProjectName);
                                    clickSpeleoDBProject(event);
                                    
                                    // Refresh project listing after project switch is complete
                                    parentPlugin.executorService.execute(() -> {
                                        try {
                                            logMessage("Refreshing project listing after project switch...");
                                            listProjects();
                                        } catch (Exception e) {
                                            logMessage("Error refreshing project listing: " + e.getMessage());
                                        }
                                    });
                                    
                                } catch (IOException | InterruptedException | URISyntaxException e) {
                                    logMessage("Error switching to new project: " + e.getMessage());
                                    Platform.runLater(() -> projectListView.setDisable(false));
                                }
                            });
                        } else {
                            logMessage("Failed to release lock on: " + getCurrentProjectName() + ". Cannot switch projects.");
                            Platform.runLater(() -> projectListView.setDisable(false));
                        }
                    } catch (IOException | InterruptedException | URISyntaxException e) {
                        logMessage("Error releasing lock: " + e.getMessage());
                        Platform.runLater(() -> projectListView.setDisable(false));
                    }
                });
                
                return;
            }
            
            // No active lock - proceed normally
            logMessage("Selected project: " + selectedProjectName);
            clickSpeleoDBProject(event);

        } catch (IOException | InterruptedException | URISyntaxException e) {
            logMessage("Error handling project action: " + e.getMessage());
            Platform.runLater(() -> projectListView.setDisable(false));
        }

    }

    // ---------------------- Project Mutex Management --------------------- //

    /**
     * Shows a confirmation dialog asking the user if they want to release the current project lock
     * before switching to a different project.
     * OPTIMIZED: Pre-built components and StringBuilder for faster modal loading.
     * 
     * @param newProjectName the name of the project the user wants to switch to
     * @return true if the user wants to release the lock and switch projects, false otherwise
     */
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
            logMessage("User cancelled unlock operation.");
            return;
        }

        logMessage("Unlocking project " + currentProject.getString("name"));
        serverProgressIndicator.setVisible(true);
        uploadButton.setDisable(true);
        unlockButton.setDisable(true);

        parentPlugin.executorService.execute(() -> {
            try {
                if (speleoDBService.releaseProjectMutex(currentProject)) {
                    logMessage("Project " + currentProject.getString("name") + " unlocked");
                    
                    // Show success animation
                    showSuccessAnimation();
                    
                    currentProject = null;

                    Platform.runLater(() -> {
                        actionsTitlePane.setVisible(false);
                        actionsTitlePane.setExpanded(false);
                        projectsTitlePane.setExpanded(true);
                    });

                    listProjects();
                } else {
                    logMessage("Failed to release lock for " + currentProject.getString("name"));
                    showErrorAnimation();
                }
            } catch (IOException | InterruptedException | URISyntaxException e) {
                logMessage("Error releasing lock: " + e.getMessage());
                showErrorAnimation();
            } finally {
                Platform.runLater(() -> {
                    serverProgressIndicator.setVisible(false);
                    uploadButton.setDisable(false);
                    unlockButton.setDisable(false);
                });
            }
        });
    }

    // --------------------- Project Saving and Upload --------------------- //

    /**
     * Shared method to upload a project with a given commit message.
     * 
     * @param commitMessage the commit message for the upload
     */
    private void uploadProjectWithMessage(String commitMessage) {
        //TODO: Find alternative
        parentPlugin.saveSurvey();

      /*
        if (UndoRedo.maxActionNumber > 0) {
            core.mainController.saveTML(false);
        } else {
            logMessage("No changes to the project detected");
            return;
        }
       */

        logMessage("Uploading project " + currentProject.getString("name") + " ...");
        
        serverProgressIndicator.setVisible(true);
        uploadButton.setDisable(true);
        unlockButton.setDisable(true);

        parentPlugin.executorService.execute(() -> {
            try {
                speleoDBService.uploadProject(commitMessage, currentProject);
                logMessage("Upload successful.");
                
                // Show success animation
                showSuccessAnimation();
                
                // Clear the upload message text field after successful upload
                Platform.runLater(() -> {
                    uploadMessageTextField.clear();
                });
                
                // Show confirmation popup asking if user wants to release the write lock
                Platform.runLater(() -> {
                    boolean shouldReleaseLock = showReleaseLockConfirmation();
                    
                    if (shouldReleaseLock) {
                        logMessage("User chose to release the write lock.");
                        // Release the lock in background thread
                        parentPlugin.executorService.execute(() -> {
                            try {
                                if (speleoDBService.releaseProjectMutex(currentProject)) {
                                    logMessage("Project " + currentProject.getString("name") + " unlocked");
                                    currentProject = null;

                                    Platform.runLater(() -> {
                                        actionsTitlePane.setVisible(false);
                                        actionsTitlePane.setExpanded(false);
                                        projectsTitlePane.setExpanded(true);
                                    });

                                    listProjects();
                                } else {
                                    logMessage("Failed to release lock for " + currentProject.getString("name"));
                                    showErrorAnimation();
                                }
                            } catch (IOException | InterruptedException | URISyntaxException e) {
                                logMessage("Error releasing lock: " + e.getMessage());
                                showErrorAnimation();
                            }
                        });
                    } else {
                        logMessage("User chose to keep the write lock.");
                    }
                });
                
            } catch (Exception e) {
                logMessage("Upload failed: " + e.getMessage());
                showErrorAnimation();
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
            logMessage("Upload message cannot be empty.");
            return;
        }
        
        uploadProjectWithMessage(message);
    }

    public void onSignupSpeleoDB(ActionEvent actionEvent) {
        try {
            String instance = instanceTextField.getText().trim();
            if (instance.isEmpty()) {
                instance = DEFAULT_INSTANCE;
            }
            
            String protocol = isDebugMode() ? "http" : "https";
            String signupUrl = protocol + "://" + instance + "/signup/";
            
            java.awt.Desktop.getDesktop().browse(new java.net.URI(signupUrl));
            logMessage("Opening signup page: " + signupUrl);
        } catch (IOException | URISyntaxException e) {
            logMessage("Failed to open signup page: " + e.getMessage());
            showErrorAnimation();
        }
    }
    
    /**
     * Handles the "Learn About" button click event to open the SpeleoDB website.
     */
    @FXML
    public void onLearnAbout(ActionEvent actionEvent) {
        try {
            String speleoBDUrl = "https://www.speleoDB.org";
            
            // Open URL in default browser
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(speleoBDUrl));
                    logMessage("Opened SpeleoDB website in browser: " + speleoBDUrl);
                } else {
                    logMessage("Browser not supported on this system");
                }
            } else {
                logMessage("Desktop operations not supported on this system");
            }
        } catch (IOException | URISyntaxException e) {
            logMessage("Failed to open SpeleoDB website: " + e.getMessage());
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
            speleoDBService.releaseProjectMutex(currentProject);
            currentProject = null;
        }
    }
    
    /**
     * Public accessor for logging messages from the plugin.
     * 
     * @param message the message to log
     */
    public void logMessageFromPlugin(String message) {
        logMessage(message);
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
            if (READ_ONLY.name().equals(permission)) {
                String message = "Project '" + projectName + "' is read-only - no lock needed";
                logMessage("🔒 " + message);
                return LockResult.failure(project, message);
            }
            
            logMessage("🔄 Acquiring lock for " + context + ": " + projectName);
            
            boolean lockAcquired = speleoDBService.acquireOrRefreshProjectMutex(project);
            
            if (lockAcquired) {
                String successMessage = "✓ Lock acquired successfully for " + context + ": " + projectName;
                logMessage(successMessage);
                return LockResult.success(project, successMessage);
            } else {
                String failureMessage = "⚠️ Failed to acquire lock for " + context + ": " + projectName;
                logMessage(failureMessage);
                return LockResult.failure(project, failureMessage);
            }
            
        } catch (Exception e) {
            String errorMessage = "❌ Error acquiring lock for " + context + ": " + projectName + " - " + e.getMessage();
            logMessage(errorMessage);
            return LockResult.error(project, errorMessage, e);
        }
    }
    
    /**
     * Acquires lock and updates UI state accordingly.
     * This is the most common pattern used throughout the application.
     * 
     * @param project the project to acquire lock for
     * @param context description of the operation context
     * @param onSuccess callback executed on successful lock acquisition (on UI thread)
     * @param onFailure callback executed on lock failure (on UI thread)
     */
    private void acquireProjectLockWithUI(JsonObject project, String context, 
                                         Runnable onSuccess, Runnable onFailure) {
        parentPlugin.executorService.execute(() -> {
            LockResult result = acquireProjectLock(project, context);
            
            Platform.runLater(() -> {
                if (result.isAcquired()) {
                    // Set as current project if lock acquired
                    currentProject = project;
                    
                    // Enable UI controls for editing
                    actionsTitlePane.setVisible(true);
                    actionsTitlePane.setExpanded(true);
                    actionsTitlePane.setText("Actions on `" + project.getString("name") + "`.");
                    uploadButton.setDisable(false);
                    unlockButton.setDisable(false);
                    
                    if (onSuccess != null) onSuccess.run();
                } else {
                    // Disable editing controls if lock not acquired
                    uploadButton.setDisable(true);
                    unlockButton.setDisable(true);
                    
                    if (onFailure != null) onFailure.run();
                }
            });
        });
    }
    
    /**
     * Simplified lock acquisition for cases where only the boolean result is needed.
     * Used primarily in tests and simple operations.
     * 
     * @param project the project to acquire lock for
     * @param context description of the operation context
     * @return true if lock was acquired, false otherwise
     */
    private boolean tryAcquireProjectLock(JsonObject project, String context) {
        return acquireProjectLock(project, context).isAcquired();
    }

    // ========================== PROJECT OPENING ========================== //
}
