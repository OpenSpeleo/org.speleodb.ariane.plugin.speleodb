package org.speleodb.ariane.plugin.speleodb;

import com.arianesline.ariane.plugin.api.Plugin;

import javafx.scene.control.ButtonType;

/**
 * Constants for the SpeleoDB plugin to eliminate hard-coded strings and magic numbers.
 * All string literals, numeric values, and configuration parameters are centralized here.
 */
public final class SpeleoDBConstants {
    
    // ==================== VERSION INFORMATION ====================
    /**
     * SpeleoDB Plugin Version (CalVer format: YYYY.MM.DD)
     */
    public static final String VERSION = null;
    public static final String VERSION_DISPLAY = VERSION != null ? "v" + VERSION : "Development";
    
    /**
     * ARIANE Software Version (loaded from Plugin API containerVersion)
     */
    public static final String ARIANE_VERSION = Plugin.containerVersion.toString();
    public static final String ARIANE_SOFTWARE_NAME = "ARIANE";
    
    // ==================== TEST MODE ====================
    /**
     * Test Mode Flag
     * When set to true, the plugin uses isolated test preferences to avoid affecting user data.
     * This is automatically set by the build system during test execution.
     */
    public static final boolean TEST_MODE = false;
    
    // ==================== PREVENT INSTANTIATION ====================
    private SpeleoDBConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
    
    // ==================== ENUMS ====================
    /**
     * Sorting modes for project lists
     */
    public static enum SortMode { 
        BY_NAME, 
        BY_DATE 
    }
    
    /**
     * Access levels for SpeleoDB projects
     */
    public static enum AccessLevel {
        ADMIN, 
        READ_AND_WRITE, 
        READ_ONLY
    }
    // ==================== URL CONSTANTS ====================
    public static final class URLS {
        // public static final String WEBVIEW = "http://localhost:8000/webview/ariane/";
        public static final String WEBVIEW = "https://" + PREFERENCES.DEFAULT_INSTANCE + "/webview/ariane/";
    }
    
    // ==================== API ENDPOINTS ====================
    public static final class API {
        public static final String BASE_PATH = "/api/v1";
        public static final String AUTH_TOKEN_ENDPOINT = BASE_PATH + "/user/auth-token/";
        public static final String PROJECTS_ENDPOINT = BASE_PATH + "/projects/";
        public static final String ANNOUNCEMENTS_ENDPOINT = BASE_PATH + "/announcements/";
        public static final String PLUGIN_RELEASES_ENDPOINT = BASE_PATH + "/plugin_releases/";
        public static final String UPLOAD_ARIANE_TML_PATH = "/upload/ariane_tml/";
        public static final String ACQUIRE_LOCK_PATH = "/acquire/";
        public static final String RELEASE_LOCK_PATH = "/release/";
        public static final String DOWNLOAD_ARIANE_TML_PATH = "/download/ariane_tml/";
    }
    
    // ==================== HTTP STATUS CODES ====================
    public static final class HTTP_STATUS {
        public static final int OK = 200;
        public static final int CREATED = 201;
        public static final int UNPROCESSABLE_ENTITY = 422;
    }
    
    // ==================== NETWORK & CONNECTION ====================
    public static final class NETWORK {
        public static final String HTTP_PROTOCOL = "http://";
        public static final String HTTPS_PROTOCOL = "https://";
        public static final String LOCAL_PATTERN = "(^localhost(:[0-9]+)?$)|(^127\\.[0-9]+\\.[0-9]+\\.[0-9]+(:[0-9]+)?$)|(^10\\.[0-9]+\\.[0-9]+\\.[0-9]+(:[0-9]+)?$)|(^172\\.(1[6-9]|2[0-9]|3[0-1])\\.[0-9]+\\.[0-9]+(:[0-9]+)?$)|(^192\\.168\\.[0-9]+\\.[0-9]+(:[0-9]+)?$)";
        public static final int CONNECT_TIMEOUT_SECONDS = 30;
        public static final int REQUEST_TIMEOUT_SECONDS = 60;
        public static final int DOWNLOAD_TIMEOUT_SECONDS = 120;
        public static final int DEFAULT_TIMEOUT_MILLIS = 10000;
        public static final int EXECUTOR_SHUTDOWN_TIMEOUT_MILLIS = 500;
    }
    
