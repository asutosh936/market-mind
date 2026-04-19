package com.marketmind.model.dto;

import com.marketmind.model.enums.PatternType;

/**
 * DTO representing a detected candlestick pattern.
 * This is returned as part of analysis responses.
 */
public class PatternDto {
    private String name;
    private PatternType type;
    private double confidence; // 0.0 to 1.0
    private String description;

    public PatternDto(String name, PatternType type, double confidence, String description) {
        this.name = name;
        this.type = type;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp between 0 and 1
        this.description = description;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PatternType getType() {
        return type;
    }

    public void setType(PatternType type) {
        this.type = type;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "PatternDto{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", confidence=" + String.format("%.2f", confidence) +
                ", description='" + description + '\'' +
                '}';
    }
}
