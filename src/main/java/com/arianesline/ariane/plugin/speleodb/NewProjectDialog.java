package com.arianesline.ariane.plugin.speleodb;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
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
 */
public class NewProjectDialog extends Dialog<NewProjectDialog.ProjectData> {
    
    private TextField nameField;
    private TextArea descriptionField;
    private ComboBox<CountryItem> countryComboBox;
    private TextField latitudeField;
    private TextField longitudeField;
    
    @SuppressWarnings("this-escape")
    public NewProjectDialog() {
        // Create the form content first
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
        VBox content = new VBox(UIConstants.STANDARD_SPACING);
        content.setPadding(UIConstants.getStandardPadding());
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
        VBox section = new VBox(UIConstants.SMALL_SPACING);
        
        Label nameLabel = new Label("Project Name:");
        Label asterisk = new Label(" *");
        asterisk.setStyle(UIConstants.REQUIRED_FIELD_STYLE);
        
        HBox labelBox = new HBox(nameLabel, asterisk);
        nameLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, UIConstants.LABEL_FONT_SIZE));
        
        nameField = new TextField();
        nameField.setPromptText("Enter project name");
        nameField.setPrefWidth(UIConstants.STANDARD_FIELD_WIDTH);
        
        section.getChildren().addAll(labelBox, nameField);
        return section;
    }
    
    private VBox createDescriptionSection() {
        VBox section = new VBox(UIConstants.SMALL_SPACING);
        
        Label descLabel = new Label("Description:");
        Label asterisk = new Label(" *");
        asterisk.setStyle(UIConstants.REQUIRED_FIELD_STYLE);
        
        HBox labelBox = new HBox(descLabel, asterisk);
        descLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, UIConstants.LABEL_FONT_SIZE));
        
        descriptionField = new TextArea();
        descriptionField.setPromptText("Enter project description");
        descriptionField.setPrefRowCount(UIConstants.TEXT_AREA_ROWS);
        descriptionField.setPrefWidth(UIConstants.STANDARD_FIELD_WIDTH);
        descriptionField.setWrapText(true);
        
        section.getChildren().addAll(labelBox, descriptionField);
        return section;
    }
    
    private VBox createCountrySection() {
        VBox section = new VBox(UIConstants.SMALL_SPACING);
        
        Label countryLabel = new Label("Country:");
        Label asterisk = new Label(" *");
        asterisk.setStyle(UIConstants.REQUIRED_FIELD_STYLE);
        
        HBox labelBox = new HBox(countryLabel, asterisk);
        countryLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, UIConstants.LABEL_FONT_SIZE));
        
        countryComboBox = new ComboBox<>();
        countryComboBox.setPromptText("Select a country...");
        countryComboBox.setPrefWidth(UIConstants.STANDARD_FIELD_WIDTH);
        
        // Load countries from JSON file
        loadCountries();
        
        section.getChildren().addAll(labelBox, countryComboBox);
        return section;
    }
    
    private VBox createCoordinatesSection() {
        VBox section = new VBox(UIConstants.LARGE_SPACING);
        
        // Latitude
        VBox latSection = new VBox(UIConstants.SMALL_SPACING);
        Label latLabel = new Label("Latitude:");
        latLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, UIConstants.LABEL_FONT_SIZE));
        
        latitudeField = new TextField();
        latitudeField.setPromptText("[Optional]");
        latitudeField.setPrefWidth(UIConstants.STANDARD_FIELD_WIDTH);
        
        latSection.getChildren().addAll(latLabel, latitudeField);
        
        // Longitude
        VBox lonSection = new VBox(UIConstants.SMALL_SPACING);
        Label lonLabel = new Label("Longitude:");
        lonLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.MEDIUM, UIConstants.LABEL_FONT_SIZE));
        
        longitudeField = new TextField();
        longitudeField.setPromptText("[Optional]");
        longitudeField.setPrefWidth(UIConstants.STANDARD_FIELD_WIDTH);
        
        lonSection.getChildren().addAll(lonLabel, longitudeField);
        
        section.getChildren().addAll(latSection, lonSection);
        return section;
    }
    
    private void loadCountries() {
        try {
            // Load countries.json from resources
            InputStream inputStream = getClass().getResourceAsStream("countries.json");
            if (inputStream == null) {
                throw new RuntimeException("countries.json not found in resources at: " + 
                    getClass().getPackage().getName().replace('.', '/') + "/countries.json");
            }
            
            // Read and parse JSON
            String jsonContent = new String(inputStream.readAllBytes());
            JsonReader jsonReader = Json.createReader(new StringReader(jsonContent));
            JsonObject countriesObj = jsonReader.readObject();
            
            // Convert to sorted map for alphabetical ordering
            Map<String, String> countries = new TreeMap<>();
            for (Map.Entry<String, jakarta.json.JsonValue> entry : countriesObj.entrySet()) {
                countries.put(entry.getValue().toString().replace("\"", ""), entry.getKey());
            }
            
            // Add countries to combo box
            for (Map.Entry<String, String> entry : countries.entrySet()) {
                countryComboBox.getItems().add(new CountryItem(entry.getValue(), entry.getKey()));
            }
            
            System.out.println("Successfully loaded " + countries.size() + " countries from JSON");
            
        } catch (Exception e) {
            System.err.println("Error loading countries: " + e.getMessage());
            e.printStackTrace();
            // Add fallback countries
            countryComboBox.getItems().add(new CountryItem("US", "United States"));
            countryComboBox.getItems().add(new CountryItem("CA", "Canada"));
            countryComboBox.getItems().add(new CountryItem("GB", "United Kingdom"));
        }
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