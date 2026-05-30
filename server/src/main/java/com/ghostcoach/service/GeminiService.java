package com.ghostcoach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostcoach.dto.FeedbackDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ghostcoach.util.JsonUtil;
import java.util.*;

/**
 * Thin wrapper around the Gemini Vision API.
 * Responsible for building the HTTP request payload, sending it, and
 * deserializing the response — nothing else. Prompt construction lives
 * in {@link PromptBuilderService} to keep this class focused on the transport layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Sends an image + prompt to Gemini's vision model and returns a structured coaching report.
     * The image is sent as a base64-encoded inline data part alongside the text prompt —
     * Gemini's multimodal API does not require a separate file upload step.
     *
     * <p>Using {@code responseMimeType: "application/json"} in the generation config
     * instructs Gemini to return a valid JSON document rather than prose wrapped in
     * markdown code fences, which eliminates the need for regex stripping in most cases.
     *
     * @param imageBytes raw bytes of the uploaded image
     * @param mimeType   "image/jpeg" or "image/png" — passed to Gemini as the inline data type
     * @param prompt     the coaching analysis prompt from {@link PromptBuilderService}
     * @return parsed {@link FeedbackDto} with all 6 coaching report fields populated
     * @throws RuntimeException if the API key is missing, the HTTP call fails, or the JSON cannot be parsed
     */
    public FeedbackDto analyzeStance(byte[] imageBytes, String mimeType, String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("GEMINI_API_KEY is not configured. Please set it in your .env file.");
        }

        Map<String, Object> request = buildVisionRequest(imageBytes, mimeType, prompt);
        String url = apiUrl + "?key=" + apiKey;

        log.info("Calling Gemini Vision API [imageBytes={}B, mimeType={}]", imageBytes.length, mimeType);
        long start = System.currentTimeMillis();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            String text = extractText(response);
            FeedbackDto feedback = parseFeedback(text);
            log.info("Gemini Vision API success [{}ms, score={}]", System.currentTimeMillis() - start, feedback.getOverallScore());
            return feedback;
        } catch (Exception e) {
            log.error("Gemini Vision API failed [{}ms] — {}", System.currentTimeMillis() - start, e.getMessage(), e);
            throw new RuntimeException("Gemini API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a text-only prompt to Gemini for the coaching chat feature.
     * The full session context and conversation history are baked into the prompt
     * by {@link PromptBuilderService#buildChatPrompt} rather than using Gemini's
     * multi-turn {@code contents} array — simpler to implement and sufficient for
     * sessions with a small number of messages.
     *
     * @param prompt context-rich prompt including session report + chat history
     * @return the coach's reply as a plain string (trimmed by the caller)
     */
    public String chat(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("GEMINI_API_KEY is not configured.");
        }

        Map<String, Object> request = buildTextRequest(prompt);
        String url = apiUrl + "?key=" + apiKey;

        log.debug("Calling Gemini Chat API [promptLen={}]", prompt.length());
        long start = System.currentTimeMillis();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            String reply = extractText(response);
            log.info("Gemini Chat API success [{}ms, replyLen={}]", System.currentTimeMillis() - start, reply.length());
            return reply;
        } catch (Exception e) {
            log.error("Gemini Chat API failed [{}ms] — {}", System.currentTimeMillis() - start, e.getMessage(), e);
            throw new RuntimeException("Gemini chat API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Constructs the Gemini API request body for a multimodal (text + image) call.
     * Temperature 0.3 keeps coaching feedback deterministic and factual — higher
     * values produce more creative but less reliable technique analysis.
     */
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
                        // Forces JSON-only output — prevents markdown code fences in responses
                        "responseMimeType", "application/json"
                )
        );
    }

    /**
     * Constructs the Gemini API request body for a text-only chat call.
     * Higher temperature (0.7) than analysis to allow more natural coaching language.
     */
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

    /**
     * Navigates the nested Gemini response structure to extract the raw text output.
     * Path: response → candidates[0] → content → parts[0] → text
     * Wraps any navigation failure with a clear error that includes the raw response
     * for easier debugging.
     */
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

    /**
     * Deserializes the Gemini text output into a {@link FeedbackDto}.
     * Defensively strips markdown code fences in case Gemini ignores
     * {@code responseMimeType: application/json} on certain prompts or model versions.
     */
    private FeedbackDto parseFeedback(String text) {
        text = JsonUtil.stripMarkdownFences(text);
        try {
            return objectMapper.readValue(text, FeedbackDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI coaching response as JSON. Raw: " + text, e);
        }
    }
}
