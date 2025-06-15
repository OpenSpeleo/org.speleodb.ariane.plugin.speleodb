package com.arianesline.ariane.plugin.speleodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * Comprehensive unit tests for value objects introduced to address primitive obsession.
 * Tests AuthToken, ProjectId, InstanceUrl, and ProjectCreationRequest classes.
 */
@DisplayName("Value Objects Tests")
class ValueObjectsTest {

    @Nested
    @DisplayName("AuthToken Tests")
    class AuthTokenTests {
        
        @Test
        @DisplayName("Should create valid AuthToken from non-empty string")
        void shouldCreateValidAuthToken() {
            AuthToken token = AuthToken.of("abc123");
            
            assertThat(token.getValue()).isEqualTo("abc123");
            assertThat(token.isValid()).isTrue();
            assertThat(token.isEmpty()).isFalse();
        }
        
        @Test
        @DisplayName("Should trim whitespace when creating AuthToken")
        void shouldTrimWhitespace() {
            AuthToken token = AuthToken.of("  abc123  ");
            
            assertThat(token.getValue()).isEqualTo("abc123");
            assertThat(token.isValid()).isTrue();
        }
        
        @Test
        @DisplayName("Should create empty AuthToken")
        void shouldCreateEmptyAuthToken() {
            AuthToken token = AuthToken.empty();
            
            assertThat(token.getValue()).isEmpty();
            assertThat(token.isValid()).isFalse();
            assertThat(token.isEmpty()).isTrue();
        }
        
