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
import java.util.prefs.Preferences;

import static com.arianesline.ariane.plugin.speleodb.SpeleoDBAccessLevel.READ_ONLY;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
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
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
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

    //TODO: Find alternative   private final CoreContext core = CoreContext.getInstance();
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
    private Button refreshProjectsButton;
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



    // SpeleoDBService instance for handling server communication.
    private final SpeleoDBService speleoDBService = new SpeleoDBService(this);

    // Constants for Preferences keys and default values.
    private static final String PREF_EMAIL = "SDB_EMAIL";
    private static final String PREF_PASSWORD = "SDB_PASSWORD";
    private static final String PREF_OAUTH_TOKEN = "SDB_OAUTH_TOKEN";
    private static final String PREF_INSTANCE = "SDB_INSTANCE";
    private static final String PREF_SAVE_CREDS = "SDB_SAVECREDS";
    private static final String DEFAULT_INSTANCE = "www.speleoDB.org";

    // Internal Controller Data

    private JsonObject currentProject = null;

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
        aboutTitlePane.setExpanded(true);
        projectsTitlePane.setVisible(false);
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
        //TODO: WebView has been removed. Create directly in UI elements describing the about

        serverLog.textProperty().addListener((ObservableValue<?> observable, Object oldValue, Object newValue) -> {
            // This will scroll to the bottom - use Double.MIN_VALUE to scroll to the top
            serverLog.setScrollTop(Double.MAX_VALUE);
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
        loadPreferences();
        setupUI();
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
                    refreshProjectsButton.setDisable(false); // Enable refresh button
                    connectionButton.setText("DISCONNECT");
                    
                    // When authenticated: DISCONNECT button takes 100% width, SIGNUP button hidden
                    javafx.scene.layout.GridPane.setColumnSpan(connectionButton, 3); // Span all 3 columns
                    signupButton.setVisible(false);
                });

                listProjects();

            } catch (Exception e) {
                logMessage("Connection failed: " + e.getMessage());
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
            } finally {
                Platform.runLater(() -> {
                    serverProgressIndicator.setVisible(false);
                    refreshProjectsButton.setDisable(false);
                    refreshProjectsButton.setText("Refresh Projects");
                });
            }
        });
    }

    // -------------------------- Project Opening -------------------------- //

    private void clickSpeleoDBProject(ActionEvent e) throws URISyntaxException, IOException, InterruptedException {
        var project = (JsonObject) ((Button) e.getSource()).getUserData();

        parentPlugin.executorService.execute(() -> {

            AtomicBoolean lockIsAcquired = new AtomicBoolean(false);

            try {

                if (!project.getString("permission").equals(READ_ONLY.name())) {
                    logMessage("Locking " + project.getString("name"));

                    if (speleoDBService.acquireOrRefreshProjectMutex(project)) {
                        logMessage("Lock successful on  " + project.getString("name"));
                        lockIsAcquired.set(true);
                    } else {
                        logMessage("Lock failed on  " + project.getString("name"));
                    }
                }

                logMessage("Downloading projects " + project.getString("name"));

                Path tml_filepath = speleoDBService.downloadProject(project);

                if (Files.exists(tml_filepath)) {

                    parentPlugin.loadSurvey(tml_filepath.toFile());
                    logMessage("Download successful of " + project.getString("name"));
                    currentProject = project;
                    checkAndUpdateSpeleoDBId(project);

                    // Update UI first
                    Platform.runLater(() -> {
                        serverProgressIndicator.setVisible(false);
                        actionsTitlePane.setVisible(true);
                        actionsTitlePane.setExpanded(true);
                        actionsTitlePane.setText("Actions on `" + currentProject.getString("name") + "`.");

                        if (lockIsAcquired.get()) {
                            uploadButton.setDisable(false);
                            unlockButton.setDisable(false);
                        } else {
                            uploadButton.setDisable(true);
                            unlockButton.setDisable(true);
                        }
                    });
                    
                    // Refresh project listing after all operations are complete
                    // (no sleep needed - all operations above are now complete)
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
                throw new RuntimeException(ex);
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
     * 
     * @param newProjectName the name of the project the user wants to switch to
     * @return true if the user wants to release the lock and switch projects, false otherwise
     */
    private boolean showProjectSwitchConfirmation(String newProjectName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Switch Project");
        alert.setHeaderText("Current Project is Locked");
        alert.setContentText("You currently have an active lock on project \"" + 
                            currentProject.getString("name") + "\".\n\n" +
                            "To switch to project \"" + newProjectName + "\", you need to release your current lock.\n\n" +
                            "Do you want to release the lock and switch projects?\n\n" +
                            "• Yes: Release lock and switch to \"" + newProjectName + "\"\n" +
                            "• No: Keep current lock and stay on \"" + currentProject.getString("name") + "\"");
        
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
        alert.setContentText("Are you sure you want to unlock project \"" + 
                            currentProject.getString("name") + "\"?\n\n" +
                            "This will release your write lock and allow other users to edit the project.\n\n" +
                            "• Yes: Unlock project (other users can edit)\n" +
                            "• No: Keep lock (continue editing)");
        
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
        alert.setContentText("Do you want to release the write lock for project \"" + 
                            currentProject.getString("name") + "\"?\n\n" +
                            "• Yes: Release lock (other users can edit)\n" +
                            "• No: Keep lock (continue editing)");
        
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
                    currentProject = null;

                    Platform.runLater(() -> {
                        actionsTitlePane.setVisible(false);
                        actionsTitlePane.setExpanded(false);
                        projectsTitlePane.setExpanded(true);
                    });

                    listProjects();
                } else {
                    logMessage("Failed to release lock for " + currentProject.getString("name"));
                }
            } catch (IOException | InterruptedException | URISyntaxException e) {
                logMessage("Error releasing lock: " + e.getMessage());
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
     * Handles the "Save Project" button click event for SpeleoDB.
     */
    @FXML
    public void onUploadSpeleoDB(ActionEvent actionEvent) throws IOException, URISyntaxException, InterruptedException {

        //TODO: FInd alternative
        parentPlugin.saveSurvey();

      /*
        if (UndoRedo.maxActionNumber > 0) {
            core.mainController.saveTML(false);
        } else {
            logMessage("No changes to the project detected");
            return;
        }

       */

        String message = uploadMessageTextField.getText();
        if (message.isEmpty()) {
            logMessage("Upload message cannot be empty.");
            return;
        }

        logMessage("Uploading project " + currentProject.getString("name") + " ...");
        
        serverProgressIndicator.setVisible(true);
        uploadButton.setDisable(true);
        unlockButton.setDisable(true);

        parentPlugin.executorService.execute(() -> {
            try {
                speleoDBService.uploadProject(message, currentProject);
                logMessage("Upload successful.");
                
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
                                }
                            } catch (IOException | InterruptedException | URISyntaxException e) {
                                logMessage("Error releasing lock: " + e.getMessage());
                            }
                        });
                    } else {
                        logMessage("User chose to keep the write lock.");
                    }
                });
                
            } catch (Exception e) {
                logMessage("Upload failed: " + e.getMessage());
            } finally {
                Platform.runLater(() -> {
                    serverProgressIndicator.setVisible(false);
                    uploadButton.setDisable(false);
                });
            }
        });

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
