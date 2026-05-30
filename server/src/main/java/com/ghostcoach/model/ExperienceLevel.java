package com.ghostcoach.model;

public enum ExperienceLevel {
    BEGINNER, INTERMEDIATE, ADVANCED;

    public static boolean isValid(String value) {
        if (value == null) return false;
        try {
            valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