    // ==================== HTTP HEADERS ====================
    public static final class HEADERS {
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String AUTHORIZATION = "Authorization";
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_JSON_UTF8 = "application/json; charset=utf-8";
        public static final String TOKEN_PREFIX = "Token ";
        public static final String MULTIPART_FORM_DATA = "multipart/form-data";
        public static final String TEXT_PLAIN = "text/plain";
        public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    }
    
    // ==================== MULTIPART CONSTANTS ====================
    public static final class MULTIPART {
        public static final String CRLF = "\r\n";
        public static final String QUOTE_CRLF = "\"\r\n";
        public static final String CONTENT_TYPE_PREFIX = "multipart/form-data; boundary=";
        public static final String CONTENT_TYPE_HEADER = "Content-Type: ";
        public static final String DOUBLE_CRLF = "\r\n\r\n";
        public static final String OCTET_STREAM_HEADER = "Content-Type: application/octet-stream\r\n\r\n";
        public static final String BOUNDARY_PREFIX = "Boundary";
        public static final String BOUNDARY_START = "--";
        public static final String BOUNDARY_END = "--";
        public static final String DASH = "-";
        public static final String CONTENT_DISPOSITION_FORM_DATA = "Content-Disposition: form-data; name=\"";
        public static final String FILENAME_PARAM = "\"; filename=\"";
        public static final String DEFAULT_MIMETYPE = "text/plain";
    }
    
    // ==================== USER PREFERENCES ====================
    public static final class PREFERENCES {
        public static final String PREF_EMAIL = "SDB_EMAIL";
        public static final String PREF_PASSWORD = "SDB_PASSWORD";
        public static final String PREF_OAUTH_TOKEN = "SDB_OAUTH_TOKEN";
        public static final String PREF_INSTANCE = "SDB_INSTANCE";
        public static final String PREF_SUCCESS_GIF_INDEX = "SDB_SUCCESS_GIF_INDEX";
        public static final String PREF_DISPLAYED_ANNOUNCEMENTS = "SDB_DISPLAYED_ANNOUNCEMENTS";
        public static final String DEFAULT_INSTANCE = "www.speleoDB.org";
        
        // Test preferences node path (without leading slash for Preferences.userRoot().node())
        public static final String TEST_PREFERENCES_NODE = "org/speleodb/ariane/plugin/speleodb/test";
        // Test preferences absolute path (with leading slash for path comparisons)
        public static final String TEST_PREFERENCES_PATH = "/" + TEST_PREFERENCES_NODE;
    }
    
    // ==================== FILE PATHS & DIRECTORIES ====================
    public static final class PATHS {
        public static final String ARIANE_ROOT_DIR = System.getProperty("user.home") + System.getProperty("file.separator") + ".ariane";
        public static final String PLUGINS_DIR = ARIANE_ROOT_DIR + System.getProperty("file.separator") + "Plugins";
        public static final String COUNTRIES_RESOURCE = "countries.json";
        public static final String DEBUG_PROPERTIES = "/debug.properties";
        public static final String TML_FILE_EXTENSION = ".tml";
        public static final String JAR_FILE_EXTENSION = ".jar";
        public static final String SPELEODB_FXML = "/fxml/SpeleoDB.fxml";
        public static final String LOGO_IMAGE = "/images/logo.png";
        public static final String EMPTY_TML = "/tml/empty_project.tml";
        public static final String SUCCESS_GIFS_DIR = "/images/success_gifs/";
        public static final String SUCCESS_GIFS_PATTERN = "*.gif";
    }
    
