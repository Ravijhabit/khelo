package com.ghostcoach.model;

public enum Sport {
    CRICKET, FOOTBALL, BASKETBALL, BADMINTON;

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
