package com.arianesline.ariane.plugin.speleodb;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Dialog for creating a new SpeleoDB project.
 * Provides a form with project name, description, country, latitude, and longitude fields.
 * OPTIMIZED: Pre-loads countries data and caches UI components for faster display.
 */
public class NewProjectDialog extends Dialog<NewProjectDialog.ProjectData> {
    
    // Static cache for countries data - loaded once and reused
    private static Map<String, String> cachedCountries = null;
    private static boolean countriesLoadAttempted = false;
    
    private TextField nameField;
    private TextArea descriptionField;
    private ComboBox<CountryItem> countryComboBox;
    private TextField latitudeField;
    private TextField longitudeField;
    
    /**
     * Pre-loads countries data in a background thread to optimize dialog creation.
     * This method can be called during application startup to warm the cache.
     */
    public static void preLoadCountriesData() {
        if (!countriesLoadAttempted) {
            countriesLoadAttempted = true;
            
            // Load in background to avoid blocking UI
            Thread countriesThread = new Thread(() -> {
                try {
                    loadCountriesFromResource();
                } catch (Exception e) {
                    System.err.println("Error pre-loading countries data: " + e.getMessage());
                }
            }, "CountriesLoader");
            countriesThread.setDaemon(true); // Mark as daemon to allow JVM shutdown
            countriesThread.start();
        }
    }
    
    /**
     * Loads countries data from JSON resource file.
     * Results are cached in static field for reuse across dialog instances.
     */
    private static synchronized void loadCountriesFromResource() {
        if (cachedCountries != null) {
            return; // Already loaded
        }
        
        try {
            // Load countries.json from resources
            InputStream inputStream = NewProjectDialog.class.getResourceAsStream("countries.json");
            if (inputStream == null) {
                throw new RuntimeException("countries.json not found in resources at: " + 
                    NewProjectDialog.class.getPackage().getName().replace('.', '/') + "/countries.json");
            }
            
            // Read and parse JSON
            String jsonContent = new String(inputStream.readAllBytes());
            JsonObject countriesObj;
            try (JsonReader jsonReader = Json.createReader(new StringReader(jsonContent))) {
                countriesObj = jsonReader.readObject();
            }
            
            // Convert to sorted map for alphabetical ordering
            cachedCountries = new TreeMap<>();
            for (Map.Entry<String, jakarta.json.JsonValue> entry : countriesObj.entrySet()) {
                cachedCountries.put(entry.getValue().toString().replace("\"", ""), entry.getKey());
            }
            
            System.out.println("Successfully cached " + cachedCountries.size() + " countries from JSON");
            
        } catch (IOException | RuntimeException e) {
            System.err.println("Error loading countries: " + e.getMessage());
            e.printStackTrace();
            // Create empty map as fallback
            cachedCountries = new TreeMap<>();
        }
    }
    
