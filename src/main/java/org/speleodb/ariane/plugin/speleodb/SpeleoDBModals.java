package org.speleodb.ariane.plugin.speleodb;

import java.util.Optional;
import java.util.function.Consumer;

import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.ANIMATIONS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.DIALOGS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.DIMENSIONS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.MESSAGES;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.STYLES;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.STYLES.MATERIAL_COLORS;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Centralized modal management for SpeleoDB plugin with Material Design styling.
 * Provides consistent modal dialogs throughout the application.
 */
public class SpeleoDBModals {
    
    // Pre-warmed modal instances for performance
    private static Alert preWarmedConfirmationModal;
    private static Dialog<String> preWarmedInputModal;
    
    private static final SpeleoDBLogger logger = SpeleoDBLogger.getInstance();
    
    /**
     * Pre-warms modal system for instant display performance.
     * Should be called during application initialization.
     */
    public static void preWarmModalSystem() {
        Platform.runLater(() -> {
            try {
                // Pre-warm confirmation modal
                preWarmedConfirmationModal = new Alert(Alert.AlertType.CONFIRMATION);
                preWarmedConfirmationModal.setTitle("");
                preWarmedConfirmationModal.setHeaderText("");
                preWarmedConfirmationModal.setContentText("");
                
                // Pre-warm input modal
                preWarmedInputModal = new Dialog<>();
                preWarmedInputModal.setTitle("");
                preWarmedInputModal.setHeaderText("");
                
                logger.debug("Modal system pre-warmed successfully");
            } catch (Exception e) {
                logger.error("Failed to pre-warm modal system", e);
            }
        });
    }
    
