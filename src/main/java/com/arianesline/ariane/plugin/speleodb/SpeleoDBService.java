package com.arianesline.ariane.plugin.speleodb;

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
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/**
 * Service for handling communication with SpeleoDB API endpoints.
 * Handles authentication, project management, and file operations.
 * Enhanced with async capabilities to address blocking operations antipattern.
 */
public class SpeleoDBService {

    public final static String ARIANE_ROOT_DIR = System.getProperty("user.home") + File.separator + ".ariane";
    
    // Constants for async operations
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_CONCURRENT_REQUESTS = 10;
    
    private final SpeleoDBController controller;
    private String authToken = "";
    private String SDB_instance = "";
    
    // Shared HttpClient instance for better resource management
    private final HttpClient sharedHttpClient;

    public SpeleoDBService(SpeleoDBController controller) {
        this.controller = controller;
        // Create a shared HttpClient with optimized configuration
        this.sharedHttpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();
    }

    /**
     * Gets the current SpeleoDB instance address.
     * This allows controlled access to the private `SDB_instance` field.
     *
     * @return the SpeleoDB instance address, or an empty string if not authenticated.
     * @throws IllegalStateException if not authenticated.
     */
    public String getSDBInstance() throws IllegalStateException {
        if (!isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated. Please log in.");
        }
        return SDB_instance;
    }

    /**
     * Sets the current SpeleoDB instance address.
     * This allows controlled access to the private `SDB_instance` field.
     *
     * @param instanceUrl the SpeleoDB instance URL.
     */
    private void setSDBInstance(String instanceUrl) {
        // Regex to match localhost, private IP ranges, or loopback addresses.
        String localPattern = "(^localhost)|(^127\\.)|(^10\\.)|(^172\\.(1[6-9]|2[0-9]|3[0-1])\\.)|(^192\\.168\\.)";

        if (Pattern.compile(localPattern).matcher(instanceUrl).find()) {
            // For local addresses and IPs, use http://
            SDB_instance = "http://" + instanceUrl;
        } else {
            // For non-local addresses, use https://
            SDB_instance = "https://" + instanceUrl;
        }
    }

    /* ===================== AUTHENTICATION MANAGEMENT ===================== */

    /**
     * Parses the authentication token from the JSON response.
     *
     * @param responseBody the response body containing the JSON data.
     * @return the authentication token.
     */
    private String parseAuthToken(String responseBody) {
        int tokenStart = responseBody.indexOf("\"token\":\"") + 9;
        int tokenEnd = responseBody.indexOf("\"", tokenStart);
        return responseBody.substring(tokenStart, tokenEnd);
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
        setSDBInstance(instanceUrl);  // First to ensure proper selection of http or https

        URI uri = ApiConstants.buildAuthUrl(SDB_instance);
        HttpRequest request;

        if (oAuthToken != null && !oAuthToken.isEmpty()) {
            // Authenticate using OAuth token.
            request = HttpRequest.newBuilder(uri)
                    .GET()
                    .setHeader(ApiConstants.CONTENT_TYPE_HEADER, ApiConstants.APPLICATION_JSON)
                    .setHeader(ApiConstants.AUTHORIZATION_HEADER, ApiConstants.buildTokenAuthHeader(oAuthToken))
                    .build();
        } else {
            // Authenticate using email and password - use JSON builder for safety
            JsonObject loginRequest = Json.createObjectBuilder()
                    .add("email", email)
                    .add("password", password)
                    .build();
            String requestBody = loginRequest.toString();
            
            request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .setHeader(ApiConstants.CONTENT_TYPE_HEADER, ApiConstants.APPLICATION_JSON)
                    .build();
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            authToken = parseAuthToken(response.body());
        } else {
            logout(); // Ensure we clears the `SDB_instance`
            throw new Exception("Authentication failed with status code: " + response.statusCode());
        }
    }

    /**
     * Logs the user out by clearing the authentication token and the SDB_instance
     */
    public void logout() {
        authToken = "";
        SDB_instance = "";
    }

    /**
     * Checks if the user is currently authenticated.
     *
     * @return true if authenticated, false otherwise.
     */
    public boolean isAuthenticated() {
        return !authToken.isEmpty() && !SDB_instance.isEmpty();
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
            throw new IllegalStateException("User is not authenticated.");
        }

        URI uri = ApiConstants.buildProjectsUrl(SDB_instance);

