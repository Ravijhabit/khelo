package com.ghostcoach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostcoach.dto.FeedbackDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock RestTemplate restTemplate;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(geminiService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(geminiService, "apiUrl", "https://gemini.fake/v1:generateContent");
    }

    private Map<String, Object> geminiResponse(String text) {
        Map<String, Object> part = Map.of("text", text);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> candidate = Map.of("content", content);
        return Map.of("candidates", List.of(candidate));
    }

    private static final String VALID_JSON =
            "{\"overallScore\":8,\"strengths\":[\"Good stance\"]," +
            "\"areasToImprove\":[\"Wrist position\"]," +
            "\"priorityFix\":\"Keep elbow up\"," +
            "\"drillSuggestion\":\"Shadow batting drill\"," +
            "\"confidenceLevel\":\"High\"}";

    // --- analyzeStance ---

    @Test
    void analyzeStance_blankApiKey_throwsBeforeHttpCall() {
        ReflectionTestUtils.setField(geminiService, "apiKey", "");
        assertThatThrownBy(() -> geminiService.analyzeStance(new byte[1], "image/jpeg", "prompt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("GEMINI_API_KEY is not configured");
    }

    @Test
    void analyzeStance_validJsonResponse_returnsParsedFeedback() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(geminiResponse(VALID_JSON));

        FeedbackDto feedback = geminiService.analyzeStance("img".getBytes(), "image/jpeg", "analyze this");

        assertThat(feedback.getOverallScore()).isEqualTo(8);
        assertThat(feedback.getStrengths()).containsExactly("Good stance");
        assertThat(feedback.getPriorityFix()).isEqualTo("Keep elbow up");
        assertThat(feedback.getConfidenceLevel()).isEqualTo("High");
    }

    @Test
    void analyzeStance_jsonWrappedInMarkdownFence_stripsAndParses() {
        String fenced = "```json\n" + VALID_JSON + "\n```";
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(geminiResponse(fenced));

        FeedbackDto feedback = geminiService.analyzeStance("img".getBytes(), "image/png", "prompt");

        assertThat(feedback.getOverallScore()).isEqualTo(8);
    }

    @Test
    void analyzeStance_httpCallFails_throwsWithContext() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> geminiService.analyzeStance("img".getBytes(), "image/jpeg", "prompt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Gemini API call failed");
    }

    @Test
    void analyzeStance_malformedJson_throwsWithRawText() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(geminiResponse("not-valid-json"));

        assertThatThrownBy(() -> geminiService.analyzeStance("img".getBytes(), "image/jpeg", "prompt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse AI coaching response");
    }

    // --- chat ---

    @Test
    void chat_nullApiKey_throwsBeforeHttpCall() {
        ReflectionTestUtils.setField(geminiService, "apiKey", null);
        assertThatThrownBy(() -> geminiService.chat("hello"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("GEMINI_API_KEY is not configured");
    }

    @Test
    void chat_validResponse_returnsReplyText() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(geminiResponse("Focus on your wrist position."));

        String reply = geminiService.chat("What should I improve?");

        assertThat(reply).isEqualTo("Focus on your wrist position.");
    }

    @Test
    void chat_httpCallFails_throwsWithContext() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Timeout"));

        assertThatThrownBy(() -> geminiService.chat("prompt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Gemini chat API call failed");
    }
}
