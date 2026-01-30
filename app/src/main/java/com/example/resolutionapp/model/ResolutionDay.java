package com.example.resolutionapp.model;

import java.util.List;
import java.util.ArrayList;

public class ResolutionDay {
    private String date; // Format YYYY-MM-DD
    private List<String> completedHabitIds; // List of IDs of habits completed on this day

    public ResolutionDay() {
        // Required for Firestore
    }

    public ResolutionDay(String date, List<String> completedHabitIds) {
        this.date = date;
        this.completedHabitIds = completedHabitIds;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<String> getCompletedHabitIds() {
        return completedHabitIds;
    }

    public void setCompletedHabitIds(List<String> completedHabitIds) {
        this.completedHabitIds = completedHabitIds;
    }

    // Helper to check if a specific habit was completed
    public boolean isHabitCompleted(String habitId) {
        return completedHabitIds != null && completedHabitIds.contains(habitId);
    }
}
