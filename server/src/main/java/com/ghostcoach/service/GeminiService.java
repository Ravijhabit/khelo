package com.ghostcoach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostcoach.dto.FeedbackDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public FeedbackDto analyzeStance(byte[] imageBytes, String mimeType, String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("GEMINI_API_KEY is not configured. Please set it in your .env file.");
        }

        Map<String, Object> request = buildVisionRequest(imageBytes, mimeType, prompt);
        String url = apiUrl + "?key=" + apiKey;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            String text = extractText(response);
            return parseFeedback(text);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API call failed: " + e.getMessage(), e);
        }
    }

    public String chat(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("GEMINI_API_KEY is not configured.");
        }

        Map<String, Object> request = buildTextRequest(prompt);
        String url = apiUrl + "?key=" + apiKey;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            return extractText(response);
        } catch (Exception e) {
            throw new RuntimeException("Gemini chat API call failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildVisionRequest(byte[] imageBytes, String mimeType, String prompt) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> inlineData = Map.of("mimeType", mimeType, "data", base64Image);
        Map<String, Object> imagePart = Map.of("inlineData", inlineData);

        Map<String, Object> content = Map.of("parts", List.of(textPart, imagePart));

        return Map.of(
                "contents", List.of(content),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 1024,
                        "responseMimeType", "application/json"
                )
        );
    }

    private Map<String, Object> buildTextRequest(String prompt) {
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(textPart));

        return Map.of(
                "contents", List.of(content),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 512
                )
        );
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response structure: " + response);
        }
    }

    private FeedbackDto parseFeedback(String text) {
        // Strip markdown code fences if Gemini wraps the JSON
        text = text.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("(?s)^```(?:json)?\\s*", "").replaceAll("(?s)```\\s*$", "").trim();
        }
        try {
            return objectMapper.readValue(text, FeedbackDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI coaching response as JSON. Raw: " + text, e);
        }
    }
}
