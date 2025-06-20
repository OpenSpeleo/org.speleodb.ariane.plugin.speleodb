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
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;


/**
 * Service class for handling SpeleoDB server communication.
 */
public class SpeleoDBService {
    public final static String ARIANE_ROOT_DIR = System.getProperty("user.home") + File.separator + ".ariane";
    private final SpeleoDBController controller;
    private String authToken = "";
    private String SDB_instance = "";

    public SpeleoDBService(SpeleoDBController controller) {
        this.controller = controller;
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
     * Creates an HTTP client with the appropriate protocol version based on the instance URL.
     * Uses HTTP/1.1 for HTTP connections (better compatibility with local servers)
     * Uses HTTP/2 for HTTPS connections (better performance for remote servers)
     * 
     * @return HttpClient configured with the appropriate protocol version
     */
    private HttpClient createHttpClient() {
        if (SDB_instance.startsWith("http://")) {
            // Use HTTP/1.1 for HTTP connections (better compatibility)
            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
        } else {
            // Use HTTP/2 for HTTPS connections (better performance)
            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .build();
        }
    }

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

        URI uri = new URI(SDB_instance + "/api/v1/user/auth-token/");
        HttpRequest request;

        if (oAuthToken != null && !oAuthToken.isEmpty()) {
            // Authenticate using OAuth token.
            request = HttpRequest.newBuilder(uri)
                    .GET()
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Authorization", "Token " + oAuthToken)
                    .build();
        } else {
            // Authenticate using email and password.
            String requestBody = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password);
            request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, java.nio.charset.StandardCharsets.UTF_8))
                    .setHeader("Content-Type", "application/json; charset=utf-8")
                    .version(HttpClient.Version.HTTP_1_1)  // Force HTTP/1.1 like curl
                    .build();
        }

        HttpClient client = createHttpClient();
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

        var uri = new URI(SDB_instance + "/api/v1/projects/");

        // Build JSON payload
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
                .setHeader("Content-Type", "application/json")
                .setHeader("Authorization", "Token " + authToken)
                .build();

        HttpResponse<String> response;
        try (HttpClient client = createHttpClient()) {
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

        var uri = new URI(SDB_instance + "/api/v1/projects/");

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .setHeader("Content-Type", "application/json")
                .setHeader("Authorization", "Token " + authToken)
                .build();

        HttpResponse<String> response;
        try (HttpClient client = createHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() != 200) {
            // TODO: Improve Error management
            throw new Exception("Failed to list projects with status code: " + response.statusCode());
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
        //TODO: FInd alternative
         if (!isAuthenticated()) {
         throw new IllegalStateException("User is not authenticated.");
         }

         // TODO: Ensure MUTEX is owned before upload - either statically but maybe preferably by calling API.
         // User could have "unlocked" the project using the WebUI while using Ariane.
         // Best way to do that is probably by just re-acquiring the mutex before upload (can be done without checking first).

         String SDB_projectId = project.getString("id");

         var uri = new URI(
         SDB_instance + "/api/v1/projects/" +
         SDB_projectId + "/upload/ariane_tml/"
         );

         Path tmp_filepath = Paths.get(ARIANE_ROOT_DIR + File.separator + SDB_projectId + ".tml");

         HTTPRequestMultipartBody multipartBody = new HTTPRequestMultipartBody.Builder()
         .addPart("message", message)
         .addPart("artifact", tmp_filepath.toFile(), null, SDB_projectId + ".tml")
         .build();

         HttpRequest request = HttpRequest.newBuilder(uri).
         PUT(HttpRequest.BodyPublishers.ofByteArray(multipartBody.getBody()))
         .setHeader("Content-Type", multipartBody.getContentType())
         .setHeader("Authorization", "Token " + authToken)
         .build();

        HttpResponse<byte[]> response;
        try (HttpClient client = createHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }

        if (response.statusCode() != 200) {
         // TODO: Improve error management
         throw new Exception("Upload failed with status code: " + response.statusCode());
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

        var uri = new URI(
                SDB_instance + "/api/v1/projects/" +
                        SDB_projectId + "/download/ariane_tml/"
        );

        var request = HttpRequest.newBuilder(uri).
                GET()
                .setHeader("Content-type", "application/json")
                .setHeader("Authorization", "token " + authToken).
                build();

        HttpResponse<byte[]> response;
        try (HttpClient client = createHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }


        Path tml_filepath = Paths.get(ARIANE_ROOT_DIR + File.separator + SDB_projectId + ".tml");

        switch (response.statusCode()) {
            case 200 -> {
                // Clean any old version
                if (Files.exists(tml_filepath)) Files.delete(tml_filepath);
                
                Files.write(tml_filepath, response.body(), StandardOpenOption.CREATE_NEW);
                
                return tml_filepath;
            }
            case 422 -> {
                // HTTP 422: Project exists but is empty - create empty TML file.
                return createEmptyTmlFileFromTemplate(SDB_projectId, project.getString("name", "Unknown Project"));
            }
            default -> {
                // Log the unexpected status code via controller if available
                String errorMessage = "Unexpected HTTP status code during project download: " + response.statusCode() + 
                                    " for project: " + project.getString("name", "Unknown Project");
                
                if (controller != null) {
                    controller.logMessageFromPlugin(errorMessage);
                }
                
                // Throw exception with detailed information
                throw new RuntimeException("Download failed with unexpected status code: " + response.statusCode() + 
                                         ". Expected: 200 (success), or 422 (project empty)");
            }
        }
    }

    /**
     * Creates an empty TML file for a project by copying the template file.
     * This method is shared between project creation and HTTP 422 download scenarios.
     * Uses the same approach as downloading TML files - simple binary file copy.
     * 
     * @param projectId the SpeleoDB project ID (used for filename)
     * @param projectName the human-readable project name (for logging only)
     * @return Path to the created TML file
     * @throws IOException if file creation fails
     */
    public Path createEmptyTmlFileFromTemplate(String projectId, String projectName) throws IOException {
        Path tmlFilePath = Paths.get(ARIANE_ROOT_DIR + File.separator + projectId + ".tml");
        
        // Ensure the .ariane directory exists
        Files.createDirectories(tmlFilePath.getParent());
        
        // Copy the template file from resources (same as download service approach)
        try (var templateStream = getClass().getResourceAsStream("/tml/empty_project.tml")) {
            if (templateStream == null) {
                throw new IOException("Template file /tml/empty_project.tml not found in resources");
            }
            
            // Simple binary copy - same approach as downloadProject()
            Files.copy(templateStream, tmlFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Log via controller if available
            if (controller != null) {
                controller.logMessageFromPlugin("Created TML file from template: " + tmlFilePath.getFileName());
            }
            
            return tmlFilePath;
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

        var uri = new URI(SDB_instance + "/api/v1/projects/" + project.getString("id") + "/acquire/");

        var request = HttpRequest.newBuilder(uri).
                POST(HttpRequest.BodyPublishers.ofString(""))
                .setHeader("Content-type", "application/json")
                .setHeader("Authorization", "Token " + authToken).
                build();


        HttpResponse<String> response;
        try (HttpClient client = createHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // TODO: Add error management
        return (response.statusCode() == 200);

    }

    /**
     * Release the project's lock - aka mutex.
     *
     * @param project A JsonObject containing project metadata.
     * @throws (URISyntaxException, IOException, InterruptedException) if the API call fails
     */
    public boolean releaseProjectMutex(JsonObject project) throws IOException, InterruptedException, URISyntaxException {

        var uri = new URI(SDB_instance + "/api/v1/projects/" + project.getString("id") + "/release/");

        var request = HttpRequest.newBuilder(uri).
                POST(HttpRequest.BodyPublishers.ofString(""))
                .setHeader("Content-type", "application/json")
                .setHeader("Authorization", "Token " + authToken).
                build();

        HttpResponse<String> response;
        try (HttpClient client = createHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // TODO: Add error management
        return (response.statusCode() == 200);
    }

    public void updateFileSpeleoDBId(String sdbProjectId) {
        //TODO: Implement method if required
    }
}