    // ==================== LOGGING CONFIGURATION ====================
    public static final class LOGGING {
        public static final String ARIANE_DIR = System.getProperty("user.home") + System.getProperty("file.separator") + ".ariane";
        public static final String SDB_DIR = ARIANE_DIR + System.getProperty("file.separator") + "speleodb";
        public static final String LOG_DIR = SDB_DIR + System.getProperty("file.separator") + "logs";
        public static final String LOG_FILE_NAME = "speleodb-plugin.log";
        public static final String LOG_FILE_PATH = LOG_DIR + System.getProperty("file.separator") + LOG_FILE_NAME;
        public static final String LOG_ARCHIVE_PATTERN = "speleodb-plugin-%d{yyyy-MM-dd}.log";
        public static final String LOG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
        public static final String LOG_LEVEL_DEBUG = "DEBUG";
        public static final String LOG_LEVEL_INFO = "INFO";
        public static final String LOG_LEVEL_WARN = "WARN";
        public static final String LOG_LEVEL_ERROR = "ERROR";
        public static final long LOG_FILE_MAX_SIZE_MB = 10;
        public static final long LOG_FILE_MAX_SIZE_BYTES = LOG_FILE_MAX_SIZE_MB * 1024 * 1024;
        public static final int LOG_FILE_MAX_BACKUP_COUNT = 5;
        public static final String LOG_FORMAT = "[%s] %s - %s%n";
        public static final String LOG_STARTUP_MESSAGE = "SpeleoDB Plugin logging initialized - Log file: ";
        public static final String LOG_SHUTDOWN_MESSAGE = "SpeleoDB Plugin logging shutdown";
        public static final String LOG_FILE_ROTATION_MESSAGE = "Log file rotated due to size limit";
        public static final String LOG_ERROR_WRITING = "Error writing to log file: ";
        public static final String LOG_ERROR_CREATING_DIR = "Error creating log directory: ";
        public static final String LOG_ERROR_ROTATING = "Error rotating log file: ";
    }
    
    // ==================== VALIDATION PATTERNS ====================
    public static final class VALIDATION {
        public static final String OAUTH_TOKEN_PATTERN = "^[a-f0-9]{40}$";
        public static final int OAUTH_TOKEN_LENGTH = 40;
        public static final String TOKEN_JSON_START = "\"token\":\"";
        public static final int TOKEN_JSON_START_LENGTH = 9;
    }
    
    // ==================== UI MESSAGES ====================
    public static final class MESSAGES {
        // Error Messages
        public static final String UPLOAD_MESSAGE_EMPTY = "Upload message cannot be empty.";
        public static final String USER_NOT_AUTHENTICATED = "User is not authenticated. Please log in.";
        public static final String USER_NOT_AUTHENTICATED_SHORT = "User is not authenticated.";
        public static final String PROJECT_LOCK_RELEASED = "Project lock released before application shutdown.";
        public static final String ERROR_LOADING_COUNTRIES = "Error loading countries: ";
        public static final String ERROR_PRE_LOADING_COUNTRIES = "Error pre-loading countries data: ";
        public static final String COUNTRIES_NOT_FOUND = "countries.json not found in resources at: ";
        public static final String INCOHERENT_FILE_ID = "Incoherent File ID detected.";
        public static final String PREVIOUS_VALUE = "\t- Previous Value: ";
        public static final String NEW_VALUE = "\t- New Value: ";
        public static final String SPELEODB_ID_UPDATED = "SpeleoDB ID updated successfully.";
        public static final String ERROR_CHECKING_SPELEODB_ID = "Error checking/updating SpeleoDB ID: ";
        public static final String UNKNOWN_ERROR = "Unknown error";
        
        // Success Messages
        public static final String SUCCESS_DEFAULT = "Success";
        public static final String ERROR_DEFAULT = "Error";
        public static final String PROJECT_LOADED_SUCCESS = "Project loaded successfully: ";
        public static final String PROJECT_LOADED_READONLY_SUCCESS = "Read-only project loaded successfully: ";
        public static final String COUNTRIES_CACHED_SUCCESS = "Successfully cached %d countries from JSON";
        public static final String OAUTH_TOKEN_SAVED = "OAuth token saved to preferences (format validated)";
        public static final String PASSWORD_SAVED = "Password saved to preferences";
        public static final String OAUTH_TOKEN_FORMAT_PASSED = "OAuth token format validation: PASSED";
        public static final String ADDING_SPELEODB_ID = "Adding SpeleoDB ID: ";
        
