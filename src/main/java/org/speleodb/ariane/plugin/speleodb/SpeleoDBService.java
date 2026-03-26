package org.speleodb.ariane.plugin.speleodb;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.API;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.HEADERS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.HTTP_STATUS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.JSON_FIELDS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.MESSAGES;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.NETWORK;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.PATHS;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.ProjectType;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;


/**
 * Service class for handling SpeleoDB server communication.
 * Uses the centralized SpeleoDBLogger directly.
 */
public class SpeleoDBService {
    private String authToken = "";
    private String sdbInstance = "";
    private HttpClient httpClient = null;

    // Centralized logger instance - used directly without wrapper methods
    private static final SpeleoDBLogger logger = SpeleoDBLogger.getInstance();

    public SpeleoDBService(SpeleoDBController controller) {
        // Controller parameter retained for API compatibility; not currently used by the service
    }

    /**
     * Gets the current SpeleoDB instance address.
     * This allows controlled access to the private `sdbInstance` field.
     *
     * @return the SpeleoDB instance address, or an empty string if not authenticated.
     * @throws IllegalStateException if not authenticated.
     */
    public String getSDBInstance() throws IllegalStateException {
        if (!isAuthenticated()) {
            throw new IllegalStateException(MESSAGES.USER_NOT_AUTHENTICATED);
        }
        return sdbInstance;
    }

    /**
     * Sets the current SpeleoDB instance address.
     *
     * @param instanceUrl the SpeleoDB instance URL.
     */
    private void setSDBInstance(String instanceUrl) {
        sdbInstance = resolveInstanceUrl(instanceUrl);
    }

    /**
     * Resolves a raw instance URL into a full URL with the appropriate protocol.
     * Normalizes the URL and applies http:// for local addresses, https:// for remote.
     *
     * @param rawUrl the raw instance URL from user input
     * @return full URL with protocol prefix (e.g., "https://www.speleodb.org")
     */
    private String resolveInstanceUrl(String rawUrl) {
        String normalizedUrl = normalizeInstanceUrl(rawUrl);
        if (Pattern.compile(NETWORK.LOCAL_PATTERN).matcher(normalizedUrl).find()) {
            return NETWORK.HTTP_PROTOCOL + normalizedUrl;
        }
        return NETWORK.HTTPS_PROTOCOL + normalizedUrl;
    }

    /**
     * Normalizes an instance URL by removing protocol prefixes and trailing slashes.
     * This ensures consistent processing regardless of user input format.
     *
     * Examples:
     * - "https://www.speleodb.org/" -> "www.speleodb.org"
     * - "http://localhost:8000/" -> "localhost:8000"
     * - "stage.speleodb.org" -> "stage.speleodb.org"
     *
     * @param instanceUrl the raw instance URL from user input
     * @return normalized URL without protocol prefix or trailing slashes
     */
    private String normalizeInstanceUrl(String instanceUrl) {
        if (instanceUrl == null || instanceUrl.trim().isEmpty()) {
            return instanceUrl;
        }

        String normalized = instanceUrl.trim();

        // Remove http:// or https:// protocol prefixes
        if (normalized.startsWith(NETWORK.HTTPS_PROTOCOL)) {
            normalized = normalized.substring(NETWORK.HTTPS_PROTOCOL.length());
        } else if (normalized.startsWith(NETWORK.HTTP_PROTOCOL)) {
            normalized = normalized.substring(NETWORK.HTTP_PROTOCOL.length());
        }

        // Remove trailing slashes
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        // RFC 2616/7230: hostnames are case-insensitive; normalize to lowercase
        normalized = normalized.toLowerCase(Locale.ROOT);

        return normalized;
    }

    /* ===================== AUTHENTICATION MANAGEMENT ===================== */

