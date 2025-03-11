package com.arianesline.ariane.plugin.speleodb;


import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import javafx.scene.web.WebView;


import static com.arianesline.ariane.plugin.speleodb.SpeleoDBAccessLevel.READ_ONLY;

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
     * Sets up default UI configurations.
     */
    private void setupUI() {
        actionsTitlePane.setVisible(false);
        aboutTitlePane.setExpanded(true);
        projectsTitlePane.setVisible(false);
        serverProgressIndicator.setVisible(false);
        connectionButton.setText("CONNECT");
        // On purpose - use the main URL, not the instance one.
        // TODO (low priority): Find a way to redirect to `http://localhost:<some_port>/webview/ariane` to
        //       allow development (maybe a sort of `if (source_distribution)` or `if (debug)`)


        aboutWebView.getEngine().load("https://www.speleodb.org/webview/ariane/");
        //TODO: WebView has been removed. Create directly in UI elements describing the about

        serverLog.textProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
                // This will scroll to the bottom - use Double.MIN_VALUE to scroll to the top
                serverLog.setScrollTop(Double.MAX_VALUE);
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
                    connectionButton.setText("DISCONNECT");
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
        connectionButton.setText("CONNECT");
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

        card.setPrefWidth(180);
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

    // -------------------------- Project Opening -------------------------- //

    private void clickSpeleoDBProject(ActionEvent e) throws URISyntaxException, IOException, InterruptedException {
        var project = (JsonObject) ((Button) e.getSource()).getUserData();

        parentPlugin.executorService.execute(() -> {

            AtomicBoolean lockIsAcquired = new AtomicBoolean(false);

            try {

                if (!project.getString("permission").equals(READ_ONLY.name())) {

                    // TODO: Should ask the user if they want to lock the project or not
                    //    - Create a UI confirmation window: "Are you modifying the project": Yes / No

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

                    Platform.runLater(() -> {

                        serverProgressIndicator.setVisible(false);
                        actionsTitlePane.setVisible(true);
                        actionsTitlePane.setExpanded(true);
                        actionsTitlePane.setText("Actions on " + currentProject.getString("name"));

                        if (lockIsAcquired.get()) {
                            uploadButton.setDisable(false);
                            unlockButton.setDisable(false);
                        } else {
                            uploadButton.setDisable(true);
                            unlockButton.setDisable(true);
                        }
                    });

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
            logMessage("Selected project: " + projectItem.getString("name"));
            clickSpeleoDBProject(event);

        } catch (Exception e) {
            logMessage("Error handling project action: " + e.getMessage());
            Platform.runLater(() -> projectListView.setDisable(false));
        }

    }

    // ---------------------- Project Mutex Management --------------------- //

    /**
     * Handles the unlock button click event for SpeleoDB.
     */
    @FXML
    public void onUnlockSpeleoDB(ActionEvent actionEvent) throws IOException, InterruptedException, URISyntaxException {

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
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    serverProgressIndicator.setVisible(false);
                });
            }
        });
    }

    // --------------------- Project Saving and Upload --------------------- //

    /**
     * Handles the upload button click event for SpeleoDB.
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
        //TODO: Implement method
    }
}