        // Update Messages
        public static final String UPDATE_CHECK_STARTING = "Checking for SpeleoDB plugin updates ...";
        public static final String UPDATE_AVAILABLE = "SpeleoDB update available: v%s";
        public static final String UPDATE_NOT_AVAILABLE = "SpeleoDB plugin is up to date (v%s)";
        public static final String UPDATE_DOWNLOAD_STARTING = "Downloading SpeleoDB update v%s ...";
        public static final String UPDATE_DOWNLOAD_SUCCESS = "SpeleoDB updated to version %s";
        public static final String UPDATE_HASH_VERIFICATION_FAILED = "Update download failed: SHA256 hash verification failed";
        public static final String UPDATE_DOWNLOAD_FAILED = "Failed to download SpeleoDB update: %s";
        public static final String UPDATE_CHECK_FAILED = "Failed to check for updates: %s";
        public static final String UPDATE_INSTALL_SUCCESS = "SpeleoDB plugin updated successfully to v%s";
        public static final String UPDATE_DIALOG_TITLE = "SpeleoDB Updated";
        public static final String UPDATE_DIALOG_HEADER = "Plugin Update Successful";
        public static final String UPDATE_RESTART_WARNING = "Please restart Ariane now for the new plugin to take effect";
        
        // Loading Messages
        public static final String LOADING_PROJECT = "Loading project file ...";
        public static final String LOADING_READONLY_PROJECT = "Loading read-only project file ...";
        
        // Validation Messages
        public static final String OAUTH_TOKEN_FORMAT_FAILED = "OAuth token format validation: FAILED - Expected 40 hex characters, got: %d characters";
        public static final String OAUTH_TOKEN_INVALID_NOT_SAVED = "Warning: Invalid OAuth token format not saved to preferences";
        
        // Connection Error Messages
        public static final String NETWORK_ERROR_ADVICE = 
                """
                Can't reach server - Please check:
                \u2022 Server is online and accessible
                \u2022 Network connection is working
                \u2022 Server URL is correct
                \u2022 Firewall isn't blocking the connection""";
        public static final String TIMEOUT_ERROR_ADVICE = 
                """
                Request timed out - Server may be:
                \u2022 Overloaded or slow to respond
                \u2022 Experiencing network issues
                \u2022 Temporarily unavailable
                Try again in a few moments""";
        
        // Auth Error Messages
        public static final String AUTH_FAILED_STATUS = "Authentication failed with status code: ";
        public static final String PROJECT_CREATE_FAILED_STATUS = "Failed to create project with status code: ";
        public static final String PROJECT_LIST_FAILED_STATUS = "Failed to list projects with status code: ";
        public static final String PROJECT_DOWNLOAD_FAILED_STATUS = "Failed to download project with status code: ";
        public static final String PROJECT_UPLOAD_FAILED_STATUS = "Failed to upload project with status code: ";
        public static final String PROJECT_DOWNLOAD_UNEXPECTED_STATUS = ". Expected: 200 (success), or 422 (project empty)";
        public static final String PROJECT_DOWNLOAD_404_EMPTY = "HTTP 422: Project exists but is empty - create empty TML file.";
        public static final String FILE_NOT_FOUND = "Downloaded file not found: ";
        public static final String DOWNLOAD_FAILED = "Failed to download project file";
        public static final String DOWNLOAD_PROJECT_FAILED = "Failed to download project: ";
        
        // Generic failure formats
        public static final String OPERATION_FAILED_FORMAT = "%s failed: %s";
        
        // Multipart body building errors
        public static final String ERROR_BUILDING_MULTIPART_BODY = "Error building HTTP request multipart body";
        public static final String ERROR_WRITING_MULTIPART_PART = "Error writing multipart part: ";
        public static final String ERROR_COPYING_FILE_CONTENT = "Error copying file content: ";
        
        // FXML loading errors
        public static final String ERROR_LOADING_FXML = "Error loading FXML for SpeleoDB UI: ";
    }
    
    // ==================== UI DIALOG CONSTANTS ====================
    public static final class DIALOGS {
        // Button Text
        public static final String BUTTON_YES = "Yes";
        public static final String BUTTON_NO = "No";
        public static final String BUTTON_SAVE_PROJECT = "Save Project";
        public static final String BUTTON_SAVE_CHANGES = "Save Changes";
        public static final String BUTTON_CANCEL = "Cancel";
        public static final String BUTTON_CONNECT = "CONNECT";
        public static final String BUTTON_CLOSE = "Close";
        public static final String BUTTON_GOT_IT = "Got it!";
        
