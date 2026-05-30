package com.ghostcoach.util;

public final class JsonUtil {

    private JsonUtil() {}

    /**
     * Strips markdown code fences (```json ... ``` or ``` ... ```) that Gemini
     * occasionally wraps around JSON output despite being instructed not to.
     */
    public static String stripMarkdownFences(String text) {
        if (text == null) return null;
        text = text.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("(?s)^```(?:json)?\\s*", "").replaceAll("(?s)```\\s*$", "").trim();
        }
        return text;
    }
}
