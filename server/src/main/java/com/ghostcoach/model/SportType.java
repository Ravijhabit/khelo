package com.ghostcoach.model;

public enum SportType {
    BADMINTON, CRICKET, BASKETBALL;

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