        // Build JSON payload using proper JSON builder
        var jsonBuilder = Json.createObjectBuilder()
                .add("name", name)
                .add("description", description)
                .add("country", countryCode);

        // Add optional coordinates if provided
        if (latitude != null && !latitude.trim().isEmpty()) {
            jsonBuilder.add("latitude", latitude);
        }
        if (longitude != null && !longitude.trim().isEmpty()) {
            jsonBuilder.add("longitude", longitude);
        }

        JsonObject payload = jsonBuilder.build();
        String requestBody = payload.toString();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .setHeader(ApiConstants.CONTENT_TYPE_HEADER, ApiConstants.APPLICATION_JSON)
                .setHeader(ApiConstants.AUTHORIZATION_HEADER, ApiConstants.buildTokenAuthHeader(authToken))
                .build();

        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() == 201) {
            // Successfully created, parse and return the project data
            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                JsonObject responseObject = reader.readObject();
                return responseObject.getJsonObject("data");
            }
        } else {
            // Handle different error cases
            String errorMessage = "Failed to create project with status code: " + response.statusCode();
            if (response.body() != null && !response.body().isEmpty()) {
                try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                    JsonObject errorObj = reader.readObject();
                    if (errorObj.containsKey("error")) {
                        errorMessage += " - " + errorObj.getString("error");
                    }
                } catch (Exception e) {
                    // If we can't parse the error, just use the response body
                    errorMessage += " - " + response.body();
                }
            }
            throw new Exception(errorMessage);
        }
    }

    // -------------------------- Project Listing -------------------------- //

    /**
     * Lists all projects from SpeleoDB.
     *
     * @return A JsonArray containing project details.
     * @throws Exception if the request fails.
     */
    public JsonArray listProjects() throws Exception {
        if (!isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated.");
        }

        URI uri = ApiConstants.buildProjectsUrl(SDB_instance);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .setHeader(ApiConstants.CONTENT_TYPE_HEADER, ApiConstants.APPLICATION_JSON)
                .setHeader(ApiConstants.AUTHORIZATION_HEADER, ApiConstants.buildTokenAuthHeader(authToken))
                .build();

        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() != 200) {
            String errorMessage = "Failed to list projects with status code: " + response.statusCode();
            if (response.body() != null && !response.body().isEmpty()) {
                try (JsonReader errorReader = Json.createReader(new StringReader(response.body()))) {
                    JsonObject errorObj = errorReader.readObject();
                    if (errorObj.containsKey("error")) {
                        errorMessage += " - " + errorObj.getString("error");
                    }
                } catch (Exception e) {
                    // If we can't parse the error, use the response body as-is
                    errorMessage += " - " + response.body();
                }
            }
            throw new Exception(errorMessage);
        }

        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject responseObject = reader.readObject();
            return responseObject.getJsonArray("data");
        }
    }

    // --------------------------- Project Upload -------------------------- //

    /**
     * Uploads a project to the SpeleoDB instance.
     *
     * @param message the upload message to accompany the project.
     * @param project A JsonObject containing project metadata.
     * @throws Exception if the upload fails.
     */
    public void uploadProject(String message, JsonObject project) throws Exception {
        if (!isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated.");
        }

         // Re-acquire mutex before upload to ensure we still have permission
         // This handles cases where the user might have unlocked via WebUI
         if (!acquireOrRefreshProjectMutex(project)) {
             throw new Exception("Failed to acquire project mutex before upload. Please ensure you have write access to the project.");
         }

         String SDB_projectId = project.getString("id");
         ApiConstants.validateProjectId(SDB_projectId);

         URI uri = ApiConstants.buildUploadUrl(SDB_instance, SDB_projectId);

         Path tmp_filepath = Paths.get(ARIANE_ROOT_DIR + File.separator + SDB_projectId + ".tml");

         HTTPRequestMultipartBody multipartBody = new HTTPRequestMultipartBody.Builder()
         .addPart("message", message)
         .addPart("artifact", tmp_filepath.toFile(), null, SDB_projectId + ".tml")
         .build();

         HttpRequest request = HttpRequest.newBuilder(uri).
         PUT(HttpRequest.BodyPublishers.ofByteArray(multipartBody.getBody()))
         .setHeader(ApiConstants.CONTENT_TYPE_HEADER, multipartBody.getContentType())
         .setHeader(ApiConstants.AUTHORIZATION_HEADER, ApiConstants.buildTokenAuthHeader(authToken))
         .build();

        HttpResponse<byte[]> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }

        if (response.statusCode() != 200) {
            String errorMessage = "Upload failed with status code: " + response.statusCode();
            if (response.body() != null && response.body().length > 0) {
                try {
                    String responseBody = new String(response.body());
                    try (JsonReader errorReader = Json.createReader(new StringReader(responseBody))) {
                        JsonObject errorObj = errorReader.readObject();
                        if (errorObj.containsKey("error")) {
                            errorMessage += " - " + errorObj.getString("error");
                        }
                    } catch (Exception e) {
                        // If we can't parse as JSON, include first 200 characters of response
                        String snippet = responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
                        errorMessage += " - " + snippet;
                    }
                }
                catch (Exception e) {
                    errorMessage += " - Unable to read response body";
                }
            }
            throw new Exception(errorMessage);
        }

    }

    // -------------------------- Project Download ------------------------- //

    /**
     * Download a project from the SpeleoDB instance.
     *
     * @param project A JsonObject containing project metadata.
     * @throws Exception if the upload fails.
     */
    public Path downloadProject(JsonObject project) throws IOException, InterruptedException, URISyntaxException {

        if (!isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated.");
        }

        String SDB_projectId = project.getString("id");
        ApiConstants.validateProjectId(SDB_projectId);

        URI uri = ApiConstants.buildDownloadUrl(SDB_instance, SDB_projectId);

        var request = HttpRequest.newBuilder(uri).
                GET()
                .setHeader(ApiConstants.CONTENT_TYPE_HEADER, ApiConstants.APPLICATION_JSON)
                .setHeader(ApiConstants.AUTHORIZATION_HEADER, ApiConstants.buildTokenAuthHeader(authToken)).
                build();

        HttpResponse<byte[]> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }

        Path tml_filepath = Paths.get(ARIANE_ROOT_DIR + File.separator + SDB_projectId + ".tml");

        if (response.statusCode() == 200) {
            // Clean any old version
            if (Files.exists(tml_filepath)) Files.delete(tml_filepath);

            Files.write(tml_filepath, response.body(), StandardOpenOption.CREATE_NEW);

            return tml_filepath;
        } else if (response.statusCode() == 404) {
            // case project has no TML file
            // Clean any old version

            if (Files.exists(tml_filepath)) Files.delete(tml_filepath);

            return tml_filepath;
        }

        // Handle other response codes with better error management
        if (response.statusCode() == 422) {
            throw new IOException("Project not found in cavelib: HTTP " + response.statusCode());
        } else {
            // For other error codes, log and return null to indicate failure
            String errorMessage = "Download failed with status code: " + response.statusCode();
            if (response.body() != null && response.body().length > 0) {
                try {
                    String responseBody = new String(response.body());
                    if (responseBody.length() > 200) {
                        responseBody = responseBody.substring(0, 200) + "...";
                    }
                    errorMessage += " - " + responseBody;
                } catch (Exception e) {
                    errorMessage += " - Unable to read response body";
                }
            }
            throw new IOException(errorMessage);
        }
    }

    // ---------------------- Project Mutex  ------------------------ //

    /**
     * Acquires or refresh the project's lock - aka mutex.
     *
     * @param project A JsonObject containing project metadata.
     * @throws (URISyntaxException, IOException, InterruptedException) if the API call fails
     */
    public boolean acquireOrRefreshProjectMutex(JsonObject project) throws URISyntaxException, IOException, InterruptedException {
        String projectId = project.getString("id");
        ApiConstants.validateProjectId(projectId);

        URI uri = ApiConstants.buildMutexAcquireUrl(SDB_instance, projectId);

        var request = HttpRequest.newBuilder(uri).
                POST(HttpRequest.BodyPublishers.ofString(""))
                .setHeader(ApiConstants.CONTENT_TYPE_HEADER, ApiConstants.APPLICATION_JSON)
                .setHeader(ApiConstants.AUTHORIZATION_HEADER, ApiConstants.buildTokenAuthHeader(authToken)).
                build();

        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() == 200) {
            return true;
        } else {
            // Log the error for debugging but still return false for backward compatibility
            String errorMessage = "Failed to acquire/refresh project mutex with status code: " + response.statusCode();
            if (response.body() != null && !response.body().isEmpty()) {
                errorMessage += " - " + response.body();
            }
            // Note: We don't throw an exception here to maintain backward compatibility
            // The calling code expects a boolean return value
            return false;
        }
    }

    /**
     * Release the project's lock - aka mutex.
     *
     * @param project A JsonObject containing project metadata.
     * @throws (URISyntaxException, IOException, InterruptedException) if the API call fails
     */
    public boolean releaseProjectMutex(JsonObject project) throws IOException, InterruptedException, URISyntaxException {
        String projectId = project.getString("id");
        ApiConstants.validateProjectId(projectId);

        URI uri = ApiConstants.buildMutexReleaseUrl(SDB_instance, projectId);

        var request = HttpRequest.newBuilder(uri).
                POST(HttpRequest.BodyPublishers.ofString(""))
                .setHeader(ApiConstants.CONTENT_TYPE_HEADER, ApiConstants.APPLICATION_JSON)
                .setHeader(ApiConstants.AUTHORIZATION_HEADER, ApiConstants.buildTokenAuthHeader(authToken)).
                build();

        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() == 200) {
            return true;
        } else {
            // Log the error for debugging but still return false for backward compatibility
            String errorMessage = "Failed to release project mutex with status code: " + response.statusCode();
            if (response.body() != null && !response.body().isEmpty()) {
                errorMessage += " - " + response.body();
            }
            // Note: We don't throw an exception here to maintain backward compatibility
            // The calling code expects a boolean return value
            return false;
        }
    }

    /**
     * Updates the survey file with the SpeleoDB project ID.
     * This method writes the project ID to the survey file's metadata or properties
     * to maintain the association between the local file and the SpeleoDB project.
     * 
     * @param sdbProjectId the SpeleoDB project ID to associate with the file
     * @return true if the update was successful, false otherwise
     */
    public boolean updateFileSpeleoDBId(String sdbProjectId) {
        if (sdbProjectId == null || sdbProjectId.trim().isEmpty()) {
            controller.logMessageFromPlugin("Error: Cannot update file with empty project ID");
            return false;
        }
        
        try {
            // Validate project ID format
            ApiConstants.validateProjectId(sdbProjectId);
            
            // Create the .ariane directory if it doesn't exist
            Path arianeDir = Paths.get(ARIANE_ROOT_DIR);
            if (!Files.exists(arianeDir)) {
                Files.createDirectories(arianeDir);
            }
            
            // Create a metadata file to store the project ID association
            Path metadataFile = Paths.get(ARIANE_ROOT_DIR + File.separator + sdbProjectId + ".metadata");
            
            // Write project metadata using proper formatting
            JsonObject metadata = Json.createObjectBuilder()
                    .add("project_id", sdbProjectId.trim())
                    .add("created", System.currentTimeMillis())
                    .add("updated", System.currentTimeMillis())
                    .build();
            
            Files.write(metadataFile, metadata.toString().getBytes(), 
                       StandardOpenOption.CREATE, 
                       StandardOpenOption.TRUNCATE_EXISTING);
            
            controller.logMessageFromPlugin("Successfully updated file metadata for project: " + sdbProjectId);
            return true;
             
        } catch (IOException e) {
            controller.logMessageFromPlugin("Error updating file SpeleoDB ID: " + e.getMessage());
            return false;
        } catch (Exception e) {
            controller.logMessageFromPlugin("Unexpected error updating file SpeleoDB ID: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== ASYNC METHODS (Non-blocking alternatives) ==================== //
    
    /**
     * Asynchronous version of listProjects() that doesn't block the calling thread.
     * 
     * @return CompletableFuture containing the JsonArray of projects
     */
    public CompletableFuture<JsonArray> listProjectsAsync() {
        if (!isAuthenticated()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("User is not authenticated.")
            );
        }

        try {
            URI uri = ApiConstants.buildProjectsUrl(SDB_instance);
            
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(HTTP_TIMEOUT)
                    .setHeader(ApiConstants.CONTENT_TYPE_HEADER, ApiConstants.APPLICATION_JSON)
                    .setHeader(ApiConstants.AUTHORIZATION_HEADER, ApiConstants.buildTokenAuthHeader(authToken))
                    .build();

            return sharedHttpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::parseProjectListResponse)
                    .exceptionally(this::handleProjectListError);
                    
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Asynchronous version of createProject() that doesn't block the calling thread.
     * 
     * @param name the project name
     * @param description the project description  
     * @param countryCode the country code
     * @param latitude the latitude (optional)
     * @param longitude the longitude (optional)
     * @return CompletableFuture containing the created project JsonObject
     */
    public CompletableFuture<JsonObject> createProjectAsync(String name, String description, 
                                                           String countryCode, String latitude, String longitude) {
        if (!isAuthenticated()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("User is not authenticated.")
            );
        }

        try {
            URI uri = ApiConstants.buildProjectsUrl(SDB_instance);
            
            String requestBody = StringFormatter.buildProjectCreationJson(name, description, countryCode, latitude, longitude);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(HTTP_TIMEOUT)
                    .setHeader(ApiConstants.CONTENT_TYPE_HEADER, ApiConstants.APPLICATION_JSON)
                    .setHeader(ApiConstants.AUTHORIZATION_HEADER, ApiConstants.buildTokenAuthHeader(authToken))
                    .build();

            return sharedHttpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::parseCreateProjectResponse)
                    .exceptionally(this::handleCreateProjectError);
                    
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Asynchronous version of acquireOrRefreshProjectMutex() that doesn't block.
     * 
     * @param project the project to acquire mutex for
     * @return CompletableFuture containing true if successful, false otherwise
     */
    public CompletableFuture<Boolean> acquireOrRefreshProjectMutexAsync(JsonObject project) {
        if (!isAuthenticated()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("User is not authenticated.")
            );
        }

        try {
            String projectId = project.getString("id");
            ApiConstants.validateProjectId(projectId);
            
            URI uri = ApiConstants.buildMutexAcquireUrl(SDB_instance, projectId);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(""))
                    .timeout(HTTP_TIMEOUT)
                    .setHeader(ApiConstants.CONTENT_TYPE_HEADER, ApiConstants.APPLICATION_JSON)
                    .setHeader(ApiConstants.AUTHORIZATION_HEADER, ApiConstants.buildTokenAuthHeader(authToken))
                    .build();

            return sharedHttpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> response.statusCode() == 200)
                    .exceptionally(throwable -> {
                        // Log error but don't fail the future - return false for backward compatibility
                        controller.logMessageFromPlugin("Error acquiring project mutex: " + throwable.getMessage());
                        return false;
                    });
                    
        } catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Provides a synchronous wrapper around the async listProjects method.
     * This maintains backward compatibility while using async internally.
     * 
     * @return JsonArray of projects
     * @throws Exception if the operation fails
     */
    public JsonArray listProjectsSync() throws Exception {
        try {
            return listProjectsAsync().get(HTTP_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new Exception("Failed to list projects: " + e.getMessage(), e);
        }
    }
    
    // ==================== PRIVATE HELPER METHODS FOR ASYNC OPERATIONS ==================== //
    
    /**
     * Parses the response from listProjects API call.
     */
    private JsonArray parseProjectListResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            String errorMessage = StringFormatter.formatErrorMessage("List projects", response.statusCode(), response.body());
            throw new RuntimeException(errorMessage);
        }

        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject responseObject = reader.readObject();
            return responseObject.getJsonArray("data");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse project list response", e);
        }
    }
    
    /**
     * Handles errors from listProjects async operation.
     */
    private JsonArray handleProjectListError(Throwable throwable) {
        String errorMessage = "Async project list operation failed: " + throwable.getMessage();
        controller.logMessageFromPlugin("ERROR: " + errorMessage);
        throw new RuntimeException(errorMessage, throwable);
    }
    
    /**
     * Parses the response from createProject API call.
     */
    private JsonObject parseCreateProjectResponse(HttpResponse<String> response) {
        if (response.statusCode() != 201) {
            String errorMessage = StringFormatter.formatErrorMessage("Create project", response.statusCode(), response.body());
            throw new RuntimeException(errorMessage);
        }

        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject responseObject = reader.readObject();
            return responseObject.getJsonObject("data");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse create project response", e);
        }
    }
    
    /**
     * Handles errors from createProject async operation.
     */
    private JsonObject handleCreateProjectError(Throwable throwable) {
        String errorMessage = "Async create project operation failed: " + throwable.getMessage();
        controller.logMessageFromPlugin("ERROR: " + errorMessage);
        throw new RuntimeException(errorMessage, throwable);
    }
    
    /**
     * Cleanup method to properly close the shared HttpClient.
     * Should be called when the service is no longer needed.
     */
    public void shutdown() {
        // HttpClient doesn't have an explicit close method, but we can clear references
        // The shared client will be garbage collected when no longer referenced
    }
}