        // Dialog Titles
        public static final String TITLE_SAVE_PROJECT = "Save Project on SpeleoDB";
        public static final String TITLE_CREATE_NEW_PROJECT = "Create New Project";
        public static final String TITLE_UPLOAD_MESSAGE_REQUIRED = "Upload Message Required";
        public static final String TITLE_SUCCESS_CELEBRATION = "Upload Successful!";

        // Announcement DefaultValues
        public static final String DEFAULT_ANNOUNCEMENT_TITLE = "SpeleoDB";
        public static final String DEFAULT_ANNOUNCEMENT_HEADER = "";
        
        // Dialog Headers
        public static final String HEADER_ENTER_PROJECT_DETAILS = "Enter project details";
        
        // Input Labels and Prompts
        public static final String LABEL_PROJECT_NAME = "Project Name:";
        public static final String LABEL_DESCRIPTION = "Description:";
        public static final String LABEL_COUNTRY = "Country:";
        public static final String LABEL_LATITUDE = "Latitude:";
        public static final String LABEL_LONGITUDE = "Longitude:";
        public static final String LABEL_UPLOAD_MESSAGE = "What did you change?";
        public static final String ASTERISK_REQUIRED = " *";
        
        // Prompt Text
        public static final String PROMPT_PROJECT_NAME = "Enter project name";
        public static final String PROMPT_DESCRIPTION = "Enter project description";
        public static final String PROMPT_COUNTRY = "Select a country ...";
        public static final String PROMPT_LATITUDE = "[Optional]";
        public static final String PROMPT_LONGITUDE = "[Optional]";
        public static final String PROMPT_UPLOAD_MESSAGE = "What did you modify and with whom ?";
    }
    
    // ==================== UI STYLING ====================
    public static final class STYLES {
        // CSS File Path
        public static final String MAIN_CSS_PATH = "/css/fxmlmain.css";
        
        // Material Design Colors
        public static final class MATERIAL_COLORS {
            public static final String PRIMARY = "#2196F3";       // Blue 500
            public static final String PRIMARY_DARK = "#1976D2";  // Blue 700
            public static final String SUCCESS = "#4CAF50";       // Green 500
            public static final String SUCCESS_DARK = "#388E3C";  // Green 700
            public static final String ERROR = "#F44336";         // Red 500
            public static final String ERROR_DARK = "#D32F2F";    // Red 700
            public static final String WARNING = "#FF9800";       // Orange 500
            public static final String WARNING_DARK = "#F57C00";  // Orange 700
            public static final String INFO = "#2196F3";          // Blue 500
            public static final String INFO_DARK = "#1976D2";     // Blue 700
        }
        
        // Colors
        public static final String COLOR_SUCCESS_BG = "#9CCC65";  // Green 400
        public static final String COLOR_SUCCESS_BORDER = "#43A047";  // Green 600
        public static final String COLOR_ERROR_BG = "#EF5350";  // Red 400
        public static final String COLOR_ERROR_BORDER = "#E53935";  // Red 600
        public static final String COLOR_MATERIAL_PRIMARY = "#1976D2";
        public static final String COLOR_MATERIAL_SURFACE = "#FFFFFF";
        public static final String COLOR_MATERIAL_ON_SURFACE = "#212121";
        public static final String COLOR_MATERIAL_SHADOW = "rgba(0,0,0,0.2)";
        
        // Success Animation Style
        public static final String SUCCESS_STYLE = "-fx-background-color:" + STYLES.COLOR_SUCCESS_BG + "; -fx-text-fill: white; " +
                "-fx-padding: 15 20; -fx-background-radius: 8; -fx-font-size: 16px; " +
                "-fx-font-weight: bold; -fx-border-radius: 8; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 2); " +
                "-fx-opacity: 1.0; -fx-mouse-transparent: true;";
        
        // Error Animation Style
        public static final String ERROR_STYLE = "-fx-background-color: " + STYLES.COLOR_ERROR_BG + "; -fx-text-fill: white; " +
                "-fx-padding: 15 25; -fx-background-radius: 10; -fx-font-size: 18px; " +
                "-fx-font-weight: bold; -fx-border-radius: 10; " +
                "-fx-border-color: " + STYLES.COLOR_ERROR_BG + "; -fx-border-width: 2; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(220,20,60,0.5), 15, 0, 0, 3); " +
                "-fx-opacity: 1.0; -fx-mouse-transparent: true;";
        
