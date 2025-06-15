package com.arianesline.ariane.plugin.speleodb;


import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private Button learnAboutButton;
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
    //TODO: Validate code removal
    //@FXML
    //private TitledPane connectionTitlePane;
    @FXML
    private TitledPane projectsTitlePane;

    @FXML
    private WebView aboutWebView;



    // Services for handling different concerns (package-private for testing)
    SpeleoDBService speleoDBService;
    final PreferencesService preferencesService = new PreferencesService();
    AuthenticationService authenticationService;

    // Internal Controller Data

    private JsonObject currentProject = null;
    
    /**
     * Implementation of AuthenticationCallback for handling authentication events.
     */
    private class AuthenticationCallbackImpl implements AuthenticationService.AuthenticationCallback {
        @Override
        public void onAuthenticationStarted() {
            Platform.runLater(() -> {
                logMessage("Starting authentication...");
                serverProgressIndicator.setVisible(true);
            });
        }
        
        @Override
        public void onAuthenticationSuccess() {
            Platform.runLater(() -> {
                showSuccessAnimation("Connected to SpeleoDB");
                serverProgressIndicator.setVisible(false);
                // Update UI state after successful authentication
                actionsTitlePane.setVisible(true);
                projectsTitlePane.setVisible(true);
                createNewProjectButton.setDisable(false);
                refreshProjectsButton.setDisable(false);
                connectionButton.setText("DISCONNECT");
                javafx.scene.layout.GridPane.setColumnSpan(connectionButton, 2);
                signupButton.setVisible(false);
            });
        }
        
        @Override
        public void onAuthenticationFailed(String message) {
            Platform.runLater(() -> {
                showErrorAnimation("Authentication Failed");
                serverProgressIndicator.setVisible(false);
            });
        }
        
        @Override
        public void onDisconnected() {
            Platform.runLater(() -> {
                showSuccessAnimation("Disconnected");
                // Update UI state after disconnection
                actionsTitlePane.setVisible(false);
                projectsTitlePane.setVisible(false);
                createNewProjectButton.setDisable(true);
                refreshProjectsButton.setDisable(true);
                connectionButton.setText("CONNECT");
                javafx.scene.layout.GridPane.setColumnSpan(connectionButton, 1);
                signupButton.setVisible(true);
            });
        }
        
        @Override
        public void logMessage(String message) {
            SpeleoDBController.this.logMessage(message);
        }
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
            successLabel.setStyle(UIConstants.SUCCESS_ANIMATION_STYLE);
            
            // Position it in the center-top of the main pane
            AnchorPane.setTopAnchor(successLabel, UIConstants.OVERLAY_TOP_ANCHOR);
            AnchorPane.setLeftAnchor(successLabel, UIConstants.OVERLAY_LEFT_ANCHOR);
            AnchorPane.setRightAnchor(successLabel, UIConstants.OVERLAY_RIGHT_ANCHOR);
            successLabel.setMaxWidth(Double.MAX_VALUE);
            successLabel.setAlignment(javafx.geometry.Pos.CENTER);
            
            speleoDBAnchorPane.getChildren().add(successLabel);
            
            // Animate the success message
            successLabel.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(UIConstants.FADE_IN_DURATION, successLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
            // Auto-hide after animation duration
            Timeline hideTimeline = new Timeline(
                new KeyFrame(UIConstants.ANIMATION_DURATION, e -> {
                    FadeTransition fadeOut = new FadeTransition(UIConstants.FADE_OUT_DURATION, successLabel);
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
            errorLabel.setStyle(UIConstants.ERROR_ANIMATION_STYLE);
            
            // Position it in the center-top of the main pane
            AnchorPane.setTopAnchor(errorLabel, UIConstants.OVERLAY_TOP_ANCHOR);
            AnchorPane.setLeftAnchor(errorLabel, UIConstants.OVERLAY_LEFT_ANCHOR);
            AnchorPane.setRightAnchor(errorLabel, UIConstants.OVERLAY_RIGHT_ANCHOR);
            errorLabel.setMaxWidth(Double.MAX_VALUE);
            errorLabel.setAlignment(javafx.geometry.Pos.CENTER);
            
            speleoDBAnchorPane.getChildren().add(errorLabel);
            
            // Animate the error message
            errorLabel.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(UIConstants.FADE_IN_DURATION, errorLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
            // Auto-hide after animation duration
            Timeline hideTimeline = new Timeline(
                new KeyFrame(UIConstants.ANIMATION_DURATION, e -> {
                    FadeTransition fadeOut = new FadeTransition(UIConstants.FADE_OUT_DURATION, errorLabel);
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

                ProjectId projectId = ProjectId.fromJson(project);
                String SDB_mainCaveFileId = parentPlugin.getSurvey().getExtraData();


                if (SDB_mainCaveFileId == null || SDB_mainCaveFileId.isEmpty()) {
                    logMessage("Adding SpeleoDB ID: " + projectId.getValue());
                    speleoDBService.updateFileSpeleoDBId(projectId.getValue());
                    parentPlugin.getSurvey().setExtraData(projectId.getValue());
                    return;
                }

                if (!SDB_mainCaveFileId.equals(projectId.getValue())) {
                    logMessage("Incoherent File ID detected.");
                    logMessage("\t- Previous Value: " + SDB_mainCaveFileId);
                    logMessage("\t- New Value: " + projectId.getValue());
                    parentPlugin.getSurvey().setExtraData(projectId.getValue());
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
        PreferencesService.UserPreferences prefs = preferencesService.loadPreferences();

        rememberCredentialsCheckBox.setSelected(prefs.isSaveCredentials());
        emailTextField.setText(prefs.getEmail());

        if (prefs.isSaveCredentials()) {
            passwordPasswordField.setText(prefs.getPassword());
            oauthtokenPasswordField.setText(prefs.getOAuthToken());
        }

        instanceTextField.setText(prefs.getInstance());
    }

    /**
     * Saves user preferences based on the current UI state.
     */
    private void savePreferences() {
        PreferencesService.UserPreferences prefs = new PreferencesService.UserPreferences(
            emailTextField.getText(),
            rememberCredentialsCheckBox.isSelected() ? passwordPasswordField.getText() : "",
            rememberCredentialsCheckBox.isSelected() ? oauthtokenPasswordField.getText() : "",
            instanceTextField.getText(),
            rememberCredentialsCheckBox.isSelected()
        );
        
        preferencesService.savePreferences(prefs);
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
        
        // For Accordion: ensure About section is the only visible and expanded pane at startup
        aboutTitlePane.setVisible(true);
        aboutTitlePane.setExpanded(true);
        
        // Use Platform.runLater to ensure accordion setup happens after FXML is fully loaded
        Platform.runLater(() -> {
            // Set About pane as the expanded pane in the Accordion
            setAccordionExpandedPane(aboutTitlePane);
        });
        
        createNewProjectButton.setDisable(true); // Disabled until authenticated
        refreshProjectsButton.setDisable(true); // Disabled until authenticated
        serverProgressIndicator.setVisible(false);
        connectionButton.setText("CONNECT");
        
        // Initial state: CONNECT and SIGNUP buttons visible 50/50
        javafx.scene.layout.GridPane.setColumnSpan(connectionButton, 1); // Span only 1 column (45%)
        signupButton.setVisible(true);
        
        // Setup AboutWebView with proper URL and error handling
        setupAboutWebView();

        serverLog.textProperty().addListener((ObservableValue<?> observable, Object oldValue, Object newValue) -> {
            // This will scroll to the bottom - use Double.MIN_VALUE to scroll to the top
            serverLog.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    /**
     * Sets up the AboutWebView with proper URL loading and error handling.
     */
    private void setupAboutWebView() {
        // Use localhost URL when in debug mode, production URL otherwise
        String aboutUrl;
        boolean isDebugMode = isDebugMode();
        
        if (isDebugMode) {
            aboutUrl = "http://localhost:8000/webview/ariane/";
        } else {
            // On purpose - use the main URL, not the instance one.
            aboutUrl = "https://www.speleoDB.org/webview/ariane/";
        }

        // Load the URL with error handling
        try {
            aboutWebView.getEngine().load(aboutUrl);
            logMessage("Loading about page from: " + aboutUrl);
            
            // Add a listener to handle loading states
            aboutWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                switch (newState) {
                    case SUCCEEDED:
                        logMessage("About page loaded successfully");
                        break;
                    case FAILED:
                        logMessage("Failed to load about page from " + aboutUrl);
                        break;
                    case CANCELLED:
                        logMessage("About page loading was cancelled");
                        break;
                }
            });
            
        } catch (Exception e) {
            logMessage("Error loading about page: " + e.getMessage());
        }
        
        //TODO: WebView has been removed. Create directly in UI elements describing the about
    }

    /**
     * Helper method to set the expanded pane in the Accordion.
     * Ensures only one pane is expanded at a time, as required by Accordion behavior.
     * 
     * @param paneToExpand the TitledPane to expand
     */
    private void setAccordionExpandedPane(javafx.scene.control.TitledPane paneToExpand) {
        if (paneToExpand != null && paneToExpand.getParent() instanceof javafx.scene.control.Accordion) {
            javafx.scene.control.Accordion accordion = (javafx.scene.control.Accordion) paneToExpand.getParent();
            accordion.setExpandedPane(paneToExpand);
            logMessage("Expanded accordion pane: " + paneToExpand.getText());
        }
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
     * Sets up listeners for UI components to automatically save preferences when they change.
     * This ensures preferences are persisted immediately when users modify settings.
     */
    private void setupPreferenceListeners() {
        // Save preferences when remember credentials checkbox changes
        rememberCredentialsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            savePreferences();
        });
        
        // Save preferences when email field changes (on focus lost or enter key)
        emailTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                savePreferences();
            }
        });
        
        // Save preferences when instance field changes (on focus lost or enter key)
        instanceTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                savePreferences();
            }
        });
        
        // Save preferences when password field changes (on focus lost)
        passwordPasswordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                savePreferences();
            }
        });
        
        // Save preferences when oauth token field changes (on focus lost)
        oauthtokenPasswordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                savePreferences();
            }
        });
    }
    
    /**
     * Shows a save modal dialog when Ctrl+S / Cmd+S is pressed.
     * This method assumes an active project with lock exists (gated by keyboard shortcut handler).
     */
    private void showSaveModal() {
        // Create custom dialog with text field
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Save Project on SpeleoDB");
        dialog.setHeaderText("Save Current Project: \"" + currentProject.getString("name") + "\"");
        
        // Set button types
        ButtonType saveButtonType = new ButtonType("Save Project", ButtonType.OK.getButtonData());
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        
        // Create the content
        GridPane grid = new GridPane();
        grid.setHgap(UIConstants.GRID_HGAP);
        grid.setVgap(UIConstants.GRID_VGAP);
        grid.setPadding(UIConstants.getGridPadding());
        
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
        dialog.getDialogPane().setMinWidth(UIConstants.DIALOG_MIN_WIDTH);
        dialog.getDialogPane().setPrefWidth(UIConstants.DIALOG_PREF_WIDTH);
        
        // Focus on the text field when dialog is shown
        dialog.setOnShown(e -> messageField.requestFocus());
        
        // Convert the result when save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return messageField.getText();
            }
            return null;
        });
        
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
     * Initializes the controller, setting up default UI states and preferences.
     *
     * @param location  the location of the FXML file.
     * @param resources additional resources, such as localized strings.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize SpeleoDBService first to avoid this-escape warning
        if (speleoDBService == null) {
            speleoDBService = new SpeleoDBService(this);
        }
        
        // Initialize AuthenticationService now that we have all dependencies
        if (authenticationService == null) {
            authenticationService = new AuthenticationService(
                speleoDBService, 
                parentPlugin != null ? parentPlugin.executorService : null, 
                new AuthenticationCallbackImpl()
            );
        }
        
        loadPreferences();
        setupUI();
        setupKeyboardShortcuts();
        setupPreferenceListeners();
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
                    
                    // Set Projects pane as the expanded pane in the Accordion
                    setAccordionExpandedPane(projectsTitlePane);
                    
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
        
        // Return to About pane when disconnected
        setAccordionExpandedPane(aboutTitlePane);
        
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
        Platform.runLater(() -> {
            projectListView.getItems().clear();
            for (JsonValue jsonValue : projectList) {
                JsonObject projectItem = jsonValue.asJsonObject();
                VBox card = createProjectCard(projectItem);
                Button button = createProjectButton(card, projectItem);
                projectListView.getItems().add(button);
            }
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
                    // Create the project via API using ProjectCreationRequest
                    ProjectCreationRequest request = ProjectCreationRequest.builder()
                        .withName(projectData.name)
                        .withDescription(projectData.description)
                        .withCountry(projectData.countryCode)
                        .withCoordinates(projectData.latitude, projectData.longitude)
                        .build();
                    
                    JsonObject createdProject = speleoDBService.createProject(request);
                    
                    Platform.runLater(() -> {
                        logMessage("Project '" + projectData.name + "' created successfully!");
                        logMessage("Project ID: " + ProjectId.fromJson(createdProject).getValue());
                        
                        // Show success animation
                        showSuccessAnimation();
                        
                        // Refresh the project list to show the new project
                        listProjects();
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

    // -------------------------- Project Opening -------------------------- //

    /**
     * Handles clicking on a SpeleoDB project button.
     * Orchestrates the project opening process in the background.
     */
    private void clickSpeleoDBProject(ActionEvent e) throws URISyntaxException, IOException, InterruptedException {
        var project = (JsonObject) ((Button) e.getSource()).getUserData();
        
        parentPlugin.executorService.execute(() -> {
            try {
                AtomicBoolean lockIsAcquired = new AtomicBoolean(false);
                
                // Step 1: Acquire project lock if needed
                if (shouldAcquireProjectLock(project)) {
                    lockIsAcquired.set(acquireProjectLock(project));
                }
                
                // Step 2: Download and load project
                if (downloadAndLoadProject(project)) {
                    // Step 3: Update UI state
                    updateUIAfterProjectLoad(project, lockIsAcquired.get());
                    
                    // Step 4: Refresh project listing
                    refreshProjectListingAfterLoad();
                } else {
                    // Step 5: Handle download failure
                    handleProjectLoadFailure();
                }
                
            } catch (Exception ex) {
                logMessage("Error opening project: " + ex.getMessage());
                handleProjectLoadFailure();
            } finally {
                Platform.runLater(() -> {
                    projectListView.setDisable(false);
                    serverProgressIndicator.setVisible(false);
                });
            }
        });
    }
    
    /**
     * Determines if a project lock should be acquired based on project permissions.
     */
    private boolean shouldAcquireProjectLock(JsonObject project) {
        return !project.getString("permission").equals(READ_ONLY.name());
    }
    
    /**
     * Attempts to acquire a lock on the specified project.
     * 
     * @param project the project to lock
     * @return true if lock was acquired, false otherwise
     */
    private boolean acquireProjectLock(JsonObject project) {
        try {
            String projectName = project.getString("name");
            logMessage("Locking " + projectName);
            
            if (speleoDBService.acquireOrRefreshProjectMutex(project)) {
                logMessage("Lock successful on " + projectName);
                return true;
            } else {
                logMessage("Lock failed on " + projectName);
                return false;
            }
        } catch (Exception e) {
            logMessage("Error acquiring lock: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Downloads and loads the specified project.
     * 
     * @param project the project to download and load
     * @return true if successful, false otherwise
     */
    private boolean downloadAndLoadProject(JsonObject project) {
        try {
            String projectName = project.getString("name");
            logMessage("Downloading project " + projectName);
            
            Path tml_filepath = speleoDBService.downloadProject(project);
            
            if (Files.exists(tml_filepath)) {
                parentPlugin.loadSurvey(tml_filepath.toFile());
                logMessage("Download successful of " + projectName);
                
                currentProject = project;
                checkAndUpdateSpeleoDBId(project);
                return true;
            } else {
                logMessage("Download failed - file does not exist");
                return false;
            }
            
        } catch (Exception e) {
            logMessage("Error downloading project: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Updates the UI state after successful project loading.
     */
    private void updateUIAfterProjectLoad(JsonObject project, boolean hasLock) {
        Platform.runLater(() -> {
            serverProgressIndicator.setVisible(false);
            actionsTitlePane.setVisible(true);
            actionsTitlePane.setExpanded(true);
            actionsTitlePane.setText("Actions on `" + project.getString("name") + "`.");
            
            if (hasLock) {
                uploadButton.setDisable(false);
                unlockButton.setDisable(false);
            } else {
                uploadButton.setDisable(true);
                unlockButton.setDisable(true);
            }
        });
    }
    
    /**
     * Refreshes the project listing after successful project load.
     */
    private void refreshProjectListingAfterLoad() {
        try {
            logMessage("Refreshing project listing after project opening...");
            JsonArray projectList = speleoDBService.listProjects();
            handleProjectListResponse(projectList);
        } catch (Exception refreshEx) {
            logMessage("Error refreshing project listing: " + refreshEx.getMessage());
        }
    }
    
    /**
     * Handles UI updates when project loading fails.
     */
    private void handleProjectLoadFailure() {
        logMessage("Project loading failed");
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

    /**
     * Handles the action performed when a project button is clicked.
     * Orchestrates project selection and potential project switching.
     */
    private void handleProjectCardClickAction(ActionEvent event, JsonObject projectItem) {
        // Prevent double click
        projectListView.setDisable(true);

        try {
            String selectedProjectName = projectItem.getString("name");
            ProjectId selectedProjectId = ProjectId.fromJson(projectItem); // Use ProjectId value object
            
            // Handle project selection based on current lock state
            if (hasActiveProjectLock()) {
                handleProjectSelectionWithActiveLock(event, projectItem, selectedProjectName, selectedProjectId);
            } else {
                handleProjectSelectionWithoutLock(event, selectedProjectName);
            }

        } catch (IOException | InterruptedException | URISyntaxException e) {
            logMessage("Error handling project action: " + e.getMessage());
            Platform.runLater(() -> projectListView.setDisable(false));
        }
    }
    
    /**
     * Handles project selection when there's an active project lock.
     * Updated to use ProjectId value object for type safety.
     */
    private void handleProjectSelectionWithActiveLock(ActionEvent event, JsonObject projectItem, 
                                                      String selectedProjectName, ProjectId selectedProjectId) 
            throws IOException, InterruptedException, URISyntaxException {
        
        ProjectId currentProjectId = ProjectId.fromJson(currentProject); // Use ProjectId value object
        
        // If clicking on the same project, proceed normally
        if (currentProjectId.equals(selectedProjectId)) {
            logMessage("Selected project: " + selectedProjectName);
            clickSpeleoDBProject(event);
            return;
        }
        
        // Different project selected - handle project switching
        handleProjectSwitching(event, selectedProjectName);
    }
    
    /**
     * Handles project selection when there's no active lock.
     */
    private void handleProjectSelectionWithoutLock(ActionEvent event, String selectedProjectName) 
            throws IOException, InterruptedException, URISyntaxException {
        
        logMessage("Selected project: " + selectedProjectName);
        clickSpeleoDBProject(event);
    }
    
    /**
     * Handles the process of switching from one locked project to another.
     */
    private void handleProjectSwitching(ActionEvent event, String selectedProjectName) {
        logMessage("Attempting to switch from locked project: " + getCurrentProjectName() + 
                  " to: " + selectedProjectName);
        
        boolean shouldSwitch = showProjectSwitchConfirmation(selectedProjectName);
        
        if (!shouldSwitch) {
            logMessage("User cancelled project switch. Staying on: " + getCurrentProjectName());
            Platform.runLater(() -> projectListView.setDisable(false));
            return;
        }
        
        // User confirmed switch - execute the switch process
        executeProjectSwitch(event, selectedProjectName);
    }
    
    /**
     * Executes the actual project switch by releasing current lock and opening new project.
     */
    private void executeProjectSwitch(ActionEvent event, String selectedProjectName) {
        logMessage("User confirmed project switch. Releasing lock on: " + getCurrentProjectName());
        
        parentPlugin.executorService.execute(() -> {
            try {
                if (releaseCurrentProjectLockInternal()) {
                    logMessage("Successfully released lock on: " + getCurrentProjectName());
                    currentProject = null;
                    
                    Platform.runLater(() -> {
                        resetUIAfterLockRelease();
                        proceedWithNewProjectSelection(event, selectedProjectName);
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
    }
    
    /**
     * Releases the current project lock (internal implementation).
     * 
     * @return true if lock was released successfully, false otherwise
     */
    private boolean releaseCurrentProjectLockInternal() 
            throws IOException, InterruptedException, URISyntaxException {
        
        return speleoDBService.releaseProjectMutex(currentProject);
    }
    
    /**
     * Resets UI elements after releasing a project lock.
     */
    private void resetUIAfterLockRelease() {
        actionsTitlePane.setVisible(false);
        actionsTitlePane.setExpanded(false);
    }
    
    /**
     * Proceeds with opening the newly selected project after releasing previous lock.
     */
    private void proceedWithNewProjectSelection(ActionEvent event, String selectedProjectName) {
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
    }

    // ---------------------- Project Mutex Management --------------------- //

    /**
     * Shows a confirmation dialog asking the user if they want to release the current project lock
     * before switching to a different project.
     * 
     * @param newProjectName the name of the project the user wants to switch to
     * @return true if the user wants to release the lock and switch projects, false otherwise
     */
    private boolean showProjectSwitchConfirmation(String newProjectName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Switch Project");
        alert.setHeaderText("Current Project is Locked");
        alert.setContentText(StringFormatter.formatProjectSwitchMessage(
            currentProject.getString("name"), newProjectName));
        
        // Customize button text
        ButtonType yesButton = new ButtonType("Yes, Switch Projects");
        ButtonType noButton = new ButtonType("No, Stay Here");
        alert.getButtonTypes().setAll(yesButton, noButton);
        
        // Show dialog and wait for user response
        return alert.showAndWait()
                   .map(response -> response == yesButton)
                   .orElse(false);
    }

    /**
     * Shows a confirmation dialog asking the user if they want to unlock the project.
     * 
     * @return true if the user wants to unlock, false otherwise
     */
    private boolean showUnlockConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unlock Project");
        alert.setHeaderText("Confirm Unlock");
        alert.setContentText(StringFormatter.formatUnlockMessage(
            currentProject.getString("name")));
        
        // Customize button text
        ButtonType yesButton = new ButtonType("Yes, Unlock");
        ButtonType noButton = new ButtonType("No, Keep Lock");
        alert.getButtonTypes().setAll(yesButton, noButton);
        
        // Show dialog and wait for user response
        return alert.showAndWait()
                   .map(response -> response == yesButton)
                   .orElse(false);
    }
    
    /**
     * Shows a confirmation dialog asking the user if they want to release the write lock.
     * 
     * @return true if the user wants to release the lock, false otherwise
     */
    private boolean showReleaseLockConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Release Write Lock");
        alert.setHeaderText("Upload Successful");
        alert.setContentText(StringFormatter.formatReleaseLockMessage(
            currentProject.getString("name")));
        
        // Customize button text
        ButtonType yesButton = new ButtonType("Yes, Release Lock");
        ButtonType noButton = new ButtonType("No, Keep Lock");
        alert.getButtonTypes().setAll(yesButton, noButton);
        
        // Show dialog and wait for user response
        return alert.showAndWait()
                   .map(response -> response == yesButton)
                   .orElse(false);
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
        // Save the survey using the plugin's save mechanism
        // This delegates to the parent plugin which handles the actual save operation
        parentPlugin.saveSurvey();

        // Alternative approach (commented out): 
        // Direct access to core functionality would require CoreContext dependency
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
                instance = PreferencesService.getDefaultInstance();
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
     * Handles the "Learn about SpeleoDB" button click event.
     * Opens the SpeleoDB website in the user's default browser.
     */
    @FXML
    public void onLearnAbout(ActionEvent actionEvent) {
        try {
            String speleoBDUrl = "https://www.speleodb.org";
            
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
        } catch (Exception e) {
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
}
