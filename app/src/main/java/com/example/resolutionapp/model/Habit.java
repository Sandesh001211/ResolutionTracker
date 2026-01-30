package com.example.resolutionapp.model;

public class Habit {
    private String id;
    private String title;
    private String description;
    private long createdTimestamp;

    public Habit() {
        // Required for Firestore
    }

    public Habit(String id, String title, String description, long createdTimestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdTimestamp = createdTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    private java.util.List<String> frequency;

    // Getters/Setters
    public java.util.List<String> getFrequency() {
        return frequency;
    }

    public void setFrequency(java.util.List<String> frequency) {
        this.frequency = frequency;
    }
}