        // Debug Animation Style (highly visible for testing)
        public static final String DEBUG_ANIMATION_STYLE = "-fx-background-color: #FF6600; -fx-text-fill: white; " +
                "-fx-padding: 20 30; -fx-background-radius: 15; -fx-font-size: 20px; " +
                "-fx-font-weight: bold; -fx-border-radius: 15; " +
                "-fx-border-color: #FF4400; -fx-border-width: 3; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(255,102,0,0.8), 20, 0, 0, 5); " +
                "-fx-opacity: 1.0; -fx-mouse-transparent: true;";
        
        // Material Design Information Dialog Style
        public static final String MATERIAL_INFO_DIALOG_STYLE = 
                "-fx-background-color: " + COLOR_MATERIAL_SURFACE + "; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(three-pass-box, " + COLOR_MATERIAL_SHADOW + ", 16, 0, 0, 8); " +
                "-fx-padding: 0;";
        
        public static final String MATERIAL_INFO_CONTENT_STYLE = 
                "-fx-background-color: " + COLOR_MATERIAL_SURFACE + "; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 24; " +
                "-fx-spacing: 16; " +
                "-fx-pref-width: -1; " +
                "-fx-pref-height: -1;";
        
        public static final String MATERIAL_INFO_TITLE_STYLE = 
                "-fx-font-size: 20px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: " + COLOR_MATERIAL_ON_SURFACE + "; " +
                "-fx-padding: 0 0 8 0;";
        
        public static final String MATERIAL_INFO_TEXT_STYLE = 
                "-fx-font-size: 14px; " +
                "-fx-text-fill: " + COLOR_MATERIAL_ON_SURFACE + "; " +
                "-fx-line-spacing: 6; " +
                "-fx-wrap-text: true; " +
                "-fx-padding: 8 0 8 0;";
        
        public static final String MATERIAL_WARNING_TEXT_STYLE = 
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #F57C00; " +
                "-fx-background-color: #FFF3E0; " +
                "-fx-background-radius: 4; " +
                "-fx-padding: 12 16; " +
                "-fx-border-color: #FFB74D; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 4; " +
                "-fx-line-spacing: 4; " +
                "-fx-wrap-text: true; " +
                "-fx-margin: 8 0 8 0;";
        
        public static final String MATERIAL_BUTTON_STYLE = 
                "-fx-background-color: " + COLOR_MATERIAL_PRIMARY + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 12 24; " +
                "-fx-background-radius: 4; " +
                "-fx-cursor: hand; " +
                "-fx-min-width: 100px; " +
                "-fx-pref-width: 100px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(25,118,210,0.3), 4, 0, 0, 2);";
        
        public static final String MATERIAL_BUTTON_HOVER_STYLE = 
                "-fx-background-color: #1565C0; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 12 24; " +
                "-fx-background-radius: 4; " +
                "-fx-cursor: hand; " +
                "-fx-min-width: 100px; " +
                "-fx-pref-width: 100px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(25,118,210,0.4), 6, 0, 0, 3);";
        
        // Required Field Style
        public static final String REQUIRED_FIELD_STYLE = "-fx-text-fill: #dc2626;";
    }
    
    // ==================== UI DIMENSIONS ====================
    public static final class DIMENSIONS {
        // Dialog Dimensions
        public static final int DIALOG_MIN_WIDTH = 500;
        public static final int DIALOG_PREF_WIDTH = 500;
        public static final int NEW_PROJECT_DIALOG_PREF_WIDTH = 400;
        public static final int SUCCESS_DIALOG_MIN_WIDTH = 400;
        public static final int SUCCESS_DIALOG_MIN_HEIGHT = 300;
        public static final int SUCCESS_DIALOG_MAX_WIDTH = 800;
        public static final int SUCCESS_DIALOG_MAX_HEIGHT = 600;
        public static final int INFO_DIALOG_MIN_WIDTH = 520;
        public static final int INFO_DIALOG_PREF_WIDTH = 520;
        public static final int INFO_DIALOG_MIN_HEIGHT = 380;
        
        // Field Dimensions
        public static final int FIELD_PREF_WIDTH = 350;
        public static final int DESCRIPTION_FIELD_ROWS = 4;
        