    /**
     * Shows a Material Design styled confirmation dialog.
     * 
     * @param title the dialog title
     * @param message the confirmation message
     * @param positiveText text for the positive button (e.g., "Yes", "OK")
     * @param negativeText text for the negative button (e.g., "No", "Cancel")
     * @return true if positive button clicked, false otherwise
     */
    public static boolean showConfirmation(String title, String message, String positiveText, String negativeText) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Must be called on JavaFX Application Thread");
        }
        
        // Use pre-warmed modal for speed, fallback to new if needed
        Alert alert = (preWarmedConfirmationModal != null) ? preWarmedConfirmationModal : new Alert(Alert.AlertType.CONFIRMATION);
        
        // Configure the modal
        alert.setTitle(title);
        alert.setHeaderText(null); // Clean Material Design look
        alert.setContentText(null); // We'll use custom content
        
        // Create custom content with Material Design styling
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("material-dialog-content-simple");
        
        // Message label with Material Design typography
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("material-dialog-message");
        messageLabel.setWrapText(true);
        messageLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        messageLabel.setMaxWidth(450); // Maximum width to prevent overly wide confirmation dialogs
        messageLabel.setMinHeight(Region.USE_PREF_SIZE); // Prevent text truncation
        
        content.getChildren().add(messageLabel);
        
        // Set the custom content
        alert.getDialogPane().setContent(content);
        
        // Apply dynamic Material Design styling for confirmation dialogs
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinWidth(550); // Ensure enough space for two wide buttons
        dialogPane.setPrefWidth(600); // Comfortable preferred width
        dialogPane.setMaxWidth(750); // Allow even wider if needed
        dialogPane.setMinHeight(160); // Increased to account for custom button area
        dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        dialogPane.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);"
        );
        
        // Remove default header styling
        Node headerPanel = dialogPane.lookup(".header-panel");
        if (headerPanel != null) {
            headerPanel.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-padding: 0;"
            );
        }
        
        // Set button types - using JavaFX's built-in system for reliability
        ButtonType positiveButton = new ButtonType(positiveText, ButtonBar.ButtonData.OK_DONE);
        ButtonType negativeButton = new ButtonType(negativeText, ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(positiveButton, negativeButton);
        
        // Apply proper button centering and styling
        Platform.runLater(() -> {
            // Style the buttons with identical sizes
            Button posBtn = (Button) alert.getDialogPane().lookupButton(positiveButton);
            Button negBtn = (Button) alert.getDialogPane().lookupButton(negativeButton);
            
            if (posBtn != null && negBtn != null) {
                // Make buttons identical size - use the larger text width
                String longerText = positiveText.length() > negativeText.length() ? positiveText : negativeText;
                double maxTextWidth = calculateTextWidth(longerText, 14, true);
                int buttonWidth = Math.max(180, (int)(maxTextWidth + 80)); // Increased minimum to 180px and padding to 80px
                
                // Apply identical styling to both buttons
                applyIdenticalButton(posBtn, MATERIAL_COLORS.INFO, MATERIAL_COLORS.INFO_DARK, buttonWidth);
                applyIdenticalButton(negBtn, "#9E9E9E", "#757575", buttonWidth);
            }
            
            // Fix button centering across full modal width
            applyCenteredButtonBar(alert.getDialogPane());
        });
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == positiveButton;
    }
    
    /**
     * Shows a Material Design styled information dialog.
     * 
     * @param title the dialog title
     * @param message the information message
     */
    public static void showInfo(String title, String message) {
        showStyledAlert(Alert.AlertType.INFORMATION, title, message, MATERIAL_COLORS.INFO, MATERIAL_COLORS.INFO_DARK);
    }
    
    /**
     * Shows a Material Design styled error dialog.
     * 
     * @param title the dialog title
     * @param message the error message
     */
    public static void showError(String title, String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showError(title, message));
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null); // Clean Material Design look
        alert.setContentText(null); // We'll use custom content
        
        // Create custom content with Material Design styling
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("material-dialog-content-simple");
        
        // Message label with Material Design typography
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("material-dialog-message");
        messageLabel.setWrapText(true);
        messageLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        messageLabel.setMaxWidth(500); // Maximum width to prevent overly wide dialogs
        messageLabel.setMinHeight(Region.USE_PREF_SIZE); // Prevent text truncation
        
        content.getChildren().add(messageLabel);
        
        // Set the custom content
        alert.getDialogPane().setContent(content);
        
        // Apply dynamic Material Design styling for error dialogs
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinWidth(280); // Minimum to prevent button clipping
        dialogPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
        dialogPane.setMaxWidth(550); // Maximum to prevent overly wide dialogs
        dialogPane.setMinHeight(160); // Increased to account for custom button area
        dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        dialogPane.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);"
        );
        
        // Remove default header styling
        Node headerPanel = dialogPane.lookup(".header-panel");
        if (headerPanel != null) {
            headerPanel.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-padding: 0;"
            );
        }
        
        // Set button type - using JavaFX's built-in system
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
        
        // Apply proper button styling and centering
        Platform.runLater(() -> {
            Button btn = (Button) alert.getDialogPane().lookupButton(okButton);
            if (btn != null) {
                applyIdenticalButton(btn, MATERIAL_COLORS.ERROR, MATERIAL_COLORS.ERROR_DARK, 140);
            }
            applyCenteredButtonBar(alert.getDialogPane());
        });
        
        alert.showAndWait();
    }
    
    /**
     * Shows a Material Design styled warning dialog.
     * 
     * @param title the dialog title
     * @param message the warning message
     */
    public static void showWarning(String title, String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showWarning(title, message));
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null); // Clean Material Design look
        alert.setContentText(null); // We'll use custom content
        
        // Create custom content with Material Design styling
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("material-dialog-content-simple");
        
        // Message label with Material Design typography
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("material-dialog-message");
        messageLabel.setWrapText(true);
        messageLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        messageLabel.setMaxWidth(500); // Maximum width to prevent overly wide dialogs
        messageLabel.setMinHeight(Region.USE_PREF_SIZE); // Prevent text truncation
        
        content.getChildren().add(messageLabel);
        
        // Set the custom content
        alert.getDialogPane().setContent(content);
        
        // Apply dynamic Material Design styling for warning dialogs
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinWidth(280); // Minimum to prevent button clipping
        dialogPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
        dialogPane.setMaxWidth(550); // Maximum to prevent overly wide dialogs
        dialogPane.setMinHeight(160); // Increased to account for custom button area
        dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        dialogPane.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);"
        );
        
        // Remove default header styling
        Node headerPanel = dialogPane.lookup(".header-panel");
        if (headerPanel != null) {
            headerPanel.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-padding: 0;"
            );
        }
        
        // Set button type - using JavaFX's built-in system
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
        
        // Apply proper button styling and centering
        Platform.runLater(() -> {
            Button btn = (Button) alert.getDialogPane().lookupButton(okButton);
            if (btn != null) {
                applyIdenticalButton(btn, MATERIAL_COLORS.WARNING, MATERIAL_COLORS.WARNING_DARK, 140);
            }
            applyCenteredButtonBar(alert.getDialogPane());
        });

        alert.showAndWait();
    }
    
    /**
     * Shows a Material Design styled input dialog for entering text.
     * 
     * @param title the dialog title
     * @param header the header text
     * @param prompt the input field prompt text
     * @param defaultValue the default value (can be null)
     * @param onSave callback when save is clicked with the entered text
     * @param onCancel callback when cancel is clicked
     */
    public static void showInputDialog(String title, String header, String prompt, String defaultValue, 
                                     Consumer<String> onSave, Runnable onCancel) {
        Dialog<String> dialog = (preWarmedInputModal != null) ? preWarmedInputModal : new Dialog<>();
        
        // CRITICAL: Clear any existing button types to prevent duplication
        dialog.getDialogPane().getButtonTypes().clear();
        
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setResizable(true);
        
        // Create content
        VBox content = new VBox(DIMENSIONS.DIALOG_CONTENT_SPACING);
        content.setPadding(new Insets(DIMENSIONS.DIALOG_PADDING));
        content.setStyle(STYLES.MATERIAL_INFO_CONTENT_STYLE);
        
        // Header label
        Label headerLabel = new Label(header);
        headerLabel.setStyle(STYLES.MATERIAL_INFO_TITLE_STYLE);
        
        // Input field
        TextField inputField = new TextField(defaultValue);
        inputField.setPromptText(prompt);
        inputField.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-padding: 10; " +
            "-fx-background-radius: 4; " +
            "-fx-border-radius: 4; " +
            "-fx-border-color: #BDBDBD; " +
            "-fx-border-width: 1; " +
            "-fx-pref-column-count: 50;"  // Show about 50 characters visible
        );
        
        // Set preferred width to show substantial text
        inputField.setPrefWidth(450);
        inputField.setMaxWidth(Double.MAX_VALUE); // Allow it to grow with dialog
        
        // Focus on hover
        inputField.setOnMouseEntered(e -> 
            inputField.setStyle(inputField.getStyle() + "-fx-border-color: " + MATERIAL_COLORS.PRIMARY + ";")
        );
        inputField.setOnMouseExited(e -> 
            inputField.setStyle(inputField.getStyle() + "-fx-border-color: #BDBDBD;")
        );
        
        content.getChildren().addAll(headerLabel, inputField);
        
        // Set content
        dialog.getDialogPane().setContent(content);
        
        // Load CSS if not already loaded
        DialogPane dialogPane = dialog.getDialogPane();
        if (dialogPane.getStylesheets().isEmpty()) {
            dialogPane.getStylesheets().add(
                SpeleoDBModals.class.getResource(STYLES.MAIN_CSS_PATH).toExternalForm()
            );
        }
        
        // Apply size constraints to match other modals but wider for text input
        dialogPane.setMinWidth(500); // Wider minimum for text field
        dialogPane.setPrefWidth(550); // Preferred width
        dialogPane.setMaxWidth(600); // Allow slightly wider for long text
        dialogPane.setMinHeight(180); // Reasonable height for input dialog
        dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        
        // Apply Material Design styling (but don't let it override our size constraints)
        dialogPane.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);"
        );
        
        // Remove default header styling
        Node headerPanel = dialogPane.lookup(".header-panel");
        if (headerPanel != null) {
            headerPanel.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-padding: 0;"
            );
        }
        
        // Add buttons using JavaFX's built-in system
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);
        
        // Handle result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButton) {
                return inputField.getText();
            }
            return null;
        });
        
        // Apply proper button styling and centering
        Platform.runLater(() -> {
            Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveButton);
            Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelButton);
            
            if (saveBtn != null && cancelBtn != null) {
                // Make buttons identical size - use larger text width
                String longerText = "Save".length() > "Cancel".length() ? "Save" : "Cancel";
                double maxTextWidth = calculateTextWidth(longerText, 14, true);
                int buttonWidth = Math.max(140, (int)(maxTextWidth + 50));
                
                // Apply identical styling
                applyIdenticalButton(saveBtn, MATERIAL_COLORS.SUCCESS, MATERIAL_COLORS.SUCCESS_DARK, buttonWidth);
                applyIdenticalButton(cancelBtn, MATERIAL_COLORS.ERROR, MATERIAL_COLORS.ERROR_DARK, buttonWidth);
                
                // Allow Enter key to trigger Save
                inputField.setOnAction(e -> saveBtn.fire());
            }
            
            applyCenteredButtonBar(dialog.getDialogPane());
        });
        
        // Focus input field
        Platform.runLater(inputField::requestFocus);
        
        Optional<String> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            if (onSave != null) {
                onSave.accept(result.get());
            }
        } else {
            if (onCancel != null) {
                onCancel.run();
            }
        }
    }
    
    /**
     * Shows a save modal dialog when Ctrl+S / Cmd+S is pressed.
     */
    public static void showSaveModal(String projectName, Consumer<String> upload_callback, AnchorPane speleoDBAnchorPane) {
        
        SpeleoDBModals.showInputDialog(
            "Save Project on SpeleoDB",
            "Save Current Project: `" + projectName + "`",
            "What did you change?",
            "", // Empty default value
            message -> {
                if (message.trim().isEmpty()) {
                    logger.info(MESSAGES.UPLOAD_MESSAGE_EMPTY);
                    SpeleoDBModals.showError(DIALOGS.TITLE_UPLOAD_MESSAGE_REQUIRED, MESSAGES.UPLOAD_MESSAGE_EMPTY);
                    SpeleoDBTooltips.showError();
                } else {
                    upload_callback.accept(message.trim());
                }
            },
            () -> {
                // User cancelled
                logger.debug("User cancelled save dialog");
            }
        );
    }

    /**
     * Shows a Material Design styled success celebration dialog with animation.
     * 
     * @param gifPath path to the success GIF (optional)
     * @param onClose callback when dialog closes
     * @param owner the owner window
     */
    public static void showSuccessCelebration(String gifPath, Runnable onClose, Window owner) {
        try {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(DIALOGS.TITLE_SUCCESS_CELEBRATION);
            dialog.setHeaderText(null);
            dialog.setResizable(true);
            
            DialogPane dialogPane = dialog.getDialogPane();
            
            // Create content first to determine size based on GIF
            VBox content = new VBox(10);
            content.setAlignment(Pos.CENTER);
            content.setStyle("-fx-background-color: white; -fx-padding: 20;");
            
            // Calculate dialog dimensions with guaranteed button space
            final int GUARANTEED_BUTTON_AREA_HEIGHT = 80; // Reserve 80px for buttons
            
            if (gifPath != null) {
                try {
                    Image gifImage = new Image(SpeleoDBModals.class.getResourceAsStream(gifPath));
                    ImageView gifView = new ImageView(gifImage);
                    gifView.setPreserveRatio(true);
                    
                    // Calculate appropriate size based on actual image dimensions
                    double imageWidth = gifImage.getWidth();
                    double imageHeight = gifImage.getHeight();
                    double maxWidth = 700; // Reasonable max width for success dialogs
                    double maxHeight = 400; // Reduced to leave space for buttons
                    
                    // Scale to fit within max bounds while preserving ratio
                    double scale = Math.min(maxWidth / imageWidth, maxHeight / imageHeight);
                    if (scale > 1.0) scale = 1.0; // Don't upscale
                    
                    double fitWidth = imageWidth * scale;
                    double fitHeight = imageHeight * scale;
                    
                    gifView.setFitWidth(fitWidth);
                    gifView.setFitHeight(fitHeight);
                    content.getChildren().add(gifView);
                    
                    // Set dialog size with guaranteed button space
                    dialogPane.setMinWidth(Math.max(300, fitWidth + 80)); // Minimum viable width
                    dialogPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    dialogPane.setMaxWidth(fitWidth + 80); // Max based on content with padding
                    dialogPane.setMinHeight(Math.max(200, fitHeight + GUARANTEED_BUTTON_AREA_HEIGHT + 60)); // Guaranteed space
                    dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    dialogPane.setMaxHeight(fitHeight + GUARANTEED_BUTTON_AREA_HEIGHT + 100); // Extra space for safety
                    
                } catch (Exception e) {
                    logger.error("Failed to load celebration GIF", e);
                    addFallbackSuccessContent(content);
                    // Use dynamic sizing for fallback content with guaranteed button space
                    dialogPane.setMinWidth(350);
                    dialogPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    dialogPane.setMaxWidth(450); // Reasonable max for text-only success
                    dialogPane.setMinHeight(200 + GUARANTEED_BUTTON_AREA_HEIGHT);
                    dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    dialogPane.setMaxHeight(300 + GUARANTEED_BUTTON_AREA_HEIGHT);
                }
            } else {
                addFallbackSuccessContent(content);
                // Use dynamic sizing for fallback content with guaranteed button space
                dialogPane.setMinWidth(350);
                dialogPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
                dialogPane.setMaxWidth(450); // Reasonable max for text-only success
                dialogPane.setMinHeight(200 + GUARANTEED_BUTTON_AREA_HEIGHT);
                dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
                dialogPane.setMaxHeight(300 + GUARANTEED_BUTTON_AREA_HEIGHT);
            }
            
            // Apply Material Design styling
            dialogPane.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);"
            );
            
            // Load CSS if not already loaded
            if (dialogPane.getStylesheets().isEmpty()) {
                dialogPane.getStylesheets().add(
                    SpeleoDBModals.class.getResource(STYLES.MAIN_CSS_PATH).toExternalForm()
                );
            }
            
            dialogPane.setContent(content);
            
            // Add close button using JavaFX's built-in system
            ButtonType closeButton = new ButtonType(DIALOGS.BUTTON_CLOSE, ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(closeButton);
            
            // Apply proper button styling and centering
            Platform.runLater(() -> {
                Button btn = (Button) dialog.getDialogPane().lookupButton(closeButton);
                if (btn != null) {
                    applyIdenticalButton(btn, MATERIAL_COLORS.SUCCESS, MATERIAL_COLORS.SUCCESS_DARK, 140);
                }
                applyCenteredButtonBar(dialog.getDialogPane());
            });
            
            // Auto-close timer
            Timeline autoClose = new Timeline(
                new KeyFrame(
                    Duration.seconds(ANIMATIONS.SUCCESS_CELEBRATION_AUTO_CLOSE_SECONDS),
                    e -> dialog.close()
                )
            );
            autoClose.play();
            
            // Set owner
            if (owner != null) {
                dialog.initOwner(owner);
            }
            
            // Handle close
            dialog.setOnHidden(e -> {
                autoClose.stop();
                if (onClose != null) {
                    onClose.run();
                }
            });
            
            // Fade in animation
            dialogPane.setOpacity(0);
            dialog.show();
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialogPane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
            
        } catch (Exception e) {
            logger.error("Error showing success celebration", e);
            if (onClose != null) {
                onClose.run();
            }
        }
    }
    
    /**
     * Shows a custom Material Design dialog with full control over content.
     * 
     * @param title dialog title
     * @param content custom content node
     * @param buttons array of button configurations (text, callback, style)
     * @param owner owner window
     */
    public static void showCustomDialog(String title, Node content, ButtonConfig[] buttons, Window owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setResizable(true);
        
        DialogPane dialogPane = dialog.getDialogPane();
        applyMaterialDesignStyling(dialogPane);
        
        // Set content
        dialogPane.setContent(content);
        
        // Add buttons
        for (ButtonConfig config : buttons) {
            ButtonType buttonType = new ButtonType(config.text, config.isDefault ? 
                ButtonBar.ButtonData.OK_DONE : ButtonBar.ButtonData.LEFT);
            dialogPane.getButtonTypes().add(buttonType);
            
            // Style button after adding with Perfect Material Design
            Platform.runLater(() -> {
                Button btn = (Button) dialogPane.lookupButton(buttonType);
                if (btn != null) {
                    applyPerfectMaterialButton(btn, config.color, config.colorDark, config.isDefault);
                    if (config.callback != null) {
                        btn.setOnAction(e -> {
                            config.callback.run();
                            dialog.close();
                        });
                    }
                }
            });
        }
        
        if (owner != null) {
            dialog.initOwner(owner);
        }
        
        dialog.showAndWait();
    }
    
    /**
     * Configuration for custom dialog buttons
     */
    public static class ButtonConfig {
        final String text;
        final Runnable callback;
        final String color;
        final String colorDark;
        final boolean isDefault;
        
        public ButtonConfig(String text, Runnable callback, String color, String colorDark, boolean isDefault) {
            this.text = text;
            this.callback = callback;
            this.color = color;
            this.colorDark = colorDark;
            this.isDefault = isDefault;
        }
        
        public static ButtonConfig primary(String text, Runnable callback) {
            return new ButtonConfig(text, callback, MATERIAL_COLORS.PRIMARY, MATERIAL_COLORS.PRIMARY_DARK, true);
        }
        
        public static ButtonConfig secondary(String text, Runnable callback) {
            return new ButtonConfig(text, callback, MATERIAL_COLORS.ERROR, MATERIAL_COLORS.ERROR_DARK, false);
        }
    }
    
    // ==================== Helper Methods ====================
    
    private static void showStyledAlert(Alert.AlertType type, String title, String message, 
                                       String color, String colorDark) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showStyledAlert(type, title, message, color, colorDark));
            return;
        }
        
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setResizable(true);
        
        DialogPane dialogPane = alert.getDialogPane();
        applyMaterialDesignStyling(dialogPane);
        
        // Create content with Material Design styling
        VBox content = new VBox();
        content.setStyle(STYLES.MATERIAL_INFO_CONTENT_STYLE);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle(STYLES.MATERIAL_INFO_TITLE_STYLE);
        
        Label messageLabel = new Label(message);
        messageLabel.setStyle(STYLES.MATERIAL_INFO_TEXT_STYLE);
        messageLabel.setWrapText(true);
        messageLabel.setMinWidth(DIMENSIONS.INFO_DIALOG_MIN_WIDTH - 48);
        messageLabel.setPrefWidth(DIMENSIONS.INFO_DIALOG_PREF_WIDTH - 48);
        messageLabel.setMaxWidth(DIMENSIONS.INFO_DIALOG_PREF_WIDTH - 48);
        
        content.getChildren().addAll(titleLabel, messageLabel);
        dialogPane.setContent(content);
        
        // Add default button
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
        
        // Style button with Perfect Material Design
        Platform.runLater(() -> {
            Button btn = (Button) alert.getDialogPane().lookupButton(okButton);
            if (btn != null) {
                applyPerfectMaterialButton(btn, color, colorDark, true);
            }
        });
        
        alert.showAndWait();
    }
    
    private static void applyMaterialDesignStyling(DialogPane dialogPane) {
        dialogPane.setMinWidth(DIMENSIONS.INFO_DIALOG_MIN_WIDTH);
        dialogPane.setPrefWidth(DIMENSIONS.INFO_DIALOG_PREF_WIDTH);
        dialogPane.setMinHeight(DIMENSIONS.INFO_DIALOG_MIN_HEIGHT);
        dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        dialogPane.setStyle(STYLES.MATERIAL_INFO_DIALOG_STYLE);
        
        // Note: No longer applying unified button styling since we use custom button containers
    }
    
    private static void applyPerfectMaterialButton(Button button, String color, String colorDark, boolean isPrimary) {
        if (button == null) return;
        
        // Calculate optimal button width based on text content
        double textWidth = calculateTextWidth(button.getText(), 14, true); // 14px font, bold
        int optimalWidth = Math.max(120, (int)(textWidth + 60)); // Minimum 120px, add 60px padding
        
        String baseStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12 20; " +        // Slightly more horizontal padding
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand; " +
            "-fx-min-width: %dpx; " +       // Dynamic min width
            "-fx-pref-width: %dpx; " +      // Dynamic preferred width
            "-fx-max-width: %dpx; " +       // Dynamic max width (same as pref)
            "-fx-effect: dropshadow(three-pass-box, %s, 4, 0, 0, 2); " +
            "-fx-alignment: center; " +     // Center text within button
            "-fx-text-alignment: center;",
            color, optimalWidth, optimalWidth, optimalWidth,
            convertColorToRgba(color, 0.3)
        );
        
        String hoverStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12 20; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand; " +
            "-fx-min-width: %dpx; " +
            "-fx-pref-width: %dpx; " +
            "-fx-max-width: %dpx; " +
            "-fx-effect: dropshadow(three-pass-box, %s, 6, 0, 0, 3); " +
            "-fx-alignment: center; " +
            "-fx-text-alignment: center;",
            colorDark, optimalWidth, optimalWidth, optimalWidth,
            convertColorToRgba(colorDark, 0.4)
        );
        
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
        
        // Ensure text never changes during hover
        String originalText = button.getText();
        button.setOnAction(e -> button.setText(originalText));
    }
    
    /**
     * Calculate text width for optimal button sizing
     */
    private static double calculateTextWidth(String text, int fontSize, boolean bold) {
        if (text == null || text.isEmpty()) return 0;
        
        // Approximate character width calculation
        // Bold 14px Lato text is roughly 10-11px per character on average
        // Increased to ensure no clipping with the Lato font
        double charWidth = bold ? 11.0 : 9.5;
        return text.length() * charWidth;
    }
    
    /**
     * Convert hex color to rgba with alpha
     */
    private static String convertColorToRgba(String hexColor, double alpha) {
        if (hexColor.startsWith("#")) {
            try {
                int r = Integer.valueOf(hexColor.substring(1, 3), 16);
                int g = Integer.valueOf(hexColor.substring(3, 5), 16);
                int b = Integer.valueOf(hexColor.substring(5, 7), 16);
                return String.format("rgba(%d,%d,%d,%.1f)", r, g, b, alpha);
            } catch (Exception e) {
                return "rgba(0,0,0," + alpha + ")";
            }
        }
        return hexColor;
    }
    
    /**
     * Applies identical sizing and styling to buttons
     */
    private static void applyIdenticalButton(Button button, String color, String colorDark, int width) {
        String baseStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12 20; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand; " +
            "-fx-min-width: %dpx; " +
            "-fx-pref-width: %dpx; " +
            "-fx-max-width: %dpx; " +
            "-fx-effect: dropshadow(three-pass-box, %s, 4, 0, 0, 2); " +
            "-fx-alignment: center; " +
            "-fx-text-alignment: center;",
            color, width, width, width,
            convertColorToRgba(color, 0.3)
        );
        
        String hoverStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12 20; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand; " +
            "-fx-min-width: %dpx; " +
            "-fx-pref-width: %dpx; " +
            "-fx-max-width: %dpx; " +
            "-fx-effect: dropshadow(three-pass-box, %s, 6, 0, 0, 3); " +
            "-fx-alignment: center; " +
            "-fx-text-alignment: center;",
            colorDark, width, width, width,
            convertColorToRgba(colorDark, 0.4)
        );
        
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }
    
    /**
     * Centers the ButtonBar across the full modal width using CSS
     */
    private static void applyCenteredButtonBar(DialogPane dialogPane) {
        // Apply CSS that centers the button bar across the full dialog width
        String buttonCenteringCSS = 
            ".dialog-pane { -fx-padding: 0; } " +
            ".dialog-pane .button-bar { " +
            "   -fx-padding: 16 0 20 0; " +           // Remove side padding
            "   -fx-alignment: center; " +            // Center the buttons
            "   -fx-spacing: 12; " +                  // Space between buttons
            "   -fx-background-color: #FAFAFA; " +
            "   -fx-border-color: #E0E0E0; " +
            "   -fx-border-width: 1 0 0 0; " +        // Top border only
            "} " +
            ".dialog-pane .button-bar .button { " +
            "   -fx-margin: 0 6 0 6; " +              // Equal margin around buttons
            "}";
        
        Platform.runLater(() -> {
            try {
                dialogPane.getStylesheets().add("data:text/css," + 
                    java.net.URLEncoder.encode(buttonCenteringCSS, java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception e) {
                // Fallback to inline styling
                ButtonBar buttonBar = (ButtonBar) dialogPane.lookup(".button-bar");
                if (buttonBar != null) {
                    buttonBar.setStyle(
                        "-fx-padding: 16 0 20 0; " +
                        "-fx-alignment: center; " +
                        "-fx-spacing: 12; " +
                        "-fx-background-color: #FAFAFA; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1 0 0 0;"
                    );
                }
            }
        });
    }
    
    private static void addFallbackSuccessContent(VBox content) {
        Label successLabel = new Label("🎉 Success! 🎉");
        successLabel.setStyle(
            "-fx-font-size: 24px; " +
            "-fx-text-fill: " + MATERIAL_COLORS.SUCCESS + "; " +
            "-fx-font-weight: bold;"
        );
        content.getChildren().add(successLabel);
    }
} 