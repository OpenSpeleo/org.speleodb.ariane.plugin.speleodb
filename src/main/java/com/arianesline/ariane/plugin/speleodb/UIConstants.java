package com.arianesline.ariane.plugin.speleodb;

import javafx.util.Duration;

/**
 * Constants for UI dimensions, styling, and configuration.
 * Centralizes magic numbers to improve maintainability and consistency.
 */
public final class UIConstants {
    
    // Prevent instantiation
    private UIConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // ==================== DIALOG DIMENSIONS ==================== //
    
    /** Standard field width used across dialogs */
    public static final double STANDARD_FIELD_WIDTH = 350.0;
    
    /** Standard padding for content areas */
    public static final double STANDARD_PADDING = 20.0;
    
    /** Minimum width for dialog panes */
    public static final double DIALOG_MIN_WIDTH = 500.0;
    
    /** Preferred width for dialogs */
    public static final double DIALOG_PREF_WIDTH = 500.0;
    
    /** Standard spacing between UI elements */
    public static final double STANDARD_SPACING = 15.0;
    
    /** Small spacing for compact layouts */
    public static final double SMALL_SPACING = 5.0;
    
    /** Large spacing for section separation */
    public static final double LARGE_SPACING = 10.0;
    
    // ==================== ANIMATION SETTINGS ==================== //
    
    /** Duration for success/error animations */
    public static final Duration ANIMATION_DURATION = Duration.seconds(4.0);
    
    /** Fade in duration for animations */
    public static final Duration FADE_IN_DURATION = Duration.millis(400);
    
    /** Fade out duration for animations */
    public static final Duration FADE_OUT_DURATION = Duration.millis(400);
    
    // ==================== TEXT AREA SETTINGS ==================== //
    
    /** Default number of rows for text areas */
    public static final int TEXT_AREA_ROWS = 4;
    
    /** Maximum width for text areas */
    public static final double TEXT_AREA_MAX_WIDTH = Double.MAX_VALUE;
    
    // ==================== GRID LAYOUT SETTINGS ==================== //
    
    /** Standard horizontal gap for grids */
    public static final double GRID_HGAP = 10.0;
    
    /** Standard vertical gap for grids */
    public static final double GRID_VGAP = 10.0;
    
    /** Grid padding for content areas */
    public static final double GRID_PADDING = 20.0;
    
    /** Small grid padding */
    public static final double GRID_PADDING_SMALL = 10.0;
    
    // ==================== ANCHOR PANE POSITIONING ==================== //
    
    /** Top anchor for overlay elements */
    public static final double OVERLAY_TOP_ANCHOR = 20.0;
    
    /** Left anchor for overlay elements */
    public static final double OVERLAY_LEFT_ANCHOR = 50.0;
    
    /** Right anchor for overlay elements */
    public static final double OVERLAY_RIGHT_ANCHOR = 50.0;
    
    // ==================== BUTTON STYLING ==================== //
    
    /** Standard button column span */
    public static final int BUTTON_STANDARD_SPAN = 1;
    
    /** Full width button column span */
    public static final int BUTTON_FULL_SPAN = 2;
    
    /** Extended button column span */
    public static final int BUTTON_EXTENDED_SPAN = 3;
    
    // ==================== COLOR STYLES ==================== //
    
    /** Success color for animations and indicators */
    public static final String SUCCESS_COLOR = "#4CAF50";
    
    /** Error color for animations and indicators */
    public static final String ERROR_COLOR = "#F44336";
    
    /** Required field indicator color */
    public static final String REQUIRED_COLOR = "#dc2626";
    
    /** White text color */
    public static final String WHITE_TEXT = "white";
    
    // ==================== FONT SETTINGS ==================== //
    
    /** Standard font size for animations */
    public static final String ANIMATION_FONT_SIZE = "16px";
    
    /** Standard font weight for emphasis */
    public static final String BOLD_FONT_WEIGHT = "bold";
    
    /** Medium font size for labels */
    public static final double LABEL_FONT_SIZE = 12.0;
    
    // ==================== STYLE TEMPLATES ==================== //
    
    /** 
     * Style template for success animations 
     */
    public static final String SUCCESS_ANIMATION_STYLE = 
        "-fx-background-color: " + SUCCESS_COLOR + "; " +
        "-fx-text-fill: " + WHITE_TEXT + "; " +
        "-fx-padding: 15 20; " +
        "-fx-background-radius: 8; " +
        "-fx-font-size: " + ANIMATION_FONT_SIZE + "; " +
        "-fx-font-weight: " + BOLD_FONT_WEIGHT + "; " +
        "-fx-border-radius: 8; " +
        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 2);";
    
    /** 
     * Style template for error animations 
     */
    public static final String ERROR_ANIMATION_STYLE = 
        "-fx-background-color: " + ERROR_COLOR + "; " +
        "-fx-text-fill: " + WHITE_TEXT + "; " +
        "-fx-padding: 15 20; " +
        "-fx-background-radius: 8; " +
        "-fx-font-size: " + ANIMATION_FONT_SIZE + "; " +
        "-fx-font-weight: " + BOLD_FONT_WEIGHT + "; " +
        "-fx-border-radius: 8; " +
        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 2);";
    
    /** 
     * Style template for required field indicators 
     */
    public static final String REQUIRED_FIELD_STYLE = "-fx-text-fill: " + REQUIRED_COLOR + ";";
    
    // ==================== LAYOUT HELPERS ==================== //
    
    /**
     * Creates standard insets for content padding.
     * 
     * @return standard padding insets
     */
    public static javafx.geometry.Insets getStandardPadding() {
        return new javafx.geometry.Insets(STANDARD_PADDING);
    }
    
    /**
     * Creates grid padding insets.
     * 
     * @return grid padding insets
     */
    public static javafx.geometry.Insets getGridPadding() {
        return new javafx.geometry.Insets(GRID_PADDING, GRID_PADDING, GRID_PADDING_SMALL, GRID_PADDING);
    }
    
    /**
     * Creates small spacing insets.
     * 
     * @return small spacing insets
     */
    public static javafx.geometry.Insets getSmallSpacing() {
        return new javafx.geometry.Insets(SMALL_SPACING);
    }
} 