        // Layout Spacing
        public static final int DIALOG_CONTENT_SPACING = 15;
        public static final int SECTION_SPACING = 5;
        public static final int COORDINATES_SECTION_SPACING = 10;
        public static final int GRID_HGAP = 10;
        public static final int GRID_VGAP = 10;
        
        // Padding
        public static final int DIALOG_PADDING = 20;
        public static final int GRID_PADDING_TOP = 20;
        public static final int GRID_PADDING_RIGHT = 20;
        public static final int GRID_PADDING_BOTTOM = 10;
        public static final int GRID_PADDING_LEFT = 20;
        
        // Animation Label Dimensions
        public static final int SUCCESS_LABEL_MIN_WIDTH = 150;
        public static final int SUCCESS_LABEL_MAX_WIDTH = 800;
        public static final int ERROR_LABEL_MIN_WIDTH = 200;
        public static final int ERROR_LABEL_MAX_WIDTH = 900;
        public static final int ANIMATION_TOP_MARGIN = 20;
        public static final int ANIMATION_SIDE_MARGIN = 10;
        
        // Font Sizes
        public static final int LABEL_FONT_SIZE = 12;
        public static final int SUCCESS_FONT_SIZE = 16;
        public static final int ERROR_FONT_SIZE = 18;
    }
    
    // ==================== ANIMATION TIMING ====================
    public static final class ANIMATIONS {
        public static final int FADE_IN_DURATION_MILLIS = 400;
        public static final int FADE_OUT_DURATION_MILLIS = 400;
        public static final int SUCCESS_ANIMATION_FADE_IN_MILLIS = 400;
        public static final int ERROR_ANIMATION_FADE_IN_MILLIS = 300;
        public static final int SUCCESS_AUTO_HIDE_SECONDS = 4;
        public static final int ERROR_AUTO_HIDE_SECONDS = 5;
        public static final int SUCCESS_CELEBRATION_AUTO_CLOSE_SECONDS = 5;
        public static final int INFO_POPUP_DELAY_SECONDS = 3;
        public static final double FADE_FROM_VALUE = 0.0;
        public static final double FADE_TO_VALUE = 1.0;
    }
    
    // ==================== DEBUG & SYSTEM PROPERTIES ====================
    public static final class DEBUG {
        public static final String DEBUG_MODE_PROPERTY = "debug.mode";
        public static final String DEBUG_MODE_SYSTEM_PROPERTY = "speleodb.debug.mode";
        public static final String DEBUG_MODE_ENV_VAR = "SPELEODB_DEBUG_MODE";
        public static final String DEBUG_MODE_DEFAULT = "false";
        public static final String COUNTRIES_LOADER_THREAD_NAME = "CountriesLoader";
        public static final String SPELEODB_WORKER_THREAD_NAME = "SpeleoDB-Worker";
    }
    
    // ==================== JSON FIELD NAMES ====================
    public static final class JSON_FIELDS {
        public static final String TOKEN = "token";
        public static final String EMAIL = "email";
        public static final String PASSWORD = "password";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String COUNTRY = "country";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String MESSAGE = "message";
        public static final String FILE_KEY = "artifact";
        public static final String DATA = "data";
        public static final String ERROR = "error";
        public static final String ID = "id";
        public static final String TITLE = "title";
        public static final String HEADER = "header";
        public static final String IS_ACTIVE = "is_active";
        public static final String SOFTWARE = "software";
        public static final String EXPIRES_AT = "expiracy_date";
        public static final String VERSION = "version";
        public static final String UUID = "uuid";
        // Plugin release fields
        public static final String PLUGIN_VERSION = "plugin_version";
        public static final String MIN_SOFTWARE_VERSION = "min_software_version";
        public static final String MAX_SOFTWARE_VERSION = "max_software_version";
        public static final String OPERATING_SYSTEM = "operating_system";
        public static final String DOWNLOAD_URL = "download_url";
        public static final String SHA256_HASH = "sha256_hash";
        public static final String CHANGELOG = "changelog";
    }
    
