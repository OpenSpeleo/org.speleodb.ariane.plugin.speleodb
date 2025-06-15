package com.arianesline.ariane.plugin.speleodb;

import java.util.Objects;

/**
 * Parameter object for creating a new SpeleoDB project.
 * Encapsulates all the data needed for project creation to avoid long parameter lists.
 */
public final class ProjectCreationRequest {
    
    private final String name;
    private final String description;
    private final String countryCode;
    private final String latitude;
    private final String longitude;
    
    private ProjectCreationRequest(String name, String description, String countryCode, 
                                  String latitude, String longitude) {
        this.name = Objects.requireNonNull(name, "Project name cannot be null");
        this.description = Objects.requireNonNull(description, "Project description cannot be null");
        this.countryCode = Objects.requireNonNull(countryCode, "Country code cannot be null");
        this.latitude = latitude; // Optional - can be null
        this.longitude = longitude; // Optional - can be null
    }
    
    /**
     * Gets the project name.
     * 
     * @return the project name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the project description.
     * 
     * @return the project description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the country code.
     * 
     * @return the ISO country code
     */
    public String getCountryCode() {
        return countryCode;
    }
    
    /**
     * Gets the latitude coordinate.
     * 
     * @return the latitude string, or null if not specified
     */
    public String getLatitude() {
        return latitude;
    }
    
    /**
     * Gets the longitude coordinate.
     * 
     * @return the longitude string, or null if not specified
     */
    public String getLongitude() {
        return longitude;
    }
    
    /**
     * Checks if coordinates are provided.
     * 
     * @return true if both latitude and longitude are specified, false otherwise
     */
    public boolean hasCoordinates() {
        return latitude != null && !latitude.trim().isEmpty() &&
               longitude != null && !longitude.trim().isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectCreationRequest that = (ProjectCreationRequest) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(countryCode, that.countryCode) &&
               Objects.equals(latitude, that.latitude) &&
               Objects.equals(longitude, that.longitude);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, description, countryCode, latitude, longitude);
    }
    
    @Override
    public String toString() {
        return "ProjectCreationRequest{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", countryCode='" + countryCode + '\'' +
               ", latitude='" + latitude + '\'' +
               ", longitude='" + longitude + '\'' +
               '}';
    }
    
    /**
     * Builder pattern for constructing ProjectCreationRequest instances.
     * Provides a fluent API for setting optional parameters.
     */
    public static class Builder {
        private String name;
        private String description;
        private String countryCode;
        private String latitude;
        private String longitude;
        
        /**
         * Sets the project name (required).
         * 
         * @param name the project name
         * @return this builder
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * Sets the project description (required).
         * 
         * @param description the project description
         * @return this builder
         */
        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the country code (required).
         * 
         * @param countryCode the ISO country code
         * @return this builder
         */
        public Builder withCountry(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }
        
        /**
         * Sets the coordinates (optional).
         * 
         * @param latitude the latitude coordinate
         * @param longitude the longitude coordinate
         * @return this builder
         */
        public Builder withCoordinates(String latitude, String longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }
        
        /**
         * Sets the latitude coordinate (optional).
         * 
         * @param latitude the latitude coordinate
         * @return this builder
         */
        public Builder withLatitude(String latitude) {
            this.latitude = latitude;
            return this;
        }
        
        /**
         * Sets the longitude coordinate (optional).
         * 
         * @param longitude the longitude coordinate
         * @return this builder
         */
        public Builder withLongitude(String longitude) {
            this.longitude = longitude;
            return this;
        }
        
        /**
         * Builds the ProjectCreationRequest instance.
         * 
         * @return a new ProjectCreationRequest
         * @throws IllegalArgumentException if required fields are missing
         */
        public ProjectCreationRequest build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Project name is required");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Project description is required");
            }
            if (countryCode == null || countryCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Country code is required");
            }
            
            return new ProjectCreationRequest(
                name.trim(), 
                description.trim(), 
                countryCode.trim(), 
                latitude != null ? latitude.trim() : null,
                longitude != null ? longitude.trim() : null
            );
        }
    }
    
    /**
     * Creates a new builder instance.
     * 
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Convenience method to create a ProjectCreationRequest with all required fields.
     * 
     * @param name the project name
     * @param description the project description
     * @param countryCode the country code
     * @return a new ProjectCreationRequest
     */
    public static ProjectCreationRequest of(String name, String description, String countryCode) {
        return new Builder()
            .withName(name)
            .withDescription(description)
            .withCountry(countryCode)
            .build();
    }
} 