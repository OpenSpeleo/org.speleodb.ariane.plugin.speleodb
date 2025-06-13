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
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .setHeader("Content-Type", "application/json")
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
        try (HttpClient client = HttpClient.newHttpClient()) {
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
        try (HttpClient client = HttpClient.newHttpClient()) {
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

        // TODO: Add missing response code management

        return null;
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
        try (HttpClient client = HttpClient.newHttpClient()) {
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
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // TODO: Add error management
        return (response.statusCode() == 200);
    }

    public void updateFileSpeleoDBId(String sdbProjectId) {
        //TODO: Implement method if required
    }
}
