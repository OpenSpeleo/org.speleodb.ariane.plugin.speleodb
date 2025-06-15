package com.arianesline.ariane.plugin.speleodb;

import java.util.Objects;

/**
 * Value object representing a SpeleoDB project ID.
 * Provides type safety and validation for project identifiers.
 */
public final class ProjectId {
    
    private final String value;
    
    private ProjectId(String value) {
        this.value = Objects.requireNonNull(value, "Project ID cannot be null");
    }
    
    /**
     * Creates a ProjectId from a string value.
     * 
     * @param value the project ID string (must not be null or empty)
     * @return a new ProjectId instance
     * @throws IllegalArgumentException if the value is null or empty
     */
    public static ProjectId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be empty");
        }
        return new ProjectId(value.trim());
    }
    
    /**
     * Creates a ProjectId from a JsonObject by extracting the "id" field.
     * 
     * @param project the JsonObject containing project data
     * @return a new ProjectId instance
     * @throws IllegalArgumentException if the project is null or missing "id" field
     */
    public static ProjectId fromJson(jakarta.json.JsonObject project) {
        if (project == null) {
            throw new IllegalArgumentException("Project JsonObject cannot be null");
        }
        
        if (!project.containsKey("id")) {
            throw new IllegalArgumentException("Project JsonObject must contain 'id' field");
        }
        
        return of(project.getString("id"));
    }
    
    /**
     * Gets the raw project ID value.
     * 
     * @return the project ID string
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Validates that this project ID is not empty.
     * 
     * @return true if the project ID is valid, false otherwise
     */
    public boolean isValid() {
        return !value.isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectId projectId = (ProjectId) o;
        return Objects.equals(value, projectId.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return "ProjectId{" + value + "}";
    }
} 