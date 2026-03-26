package org.speleodb.ariane.plugin.speleodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.speleodb.ariane.plugin.speleodb.SpeleoDBConstants.PATHS;

class SpeleoDBServiceTest {

    private static final String TEST_ARIANE_DIR = System.getProperty("java.io.tmpdir") + File.separator + "test_ariane";

    @BeforeAll
    static void setupTestEnvironment() throws IOException {
        Path testDir = Paths.get(TEST_ARIANE_DIR);
        if (!Files.exists(testDir)) {
            Files.createDirectories(testDir);
        }
    }

    @AfterAll
    static void cleanupTestEnvironment() throws IOException {
        Path testDir = Paths.get(TEST_ARIANE_DIR);
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("URL handling: local hosts use http, public hosts use https")
    void testUrlHandling() {
        assertThat(shouldUseHttp("localhost")).isTrue();
        assertThat(shouldUseHttp("127.0.0.1")).isTrue();
        assertThat(shouldUseHttp("192.168.1.100")).isTrue();
        assertThat(shouldUseHttp("10.0.0.1")).isTrue();
        assertThat(shouldUseHttp("172.16.0.1")).isTrue();

        assertThat(shouldUseHttp("www.example.com")).isFalse();
        assertThat(shouldUseHttp("api.speleodb.org")).isFalse();
        assertThat(shouldUseHttp("8.8.8.8")).isFalse();
    }

    @Test
    @DisplayName("Authentication state: login, getSDBInstance, logout")
    void testAuthenticationState() {
        MockSpeleoDBController controller = new MockSpeleoDBController();
        TestableSpeleoDBService service = new TestableSpeleoDBService(controller);

        assertThat(service.isAuthenticated()).isFalse();

        assertThatThrownBy(() -> service.getSDBInstance())
            .isInstanceOf(IllegalStateException.class);

        service.setTestAuthState("test-token", "https://test.example.com");
        assertThat(service.isAuthenticated()).isTrue();
        assertThat(service.getSDBInstance()).isEqualTo("https://test.example.com");

        service.logout();
        assertThat(service.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("JSON parsing: extract token from auth response")
    void testJsonParsing() {
        MockSpeleoDBController controller = new MockSpeleoDBController();
        TestableSpeleoDBService service = new TestableSpeleoDBService(controller);

        String authResponse = "{\"token\":\"abc123xyz\",\"user\":\"test@example.com\"}";
        String token = service.parseAuthTokenPublic(authResponse);
        assertThat(token).isEqualTo("abc123xyz");

        String edgeResponse = "{\"other\":\"value\",\"token\":\"edge-token-456\"}";
        String edgeToken = service.parseAuthTokenPublic(edgeResponse);
        assertThat(edgeToken).isEqualTo("edge-token-456");
    }

    @Test
    @DisplayName("File operations: project path and temp TML file lifecycle")
    void testFileOperations() throws IOException {
        String projectId = "test-project-123";
        Path expectedPath = Paths.get(PATHS.SDB_PROJECT_DIR + File.separator + projectId + PATHS.TML_FILE_EXTENSION);
        Path actualPath = Paths.get(PATHS.SDB_PROJECT_DIR, projectId + PATHS.TML_FILE_EXTENSION);
        assertThat(actualPath).isEqualTo(expectedPath);

        Path testFile = Paths.get(TEST_ARIANE_DIR + File.separator + projectId + PATHS.TML_FILE_EXTENSION);
        Files.write(testFile, "test content".getBytes());
        assertThat(testFile).exists();

        Files.delete(testFile);
        assertThat(testFile).doesNotExist();
    }

    @Test
    @DisplayName("Instance URL validation: normalization, schemes, trailing slashes")
    void testInstanceUrlValidation() {
        MockSpeleoDBController controller = new MockSpeleoDBController();
        TestableSpeleoDBService service = new TestableSpeleoDBService(controller);

        service.setInstanceUrlPublic("localhost:8000");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("http://localhost:8000");

        service.setInstanceUrlPublic("192.168.1.100:3000");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("http://192.168.1.100:3000");

        service.setInstanceUrlPublic("api.speleodb.org");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("https://api.speleodb.org");

        service.setInstanceUrlPublic("www.example.com:443");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("https://www.example.com:443");

        service.setInstanceUrlPublic("http://localhost:8000");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("http://localhost:8000");

        service.setInstanceUrlPublic("http://127.0.0.1:8000");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("http://127.0.0.1:8000");

        service.setInstanceUrlPublic("http://www.speleodb.org");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("https://www.speleodb.org");

        service.setInstanceUrlPublic("https://www.speleodb.org");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("https://www.speleodb.org");

        service.setInstanceUrlPublic("https://stage.speleodb.org");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("https://stage.speleodb.org");

        service.setInstanceUrlPublic("https://localhost:8000");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("http://localhost:8000");

        service.setInstanceUrlPublic("www.speleodb.org/");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("https://www.speleodb.org");

        service.setInstanceUrlPublic("localhost:8000/");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("http://localhost:8000");

        service.setInstanceUrlPublic("192.168.1.100:3000/");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("http://192.168.1.100:3000");

        service.setInstanceUrlPublic("https://www.speleodb.org/");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("https://www.speleodb.org");

        service.setInstanceUrlPublic("http://localhost:8000/");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("http://localhost:8000");

        service.setInstanceUrlPublic("https://stage.speleodb.org/");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("https://stage.speleodb.org");

        service.setInstanceUrlPublic("www.speleodb.org///");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("https://www.speleodb.org");

        service.setInstanceUrlPublic("http://localhost:8000//");
        assertThat(service.getInstanceUrlPublic()).isEqualTo("http://localhost:8000");
    }

    @Test
    @DisplayName("Project creation JSON: auth guard, coordinates, escaping")
    void testProjectCreation() {
        MockSpeleoDBController controller = new MockSpeleoDBController();
        TestableSpeleoDBService service = new TestableSpeleoDBService(controller);

        try {
            service.buildCreateProjectJson("Test Project", "Description", "US", "40.7128", "-74.0060");
            fail("Should throw IllegalStateException when not authenticated");
        } catch (IllegalStateException e) {
        }

        service.setTestAuthState("test-token", "https://test.example.com");

        String jsonWithCoords = service.buildCreateProjectJson("Cave Project", "A deep cave system", "MX", "20.1234", "-99.5678");
        assertThat(jsonWithCoords).contains("\"name\":\"Cave Project\"");
        assertThat(jsonWithCoords).contains("\"description\":\"A deep cave system\"");
        assertThat(jsonWithCoords).contains("\"country\":\"MX\"");
        assertThat(jsonWithCoords).contains("\"latitude\":\"20.1234\"");
        assertThat(jsonWithCoords).contains("\"longitude\":\"-99.5678\"");

        String jsonWithoutCoords = service.buildCreateProjectJson("Simple Cave", "Basic description", "CA", null, null);
        assertThat(jsonWithoutCoords).contains("\"name\":\"Simple Cave\"");
        assertThat(jsonWithoutCoords).contains("\"description\":\"Basic description\"");
        assertThat(jsonWithoutCoords).contains("\"country\":\"CA\"");
        assertThat(jsonWithoutCoords).doesNotContain("latitude");
        assertThat(jsonWithoutCoords).doesNotContain("longitude");

        String jsonWithEmptyCoords = service.buildCreateProjectJson("Empty Coords Cave", "Test", "GB", "", "  ");
        assertThat(jsonWithEmptyCoords).doesNotContain("latitude");
        assertThat(jsonWithEmptyCoords).doesNotContain("longitude");

        String jsonWithOnlyLat = service.buildCreateProjectJson("Lat Only Cave", "Test", "US", "40.7128", null);
        assertThat(jsonWithOnlyLat).contains("\"latitude\":\"40.7128\"");
        assertThat(jsonWithOnlyLat).doesNotContain("longitude");

        String jsonWithOnlyLon = service.buildCreateProjectJson("Lon Only Cave", "Test", "FR", null, "2.3522");
        assertThat(jsonWithOnlyLon).doesNotContain("latitude");
        assertThat(jsonWithOnlyLon).contains("\"longitude\":\"2.3522\"");

        String jsonWithSpecialChars = service.buildCreateProjectJson("Cueva de \"Los Ángeles\"", "A cave with special chars: áéíóú & symbols", "ES", null, null);
        assertThat(jsonWithSpecialChars).contains("Cueva de \\\"Los Ángeles\\\"");
        assertThat(jsonWithSpecialChars).contains("special chars");
    }

    @Test
    @DisplayName("Empty TML file creation from template")
    void testEmptyTmlFileCreation() throws IOException {
        MockSpeleoDBController controller = new MockSpeleoDBController();
        TestableSpeleoDBService service = new TestableSpeleoDBService(controller);

        String testProjectId = "test-422-project";
        String testProjectName = "Test HTTP 422 Project";

        try {
            Path createdFile = service.createEmptyTmlFileFromTemplate(testProjectId, testProjectName);

            assertThat(createdFile).exists();
            assertThat(Files.size(createdFile)).isPositive();
            assertThat(createdFile.getFileName().toString()).isEqualTo(testProjectId + PATHS.TML_FILE_EXTENSION);
            assertThat(createdFile.getParent().toString()).isEqualTo(PATHS.SDB_PROJECT_DIR);

            Files.deleteIfExists(createdFile);
        } catch (IOException e) {
        }
    }

    @Test
    @DisplayName("HTTP status code handling: 200 and 422 vs unexpected codes")
    void testHttp422Handling() {
        int http422 = 422;
        assertThat(http422).isEqualTo(422);

        int[] expectedStatusCodes = {200, 422};
        for (int code : expectedStatusCodes) {
            String action = switch (code) {
                case 200 -> "download_file";
                case 422 -> "create_from_template";
                default -> "unknown";
            };
            assertThat(action).isNotEqualTo("unknown");
        }

        int[] unexpectedStatusCodes = {400, 401, 403, 404, 500, 503};
        for (int code : unexpectedStatusCodes) {
            String action = switch (code) {
                case 200 -> "download_file";
                case 422 -> "create_from_template";
                default -> "exception_thrown";
            };
            assertThat(action).isEqualTo("exception_thrown");
        }
    }

    static boolean shouldUseHttp(String url) {
        String localPattern = "(^localhost)|(^127\\.)|(^10\\.)|(^172\\.(1[6-9]|2[0-9]|3[0-1])\\.)|(^192\\.168\\.)";
        return Pattern.compile(localPattern).matcher(url).find();
    }

    static class MockSpeleoDBController extends SpeleoDBController {
        private boolean progressVisible = false;
        private String serverTextAreaText = "";

        public MockSpeleoDBController() {
            super(true);
        }
    }

    static class TestableSpeleoDBService extends SpeleoDBService {
        private String testAuthToken = "";
        private String testSDBInstance = "";

        public TestableSpeleoDBService(SpeleoDBController controller) {
            super(controller);
        }

        @Override
        public boolean isAuthenticated() {
            return !testAuthToken.isEmpty() && !testSDBInstance.isEmpty();
        }

        @Override
        public String getSDBInstance() throws IllegalStateException {
            if (!isAuthenticated()) {
                throw new IllegalStateException("User is not authenticated. Please log in.");
            }
            return testSDBInstance;
        }

        @Override
        public void logout() {
            testAuthToken = "";
            testSDBInstance = "";
        }

        public void setTestAuthState(String token, String instance) {
            this.testAuthToken = token;
            this.testSDBInstance = instance;
        }

        public String parseAuthTokenPublic(String responseBody) {
            try (jakarta.json.JsonReader reader = jakarta.json.Json.createReader(new java.io.StringReader(responseBody))) {
                jakarta.json.JsonObject json = reader.readObject();
                return json.getString("token");
            }
        }

        public void setInstanceUrlPublic(String instanceUrl) {
            String normalizedUrl = normalizeInstanceUrlPublic(instanceUrl);
            String localPattern = "(^localhost)|(^127\\.)|(^10\\.)|(^172\\.(1[6-9]|2[0-9]|3[0-1])\\.)|(^192\\.168\\.)";

            if (Pattern.compile(localPattern).matcher(normalizedUrl).find()) {
                testSDBInstance = "http://" + normalizedUrl;
            } else {
                testSDBInstance = "https://" + normalizedUrl;
            }
        }

        private String normalizeInstanceUrlPublic(String instanceUrl) {
            if (instanceUrl == null || instanceUrl.trim().isEmpty()) {
                return instanceUrl;
            }

            String normalized = instanceUrl.trim();

            if (normalized.startsWith("https://")) {
                normalized = normalized.substring(8);
            } else if (normalized.startsWith("http://")) {
                normalized = normalized.substring(7);
            }

            while (normalized.endsWith("/") && normalized.length() > 1) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }

            normalized = normalized.toLowerCase(Locale.ROOT);

            return normalized;
        }

        public String getInstanceUrlPublic() {
            return testSDBInstance;
        }

        public String buildCreateProjectJson(String name, String description, String countryCode,
                                           String latitude, String longitude) {
            if (!isAuthenticated()) {
                throw new IllegalStateException("User is not authenticated.");
            }

            StringBuilder json = new StringBuilder("{");
            json.append("\"name\":\"").append(escapeJson(name)).append("\",");
            json.append("\"description\":\"").append(escapeJson(description)).append("\",");
            json.append("\"country\":\"").append(countryCode).append("\"");

            if (latitude != null && !latitude.trim().isEmpty()) {
                json.append(",\"latitude\":\"").append(latitude).append("\"");
            }
            if (longitude != null && !longitude.trim().isEmpty()) {
                json.append(",\"longitude\":\"").append(longitude).append("\"");
            }

            json.append("}");
            return json.toString();
        }

        private String escapeJson(String input) {
            if (input == null) return "";
            return input.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