        @Test
        @DisplayName("Should reject null token")
        void shouldRejectNullToken() {
            assertThatThrownBy(() -> AuthToken.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Auth token cannot be empty");
        }
        
        @Test
        @DisplayName("Should reject empty token")
        void shouldRejectEmptyToken() {
            assertThatThrownBy(() -> AuthToken.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Auth token cannot be empty");
                
            assertThatThrownBy(() -> AuthToken.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Auth token cannot be empty");
        }
        
        @Test
        @DisplayName("Should implement equality correctly")
        void shouldImplementEquality() {
            AuthToken token1 = AuthToken.of("abc123");
            AuthToken token2 = AuthToken.of("abc123");
            AuthToken token3 = AuthToken.of("def456");
            
            assertThat(token1).isEqualTo(token2);
            assertThat(token1).isNotEqualTo(token3);
            assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
        }
        
        @Test
        @DisplayName("Should not expose token value in toString")
        void shouldNotExposeTokenInToString() {
            AuthToken token = AuthToken.of("secret123");
            String toString = token.toString();
            
            assertThat(toString).doesNotContain("secret123");
            assertThat(toString).contains("***");
        }
    }

    @Nested
    @DisplayName("ProjectId Tests")
    class ProjectIdTests {
        
        @Test
        @DisplayName("Should create valid ProjectId from string")
        void shouldCreateValidProjectId() {
            ProjectId projectId = ProjectId.of("project-123");
            
            assertThat(projectId.getValue()).isEqualTo("project-123");
            assertThat(projectId.isValid()).isTrue();
        }
        
        @Test
        @DisplayName("Should create ProjectId from JsonObject")
        void shouldCreateFromJsonObject() {
            JsonObject project = Json.createObjectBuilder()
                .add("id", "project-456")
                .add("name", "Test Project")
                .build();
                
            ProjectId projectId = ProjectId.fromJson(project);
            
            assertThat(projectId.getValue()).isEqualTo("project-456");
        }
        
        @Test
        @DisplayName("Should reject null JsonObject")
        void shouldRejectNullJsonObject() {
            assertThatThrownBy(() -> ProjectId.fromJson(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project JsonObject cannot be null");
        }
        
        @Test
        @DisplayName("Should reject JsonObject without id field")
        void shouldRejectJsonObjectWithoutId() {
            JsonObject project = Json.createObjectBuilder()
                .add("name", "Test Project")
                .build();
                
            assertThatThrownBy(() -> ProjectId.fromJson(project))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project JsonObject must contain 'id' field");
        }
        
        @Test
        @DisplayName("Should trim whitespace")
        void shouldTrimWhitespace() {
            ProjectId projectId = ProjectId.of("  project-123  ");
            
            assertThat(projectId.getValue()).isEqualTo("project-123");
        }
        
        @Test
        @DisplayName("Should reject empty project ID")
        void shouldRejectEmptyProjectId() {
            assertThatThrownBy(() -> ProjectId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID cannot be empty");
        }
    }

    @Nested
    @DisplayName("InstanceUrl Tests")
    class InstanceUrlTests {
        
        @Test
        @DisplayName("Should create InstanceUrl with HTTPS for public domains")
        void shouldCreateWithHttpsForPublicDomains() {
            InstanceUrl instanceUrl = InstanceUrl.of("www.speleodb.org");
            
            assertThat(instanceUrl.getUrl()).startsWith("https://");
            assertThat(instanceUrl.getHost()).isEqualTo("www.speleodb.org");
            assertThat(instanceUrl.isSecure()).isTrue();
            assertThat(instanceUrl.isLocal()).isFalse();
        }
        
        @Test
        @DisplayName("Should create InstanceUrl with HTTP for localhost")
        void shouldCreateWithHttpForLocalhost() {
            InstanceUrl instanceUrl = InstanceUrl.of("localhost:8080");
            
            assertThat(instanceUrl.getUrl()).startsWith("http://");
            assertThat(instanceUrl.getHost()).isEqualTo("localhost");
            assertThat(instanceUrl.isSecure()).isFalse();
            assertThat(instanceUrl.isLocal()).isTrue();
        }
        
        @Test
        @DisplayName("Should create InstanceUrl with HTTP for private IPs")
        void shouldCreateWithHttpForPrivateIps() {
            InstanceUrl instanceUrl = InstanceUrl.of("192.168.1.100:3000");
            
            assertThat(instanceUrl.getUrl()).startsWith("http://");
            assertThat(instanceUrl.getHost()).isEqualTo("192.168.1.100");
            assertThat(instanceUrl.isSecure()).isFalse();
            assertThat(instanceUrl.isLocal()).isTrue();
        }
        
        @Test
        @DisplayName("Should preserve existing protocol")
        void shouldPreserveExistingProtocol() {
            InstanceUrl httpsUrl = InstanceUrl.of("https://example.com");
            InstanceUrl httpUrl = InstanceUrl.of("http://example.com");
            
            assertThat(httpsUrl.getUrl()).isEqualTo("https://example.com");
            assertThat(httpUrl.getUrl()).isEqualTo("http://example.com");
        }
        
        @Test
        @DisplayName("Should create default instance")
        void shouldCreateDefaultInstance() {
            InstanceUrl defaultInstance = InstanceUrl.defaultInstance();
            
            assertThat(defaultInstance.getHost()).isEqualTo("www.speleoDB.org");
            assertThat(defaultInstance.isSecure()).isTrue();
        }
        
        @Test
        @DisplayName("Should reject empty URL")
        void shouldRejectEmptyUrl() {
            assertThatThrownBy(() -> InstanceUrl.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Instance URL cannot be empty");
        }
        
        @Test
        @DisplayName("Should reject invalid URL")
        void shouldRejectInvalidUrl() {
            assertThatThrownBy(() -> InstanceUrl.of("invalid url with spaces"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid URL");
        }
    }

    @Nested
    @DisplayName("ProjectCreationRequest Tests")
    class ProjectCreationRequestTests {
        
        @Test
        @DisplayName("Should create request with required fields only")
        void shouldCreateWithRequiredFields() {
            ProjectCreationRequest request = ProjectCreationRequest.of(
                "Test Project", 
                "A test project", 
                "US"
            );
            
            assertThat(request.getName()).isEqualTo("Test Project");
            assertThat(request.getDescription()).isEqualTo("A test project");
            assertThat(request.getCountryCode()).isEqualTo("US");
            assertThat(request.getLatitude()).isNull();
            assertThat(request.getLongitude()).isNull();
            assertThat(request.hasCoordinates()).isFalse();
        }
        
        @Test
        @DisplayName("Should create request with coordinates using builder")
        void shouldCreateWithCoordinatesUsingBuilder() {
            ProjectCreationRequest request = ProjectCreationRequest.builder()
                .withName("Cave Project")
                .withDescription("Underground exploration")
                .withCountry("FR")
                .withCoordinates("43.123", "2.456")
                .build();
            
            assertThat(request.getName()).isEqualTo("Cave Project");
            assertThat(request.getLatitude()).isEqualTo("43.123");
            assertThat(request.getLongitude()).isEqualTo("2.456");
            assertThat(request.hasCoordinates()).isTrue();
        }
        
        @Test
        @DisplayName("Should trim whitespace on fields")
        void shouldTrimWhitespace() {
            ProjectCreationRequest request = ProjectCreationRequest.builder()
                .withName("  Project Name  ")
                .withDescription("  Description  ")
                .withCountry("  US  ")
                .withLatitude("  123.456  ")
                .withLongitude("  789.012  ")
                .build();
            
            assertThat(request.getName()).isEqualTo("Project Name");
            assertThat(request.getDescription()).isEqualTo("Description");
            assertThat(request.getCountryCode()).isEqualTo("US");
            assertThat(request.getLatitude()).isEqualTo("123.456");
            assertThat(request.getLongitude()).isEqualTo("789.012");
        }
        
        @Test
        @DisplayName("Should reject missing required fields")
        void shouldRejectMissingRequiredFields() {
            // Missing name
            assertThatThrownBy(() -> ProjectCreationRequest.builder()
                .withDescription("Description")
                .withCountry("US")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project name is required");
            
            // Missing description
            assertThatThrownBy(() -> ProjectCreationRequest.builder()
                .withName("Project")
                .withCountry("US")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project description is required");
            
            // Missing country
            assertThatThrownBy(() -> ProjectCreationRequest.builder()
                .withName("Project")
                .withDescription("Description")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country code is required");
        }
        
        @Test
        @DisplayName("Should handle partial coordinates correctly")
        void shouldHandlePartialCoordinates() {
            // Only latitude
            ProjectCreationRequest requestWithLatOnly = ProjectCreationRequest.builder()
                .withName("Project")
                .withDescription("Description")
                .withCountry("US")
                .withLatitude("123.456")
                .build();
            
            assertThat(requestWithLatOnly.hasCoordinates()).isFalse();
            
            // Only longitude
            ProjectCreationRequest requestWithLonOnly = ProjectCreationRequest.builder()
                .withName("Project")
                .withDescription("Description")
                .withCountry("US")
                .withLongitude("789.012")
                .build();
            
            assertThat(requestWithLonOnly.hasCoordinates()).isFalse();
        }
        
        @Test
        @DisplayName("Should implement equality correctly")
        void shouldImplementEquality() {
            ProjectCreationRequest request1 = ProjectCreationRequest.of("Project", "Description", "US");
            ProjectCreationRequest request2 = ProjectCreationRequest.of("Project", "Description", "US");
            ProjectCreationRequest request3 = ProjectCreationRequest.of("Other", "Description", "US");
            
            assertThat(request1).isEqualTo(request2);
            assertThat(request1).isNotEqualTo(request3);
            assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        }
    }
} 