    /**
     * Creates an HTTP client with the appropriate protocol version and reliability settings
     * based on the given instance URL.
     *
     * @param instanceUrl the full instance URL (including protocol) to determine HTTP version
     * @return HttpClient configured with appropriate HTTP version and reliability settings
     */
    private HttpClient createHttpClientForInstance(String instanceUrl) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(NETWORK.CONNECT_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (instanceUrl.startsWith(NETWORK.HTTP_PROTOCOL)) {
            builder = builder.version(HttpClient.Version.HTTP_1_1);
        } else {
            builder = builder.version(HttpClient.Version.HTTP_2);
        }

        return builder.build();
    }

    /**
     * Creates an HTTP client using the current authenticated sdbInstance.
     *
     * @return HttpClient configured for the current sdbInstance
     */
    private HttpClient createHttpClient() {
        return createHttpClientForInstance(sdbInstance);
    }

    /**
     * Parses the authentication token from the JSON response.
     *
     * @param responseBody the response body containing the JSON data.
     * @return the authentication token.
     * @throws IllegalArgumentException if the response does not contain a valid token.
     */
    private String parseAuthToken(String responseBody) {
        try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
            JsonObject json = reader.readObject();
            if (!json.containsKey(JSON_FIELDS.TOKEN)) {
                throw new IllegalArgumentException("Response does not contain a token field");
            }
            return json.getString(JSON_FIELDS.TOKEN);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse auth token from response: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticates the user with SpeleoDB using either OAuth token or email and password.
     *
     * @param email       the user's email address.
     * @param password    the user's password.
     * @param oAuthToken  the OAuth token for authentication.
     * @param instanceUrl the SpeleoDB instance URL.
     * @throws Exception if authentication fails.
     */
    public void authenticate(String email, String password, String oAuthToken, String instanceUrl) throws Exception {
        setSDBInstance(instanceUrl);
        httpClient = createHttpClient(); // Create the HTTP client after setting the instance URL

        URI uri = new URI(sdbInstance + API.AUTH_TOKEN_ENDPOINT);
        HttpRequest request;

        if (oAuthToken != null && !oAuthToken.isEmpty()) {
            // Authenticate using OAuth token.
            request = HttpRequest.newBuilder(uri)
                    .GET()
                    .setHeader(HEADERS.CONTENT_TYPE, HEADERS.APPLICATION_JSON)
                    .setHeader(HEADERS.AUTHORIZATION, HEADERS.TOKEN_PREFIX + oAuthToken)
                    .timeout(Duration.ofSeconds(NETWORK.REQUEST_TIMEOUT_SECONDS))  // Add request timeout
                    .build();
        } else {
            // Authenticate using email and password.
            var jsonBuilder = Json.createObjectBuilder()
                .add(JSON_FIELDS.EMAIL, email)
                .add(JSON_FIELDS.PASSWORD, password);

            String requestBody = jsonBuilder.build().toString();

            request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, java.nio.charset.StandardCharsets.UTF_8))
                    .setHeader(HEADERS.CONTENT_TYPE, HEADERS.APPLICATION_JSON_UTF8)
                    .timeout(Duration.ofSeconds(NETWORK.REQUEST_TIMEOUT_SECONDS))  // Add request timeout
                    .build();
        }

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == HTTP_STATUS.OK) {
            authToken = parseAuthToken(response.body());
        } else {
            logout(); // Ensure we clears the `sdbInstance`
            throw new Exception(MESSAGES.AUTH_FAILED_STATUS + response.statusCode());
        }
    }

    /**
     * Logs the user out by clearing the authentication token and the sdbInstance
     */
    public void logout() {
        authToken = "";
        sdbInstance = "";
        httpClient = null;  // Clear cached HTTP client on logout
    }

    /**
     * Checks if the user is currently authenticated.
     *
     * @return true if authenticated, false otherwise.
     */
    public boolean isAuthenticated() {
        return !authToken.isEmpty() && !sdbInstance.isEmpty();
    }

    /* ========================= PROJECT MANAGEMENT ======================== */

    // -------------------------- Project Creation ------------------------- //

    /**
     * Creates a new project on SpeleoDB.
     *
     * @param name        the project name.
     * @param description the project description.
     * @param countryCode the ISO country code.
     * @param latitude    the latitude (optional, can be null).
     * @param longitude   the longitude (optional, can be null).
     * @return A JsonObject containing the created project details.
     * @throws Exception if the request fails.
     */
    public JsonObject createProject(String name, String description, String countryCode,
                                   String latitude, String longitude) throws Exception {
        if (!isAuthenticated()) {
            throw new IllegalStateException(MESSAGES.USER_NOT_AUTHENTICATED_SHORT);
        }

        var uri = new URI(sdbInstance + API.PROJECTS_ENDPOINT);

        // Build JSON payload
        var jsonBuilder = Json.createObjectBuilder()
                .add(JSON_FIELDS.NAME, name)
                .add(JSON_FIELDS.DESCRIPTION, description)
                .add(JSON_FIELDS.COUNTRY, countryCode);

        // Add optional coordinates if provided
        if (latitude != null && !latitude.trim().isEmpty()) {
            jsonBuilder.add(JSON_FIELDS.LATITUDE, latitude);
        }
        if (longitude != null && !longitude.trim().isEmpty()) {
            jsonBuilder.add(JSON_FIELDS.LONGITUDE, longitude);
        }

        jsonBuilder.add(JSON_FIELDS.PROJECT_TYPE, ProjectType.ARIANE.name());

        String requestBody = jsonBuilder.build().toString();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .setHeader(HEADERS.CONTENT_TYPE, HEADERS.APPLICATION_JSON)
                .setHeader(HEADERS.AUTHORIZATION, HEADERS.TOKEN_PREFIX + authToken)
                .timeout(Duration.ofSeconds(NETWORK.REQUEST_TIMEOUT_SECONDS))  // Add request timeout
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == HTTP_STATUS.CREATED) {
            // Successfully created, parse and return the project data
            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject responseObject = reader.readObject();
                return responseObject.getJsonObject(JSON_FIELDS.DATA);
            }
        } else {
            // Handle different error cases
            String errorMessage = MESSAGES.PROJECT_CREATE_FAILED_STATUS + response.statusCode();
            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject errorObj = reader.readObject();
                if (errorObj.containsKey(JSON_FIELDS.ERROR)) {
                    errorMessage += " - " + errorObj.getString(JSON_FIELDS.ERROR);
                }
            } catch (Exception parseException) {
                // If we can't parse the error, just use the response body
                errorMessage += " - " + response.body();
            }
            throw new Exception(errorMessage);
        }
    }

    // -------------------------- Project Listing -------------------------- //

    /**
     * Lists all projects accessible to the authenticated user.
     *
     * @return A JsonArray containing project details.
     * @throws Exception if the request fails.
     */
    public JsonArray listProjects() throws Exception {
        if (!isAuthenticated()) {
            throw new IllegalStateException(MESSAGES.USER_NOT_AUTHENTICATED_SHORT);
        }

        var uri = new URI(sdbInstance + API.PROJECTS_ENDPOINT);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .setHeader(HEADERS.CONTENT_TYPE, HEADERS.APPLICATION_JSON)
                .setHeader(HEADERS.AUTHORIZATION, HEADERS.TOKEN_PREFIX + authToken)
                .timeout(Duration.ofSeconds(NETWORK.REQUEST_TIMEOUT_SECONDS))  // Add request timeout
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != HTTP_STATUS.OK) {
            throw new Exception(MESSAGES.PROJECT_LIST_FAILED_STATUS + response.statusCode());
        }

        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject responseObject = reader.readObject();
            JsonArray projects = responseObject.getJsonArray(JSON_FIELDS.DATA);

            return collectToJsonArray(projects.stream()
                    .filter(JsonObject.class::isInstance)
                    .map(JsonObject.class::cast)
                    .filter(project -> ProjectType.ARIANE.name().equals(project.getString(JSON_FIELDS.PROJECT_TYPE, "")))
                    .filter(project -> !JSON_FIELDS.PERMISSION_WEB_VIEWER.equals(project.getString(JSON_FIELDS.PERMISSION, ""))));
        }
    }

    // -------------------------- Project Upload --------------------------- //

    /**
     * Uploads a project to SpeleoDB.
     *
     * @param message the commit message for the upload.
     * @param project the project data containing the project ID.
     * @throws Exception if the upload fails.
     */
    public void uploadProject(String message, JsonObject project) throws Exception {
        if (!isAuthenticated()) {
            throw new IllegalStateException(MESSAGES.USER_NOT_AUTHENTICATED_SHORT);
        }

        String sanitizedMessage = (message != null) ? message.strip() : "";
        if (sanitizedMessage.isEmpty()) {
            throw new IllegalArgumentException("Upload message cannot be empty");
        }

        // TODO: Ensure MUTEX is owned before upload - either statically but maybe preferably by calling API.
        String sdbProjectId = project.getString(JSON_FIELDS.ID);
        Path tmpFilepath = Paths.get(PATHS.SDB_PROJECT_DIR + File.separator + sdbProjectId + PATHS.TML_FILE_EXTENSION);

        // Check if the TML file is the same as the empty project template
        if (Files.exists(tmpFilepath)) {
            try {
                byte[] fileData = Files.readAllBytes(tmpFilepath);
                String fileHash = calculateSHA256(fileData);

                String emptyTemplateHash = getEmptyTemplateSHA256();
                if (emptyTemplateHash != null && emptyTemplateHash.equals(fileHash)) {
                    throw new IllegalArgumentException(MESSAGES.PROJECT_UPLOAD_REJECTED_EMPTY);
                }
            } catch (IOException e) {
                logger.warn("Warning: Could not verify project file hash: " + e.getMessage());
                // Continue with upload if file reading fails - let server handle validation
            }
        }

        URI uri = new URI(
            sdbInstance + API.PROJECTS_ENDPOINT +
            sdbProjectId + API.UPLOAD_ARIANE_TML_PATH
        );

        HTTPRequestMultipartBody multipartBody = new HTTPRequestMultipartBody.Builder()
                .addPart(JSON_FIELDS.MESSAGE, sanitizedMessage)
                .addPart(JSON_FIELDS.FILE_KEY, tmpFilepath.toFile(), null, sdbProjectId + PATHS.TML_FILE_EXTENSION)
                .build();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(multipartBody.getBody()))
                .setHeader(HEADERS.CONTENT_TYPE, multipartBody.getContentType())
                .setHeader(HEADERS.AUTHORIZATION, HEADERS.TOKEN_PREFIX + authToken)
                .timeout(Duration.ofSeconds(NETWORK.REQUEST_TIMEOUT_SECONDS))  // Add request timeout
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        int status = response.statusCode();
        if (status == HTTP_STATUS.OK) {
            return;
        } else if (status == HTTP_STATUS.NOT_MODIFIED) {
            throw new NotModifiedException(MESSAGES.PROJECT_UPLOAD_NOT_MODIFIED);
        } else {
            throw new Exception(MESSAGES.PROJECT_UPLOAD_FAILED_STATUS + status);
        }
        // TODO: Improve error management
    }

    // -------------------------- Project Download ------------------------- //

    /**
     * Downloads a project from SpeleoDB and saves it locally.
     *
     * @param project the project data containing the project ID.
     * @return the Path to the downloaded project file.
     * @throws IOException              if file operations fail.
     * @throws InterruptedException     if the request is interrupted.
     * @throws URISyntaxException       if the URI is malformed.
     */
    public Path downloadProject(JsonObject project) throws IOException, InterruptedException, URISyntaxException {
        if (!isAuthenticated()) {
            throw new IllegalStateException(MESSAGES.USER_NOT_AUTHENTICATED_SHORT);
        }

        String sdbProjectId = project.getString(JSON_FIELDS.ID);

        URI uri = new URI(
                sdbInstance + API.PROJECTS_ENDPOINT +
                sdbProjectId + API.DOWNLOAD_ARIANE_TML_PATH
        );

        var request = HttpRequest.newBuilder(uri)
                .GET()
                .setHeader(HEADERS.CONTENT_TYPE, HEADERS.APPLICATION_JSON)
                .setHeader(HEADERS.AUTHORIZATION, HEADERS.TOKEN_PREFIX + authToken)
                .timeout(Duration.ofSeconds(NETWORK.DOWNLOAD_TIMEOUT_SECONDS))  // Longer timeout for downloads
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        Path tmlFilepath = Paths.get(PATHS.SDB_PROJECT_DIR + File.separator + sdbProjectId + PATHS.TML_FILE_EXTENSION);

        switch (response.statusCode()) {
            case HTTP_STATUS.OK -> {
                // Successful download - save the file
                Files.write(tmlFilepath, response.body(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return tmlFilepath;
            }
            case HTTP_STATUS.UNPROCESSABLE_ENTITY -> {
                // HTTP 422: Project exists but is empty - create empty TML file.
                logger.info(MESSAGES.PROJECT_DOWNLOAD_404_EMPTY);
                return createEmptyTmlFileFromTemplate(sdbProjectId, project.getString(JSON_FIELDS.NAME, "Unknown Project"));
            }
            default -> {
                String errorMessage = "Unexpected HTTP status code during project download: " + response.statusCode() +
                        " for project: " + project.getString(JSON_FIELDS.NAME, "Unknown Project");

                logger.info(errorMessage);

                throw new RuntimeException(MESSAGES.PROJECT_DOWNLOAD_FAILED_STATUS + response.statusCode() +
                        MESSAGES.PROJECT_DOWNLOAD_UNEXPECTED_STATUS);
            }
        }
    }

    /**
     * Creates an empty TML file from a template for projects that don't have content yet.
     * This method is shared between project creation and HTTP 422 download scenarios.
     *
     * @param projectId   the SpeleoDB project ID
     * @param projectName the human-readable project name
     * @return Path to the created TML file
     * @throws IOException if file creation fails
     */
    public Path createEmptyTmlFileFromTemplate(String projectId, String projectName) throws IOException {
        Path tmlFilePath = Paths.get(PATHS.SDB_PROJECT_DIR + File.separator + projectId + PATHS.TML_FILE_EXTENSION);

        // Copy template from resources to target location
        try (var templateStream = getClass().getResourceAsStream(PATHS.EMPTY_TML)) {
            if (templateStream == null) {
                throw new IOException("Template file `" + PATHS.EMPTY_TML + "` not found in resources");
            }

            // Create parent directories if they don't exist
            Files.createDirectories(tmlFilePath.getParent());

            // Copy template to destination
            Files.copy(templateStream, tmlFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        logger.info("Created TML file from template: " + tmlFilePath.getFileName());
        return tmlFilePath;
    }

    // -------------------------- Project Locking -------------------------- //

    /**
     * Acquires or refreshes a mutex lock on a project.
     *
     * @param project the project to lock.
     * @return true if the lock was acquired, false otherwise.
     * @throws URISyntaxException       if the URI is malformed.
     * @throws IOException              if the request fails.
     * @throws InterruptedException     if the request is interrupted.
     */
    public boolean acquireOrRefreshProjectMutex(JsonObject project) throws URISyntaxException, IOException, InterruptedException {
        var uri = new URI(sdbInstance + API.PROJECTS_ENDPOINT + project.getString(JSON_FIELDS.ID) + API.ACQUIRE_LOCK_PATH);

        var request = HttpRequest.newBuilder(uri).
                POST(HttpRequest.BodyPublishers.ofString(""))
                .setHeader(HEADERS.CONTENT_TYPE, HEADERS.APPLICATION_JSON)
                .setHeader(HEADERS.AUTHORIZATION, HEADERS.TOKEN_PREFIX + authToken)
                .timeout(Duration.ofSeconds(NETWORK.REQUEST_TIMEOUT_SECONDS))  // Add request timeout
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // TODO: Add error management
        return (response.statusCode() == HTTP_STATUS.OK);
    }

    /**
     * Releases a mutex lock on a project.
     *
     * @param project the project to unlock.
     * @return true if the lock was released, false otherwise.
     * @throws IOException              if the request fails.
     * @throws InterruptedException     if the request is interrupted.
     * @throws URISyntaxException       if the URI is malformed.
     */
    public boolean releaseProjectMutex(JsonObject project) throws IOException, InterruptedException, URISyntaxException {
        var uri = new URI(sdbInstance + API.PROJECTS_ENDPOINT + project.getString(JSON_FIELDS.ID) + API.RELEASE_LOCK_PATH);

        var request = HttpRequest.newBuilder(uri).
                POST(HttpRequest.BodyPublishers.ofString(""))
                .setHeader(HEADERS.CONTENT_TYPE, HEADERS.APPLICATION_JSON)
                .setHeader(HEADERS.AUTHORIZATION, HEADERS.TOKEN_PREFIX + authToken)
                .timeout(Duration.ofSeconds(NETWORK.REQUEST_TIMEOUT_SECONDS))  // Add request timeout
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // TODO: Add error management
        return (response.statusCode() == HTTP_STATUS.OK);
    }

    /* ========================= PUBLIC ANNOUNCEMENTS ======================== */

    /**
     * Fetches announcements from SpeleoDB for the ARIANE software.
     * This endpoint does not require authentication.
     *
     * @param instanceUrl the SpeleoDB instance URL to fetch announcements from
     * @return A JsonArray containing active announcement details for ARIANE
     * @throws Exception if the request fails
     */
    public JsonArray fetchAnnouncements(String instanceUrl) throws Exception {
        String tempInstance = resolveInstanceUrl(instanceUrl);

        URI uri = new URI(tempInstance + API.ANNOUNCEMENTS_ENDPOINT);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .setHeader(HEADERS.CONTENT_TYPE, HEADERS.APPLICATION_JSON)
                .timeout(Duration.ofSeconds(NETWORK.REQUEST_TIMEOUT_SECONDS))
                .build();

        HttpResponse<String> response = createHttpClientForInstance(tempInstance).send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != HTTP_STATUS.OK) {
            throw new Exception("Failed to fetch announcements with status code: " + response.statusCode());
        }

        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject responseObject = reader.readObject();
            JsonArray announcements = responseObject.getJsonArray(JSON_FIELDS.DATA);

            // Note: We'll get current date inside the filter for date comparison

            return collectToJsonArray(announcements.stream()
                    .filter(JsonObject.class::isInstance)
                    .map(JsonObject.class::cast)
                    .filter(announcement -> announcement.getBoolean(JSON_FIELDS.IS_ACTIVE, false))
                    .filter(announcement -> "ARIANE".equals(announcement.getString(JSON_FIELDS.SOFTWARE, "")))
                    .filter(announcement -> {
                        String expiresAt = announcement.getString(JSON_FIELDS.EXPIRES_AT, null);
                        if (expiresAt != null && !expiresAt.isEmpty()) {
                            try {
                                java.time.LocalDate expiryDate = java.time.LocalDate.parse(expiresAt);
                                java.time.LocalDate today = java.time.LocalDate.now();
                                return !expiryDate.isBefore(today);
                            } catch (java.time.format.DateTimeParseException e) {
                                logger.warn("Failed to parse expires_at field: `" + expiresAt + "`.");
                                return false;
                            }
                        }
                        return true;
                    })
                    .filter(announcement -> {
                        String announcementVersion = announcement.getString(JSON_FIELDS.VERSION, null);
                        if (announcementVersion != null && !announcementVersion.isEmpty()) {
                            return announcementVersion.equals(SpeleoDBConstants.VERSION);
                        }
                        return true;
                    }));
        }
    }

    /* ========================= PLUGIN UPDATES ======================== */

    /**
     * Fetches plugin releases from SpeleoDB for the ARIANE software.
     * This endpoint does not require authentication.
     *
     * @param instanceUrl the SpeleoDB instance URL to fetch releases from
     * @return A JsonArray containing plugin release details for ARIANE
     * @throws Exception if the request fails
     */
    public JsonArray fetchPluginReleases(String instanceUrl) throws Exception {
        String tempInstance = resolveInstanceUrl(instanceUrl);

        URI uri = new URI(tempInstance + API.PLUGIN_RELEASES_ENDPOINT);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .setHeader(HEADERS.CONTENT_TYPE, HEADERS.APPLICATION_JSON)
                .timeout(Duration.ofSeconds(NETWORK.REQUEST_TIMEOUT_SECONDS))
                .build();

        HttpResponse<String> response = createHttpClientForInstance(tempInstance).send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != HTTP_STATUS.OK) {
            throw new Exception("Failed to fetch plugin releases with status code: " + response.statusCode());
        }

        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject responseObject = reader.readObject();
            JsonArray releases = responseObject.getJsonArray(JSON_FIELDS.DATA);

            return collectToJsonArray(releases.stream()
                    .filter(JsonObject.class::isInstance)
                    .map(JsonObject.class::cast)
                    .filter(release -> SpeleoDBConstants.ARIANE_SOFTWARE_NAME.equals(release.getString(JSON_FIELDS.SOFTWARE, "")))
                    .filter(release -> {
                        String minVersion = release.getString(JSON_FIELDS.MIN_SOFTWARE_VERSION, null);
                        String maxVersion = release.getString(JSON_FIELDS.MAX_SOFTWARE_VERSION, null);
                        String currentVersion = SpeleoDBConstants.ARIANE_VERSION;

                        if (currentVersion.isEmpty()) {
                            return false;
                        }
                        if (minVersion == null && maxVersion == null) {
                            return true;
                        }
                        if (minVersion != null && SpeleoDBController.compareVersions(currentVersion, minVersion) < 0) {
                            return false;
                        }
                        return !(maxVersion != null && SpeleoDBController.compareVersions(currentVersion, maxVersion) > 0);
                    }));
        }
    }

    /* ========================= UTILITIES ======================== */

    /**
     * Collects a stream of JsonObjects into a JsonArray (sequential only).
     *
     * @param stream the stream of JsonObject to collect
     * @return a JsonArray built from the stream elements
     */
    private static JsonArray collectToJsonArray(Stream<JsonObject> stream) {
        return stream.collect(
                Json::createArrayBuilder,
                (builder, item) -> builder.add(item),
                (b1, b2) -> { throw new UnsupportedOperationException("Parallel processing not supported"); }
        ).build();
    }

    /**
     * Calculates the SHA256 hash of the given file data.
     * Package-private to allow reuse in tests.
     *
     * @param fileData the file data to hash
     * @return the SHA256 hash as a lowercase hex string
     * @throws RuntimeException if SHA-256 algorithm is not available
     */
    static String calculateSHA256(byte[] fileData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
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

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Gets the SHA256 hash of the empty project template from resources.
     *
     * @return the SHA256 hash of the empty template, or null if it cannot be calculated
     */
    private String getEmptyTemplateSHA256() {
        try (var templateStream = getClass().getResourceAsStream(PATHS.EMPTY_TML)) {
            if (templateStream == null) {
                logger.warn("Empty template file not found in resources: " + PATHS.EMPTY_TML);
                return null;
            }

            byte[] templateData = templateStream.readAllBytes();
            return calculateSHA256(templateData);
        } catch (IOException e) {
            logger.warn("Error reading empty template file for hash calculation: " + e.getMessage());
            return null;
        }
    }
}
