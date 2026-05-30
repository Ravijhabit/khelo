package com.ghostcoach.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant");

    private final String value;

    public static boolean isValid(String value) {
        for (MessageRole role : values()) {
            if (role.value.equals(value)) return true;
        }
        return false;
    }
}