    @SuppressWarnings("this-escape")
    public NewProjectDialog() {
        // Ensure countries are loaded (will use cache if already loaded)
        if (cachedCountries == null && !countriesLoadAttempted) {
            loadCountriesFromResource();
        }
        
        // Create the form content
        VBox content = createFormContent();
        
        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        
        // Enable/disable save button based on validation
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        
        // Add validation listeners
        setupValidation(saveButton);
        
        // Convert result when save is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new ProjectData(
                    nameField.getText().trim(),
                    descriptionField.getText().trim(),
                    countryComboBox.getValue() != null ? countryComboBox.getValue().code : null,
                    latitudeField.getText().trim().isEmpty() ? null : latitudeField.getText().trim(),
                    longitudeField.getText().trim().isEmpty() ? null : longitudeField.getText().trim()
                );
            }
            return null;
        });
        
        // Set dialog properties
        setTitle("Create New Project");
        setHeaderText("Enter project details");
        getDialogPane().setContent(content);
        
        // Focus on name field when dialog opens
        setOnShown(e -> nameField.requestFocus());
    }
    
    private VBox createFormContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);
        
        // Project Name section
        content.getChildren().add(createProjectNameSection());
        
        // Description section
        content.getChildren().add(createDescriptionSection());
        
        // Country section
        content.getChildren().add(createCountrySection());
        
        // Coordinates section
        content.getChildren().add(createCoordinatesSection());
        
        return content;
    }
    
    private VBox createProjectNameSection() {
        VBox section = new VBox(5);
        
        Label nameLabel = new Label("Project Name:");
        Label asterisk = new Label(" *");
        asterisk.setStyle("-fx-text-fill: #dc2626;"); // Red color for required asterisk
        
        HBox labelBox = new HBox(nameLabel, asterisk);
        nameLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, 12));
        
        nameField = new TextField();
        nameField.setPromptText("Enter project name");
        nameField.setPrefWidth(350);
        
        section.getChildren().addAll(labelBox, nameField);
        return section;
    }
    
    private VBox createDescriptionSection() {
        VBox section = new VBox(5);
        
        Label descLabel = new Label("Description:");
        Label asterisk = new Label(" *");
        asterisk.setStyle("-fx-text-fill: #dc2626;");
        
        HBox labelBox = new HBox(descLabel, asterisk);
        descLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, 12));
        
        descriptionField = new TextArea();
        descriptionField.setPromptText("Enter project description");
        descriptionField.setPrefRowCount(4);
        descriptionField.setPrefWidth(350);
        descriptionField.setWrapText(true);
        
        section.getChildren().addAll(labelBox, descriptionField);
        return section;
    }
    
    private VBox createCountrySection() {
        VBox section = new VBox(5);
        
        Label countryLabel = new Label("Country:");
        Label asterisk = new Label(" *");
        asterisk.setStyle("-fx-text-fill: #dc2626;");
        
        HBox labelBox = new HBox(countryLabel, asterisk);
        countryLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, 12));
        
        countryComboBox = new ComboBox<>();
        countryComboBox.setPromptText("Select a country ...");
        countryComboBox.setPrefWidth(350);
        
        // Load countries from JSON file
        loadCountries();
        
        section.getChildren().addAll(labelBox, countryComboBox);
        return section;
    }
    
    private VBox createCoordinatesSection() {
        VBox section = new VBox(10);
        
        // Latitude
        VBox latSection = new VBox(5);
        Label latLabel = new Label("Latitude:");
        latLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, 12));
        
        latitudeField = new TextField();
        latitudeField.setPromptText("[Optional]");
        latitudeField.setPrefWidth(350);
        
        latSection.getChildren().addAll(latLabel, latitudeField);
        
        // Longitude
        VBox lonSection = new VBox(5);
        Label lonLabel = new Label("Longitude:");
        lonLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, 12));
        
        longitudeField = new TextField();
        longitudeField.setPromptText("[Optional]");
        longitudeField.setPrefWidth(350);
        
        lonSection.getChildren().addAll(lonLabel, longitudeField);
        
        section.getChildren().addAll(latSection, lonSection);
        return section;
    }
    
    private void loadCountries() {
        // Use cached countries data if available, otherwise load synchronously
        Map<String, String> countries = cachedCountries;
        
        if (countries == null) {
            // Fallback: load synchronously if cache is not available
            loadCountriesFromResource();
            countries = cachedCountries != null ? cachedCountries : new TreeMap<>();
        }
        
        // Add countries to combo box from cache
        for (Map.Entry<String, String> entry : countries.entrySet()) {
            countryComboBox.getItems().add(new CountryItem(entry.getValue(), entry.getKey()));
        }
        
        System.out.println("Loaded " + countries.size() + " countries into combo box" + 
                          (cachedCountries != null ? " (from cache)" : " (direct load)"));
    }
    
    private void setupValidation(Button saveButton) {
        // Create a validation function
        Runnable validateForm = () -> {
            boolean nameValid = !nameField.getText().trim().isEmpty();
            boolean descValid = !descriptionField.getText().trim().isEmpty();
            boolean countryValid = countryComboBox.getValue() != null;
            
            saveButton.setDisable(!(nameValid && descValid && countryValid));
        };
        
        // Add listeners to required fields
        nameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
        countryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
    }
    
    /**
     * Data class to hold the form results
     */
    public static class ProjectData {
        public final String name;
        public final String description;
        public final String countryCode;
        public final String latitude;
        public final String longitude;
        
        public ProjectData(String name, String description, String countryCode, String latitude, String longitude) {
            this.name = name;
            this.description = description;
            this.countryCode = countryCode;
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        @Override
        public String toString() {
            return "ProjectData{" +
                   "name='" + name + '\'' +
                   ", description='" + description + '\'' +
                   ", countryCode='" + countryCode + '\'' +
                   ", latitude='" + latitude + '\'' +
                   ", longitude='" + longitude + '\'' +
                   '}';
        }
    }
    
    /**
     * Helper class for country combo box items
     */
    private static class CountryItem {
        public final String code;
        public final String name;
        
        public CountryItem(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
} 