    // ==================== NETWORK ERROR PATTERNS ====================
    public static final class ERROR_PATTERNS {
        // Connection Error Patterns
        public static final String CONNECTION_REFUSED = "connection refused";
        public static final String NO_ROUTE_TO_HOST = "no route to host";
        public static final String HOST_UNREACHABLE = "host unreachable";
        public static final String NETWORK_UNREACHABLE = "network is unreachable";
        public static final String CONNECTION_RESET = "connection reset";
        public static final String UNKNOWN_HOST = "unknown host";
        public static final String NAME_RESOLUTION_FAILED = "name resolution failed";
        public static final String CONNECT_EXCEPTION = "connectexception";
        public static final String UNKNOWN_HOST_EXCEPTION = "unknownhostexception";
        public static final String NO_ROUTE_TO_HOST_EXCEPTION = "noroutetohostexception";
        
        // Timeout Error Patterns
        public static final String TIMED_OUT = "timed out";
        public static final String TIMEOUT = "timeout";
        public static final String READ_TIMEOUT = "read timeout";
        public static final String CONNECT_TIMEOUT = "connect timeout";
        public static final String OPERATION_TIMEOUT = "operation timeout";
        public static final String SOCKET_TIMEOUT_EXCEPTION = "sockettimeoutexception";
        public static final String HTTP_TIMEOUT_EXCEPTION = "httptimeoutexception";
        public static final String INTERRUPTED_EXCEPTION = "interruptedexception";
    }
    
    // ==================== UI ICONS & SYMBOLS ====================
    public static final class ICONS {
        public static final String SUCCESS_CHECKMARK = "✅ ";
        public static final String ERROR_X = "❌ ";
    }
    
    // ==================== DEFAULT BUTTON TYPES ====================
    public static final class BUTTON_TYPES {
        public static final ButtonType FAST_YES = new ButtonType(DIALOGS.BUTTON_YES);
        public static final ButtonType FAST_NO = new ButtonType(DIALOGS.BUTTON_NO);
        public static final ButtonType FAST_SAVE = new ButtonType(DIALOGS.BUTTON_SAVE_PROJECT, ButtonType.OK.getButtonData());
        public static final ButtonType FAST_CANCEL = new ButtonType(DIALOGS.BUTTON_CANCEL, ButtonType.CANCEL.getButtonData());
        public static final ButtonType FAST_GOT_IT = new ButtonType(DIALOGS.BUTTON_GOT_IT, ButtonType.OK.getButtonData());
    }
    
    // ==================== MISC CONSTANTS ====================
    public static final class MISC {
        public static final String EMPTY_STRING = "";
        public static final String LINE_SEPARATOR = System.lineSeparator();
        public static final String MESSAGE_INDEX_SEPARATOR = "-";
        public static final int MESSAGE_INDEX_START = 0;
        public static final int STRING_BUILDER_INITIAL_CAPACITY = 200;
        public static final String PROJECT_DATA_FORMAT = "ProjectData{name='%s', description='%s', countryCode='%s', latitude='%s', longitude='%s'}";
    }
    
    // ==================== PLUGIN CONSTANTS ====================
    public static final class PLUGIN {
        public static final String NAME = "SpeleoDB";
        public static final String TITLE = "SpeleoDB";
    }
    
    // ==================== SHUTDOWN DIALOG CONSTANTS ====================
    public static final class SHUTDOWN {
        public static final String DIALOG_TITLE = "Application Shutdown";
        public static final String DIALOG_HEADER = "Project Lock Active";
        public static final String BUTTON_YES_RELEASE_LOCK = "Yes, Release Lock";
        public static final String BUTTON_NO_KEEP_LOCK = "No, Keep Lock";
        public static final String MESSAGE_PREFIX = "You have an active lock on project \"";
        public static final String MESSAGE_SUFFIX = "\".\n\nDo you want to release the lock before closing the application?\n\n";
        public static final String MESSAGE_OPTIONS = "- Yes: Release lock (other users can edit)\n- No: Keep lock (will be released when connection times out)";
        public static final String LOG_SHUTTING_DOWN_WITH_LOCK = "Application shutting down with active lock on: ";
        public static final String LOG_LOCK_TIMEOUT_RELEASE = "Lock will be released automatically when connection times out.";
        public static final String LOG_ERROR_RELEASING_LOCK = "Error releasing lock during shutdown: ";
        public static final String LOG_EXECUTOR_NOT_TERMINATED = "SpeleoDB executor did not terminate cleanly";
    }
} 