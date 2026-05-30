package com.ghostcoach.service;

import com.ghostcoach.model.ChatMessage;
import com.ghostcoach.model.CoachingSession;
import com.ghostcoach.model.SportProfile;
import com.ghostcoach.model.SportType;
import com.ghostcoach.model.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderServiceTest {

    private PromptBuilderService service;

    // 10 specifiers matching buildAnalysisPrompt's format args
    private static final String ANALYSIS_TPL =
            "Coach:%s|%s|%s|%s|%s|%s|%s|%s|%s|%s";

    // 15 specifiers matching buildChatPrompt's format args (%d for score)
    private static final String CHAT_TPL =
            "%s|%s|%s|%s|%s|%d|%s|%s|%s|%s|%s|%s|%s|%s|%s";

    @BeforeEach
    void setUp() {
        service = new PromptBuilderService();
        ReflectionTestUtils.setField(service, "analysisPromptTemplate", ANALYSIS_TPL);
        ReflectionTestUtils.setField(service, "chatPromptTemplate", CHAT_TPL);
    }

    private User buildUser() {
        return User.builder()
                .name("Alice")
                .sportProfiles(List.of(
                        SportProfile.builder().sport("Cricket").position("Batsman").experienceLevel("Beginner").build()))
                .build();
    }

    // ── buildAnalysisPrompt ───────────────────────────────────────────────────

    @Test
    void buildAnalysisPrompt_containsUserProfileAndSport() {
        String prompt = service.buildAnalysisPrompt(buildUser(), SportType.CRICKET);
        assertThat(prompt).contains("Cricket").contains("Alice").contains("Batsman").contains("Beginner");
    }

    @Test
    void buildAnalysisPrompt_usesDisplayNameNotEnumName() {
        String prompt = service.buildAnalysisPrompt(buildUser(), SportType.BADMINTON);
        // displayName() returns "Badminton" (not "BADMINTON")
        assertThat(prompt).contains("Badminton");
        assertThat(prompt).doesNotContain("BADMINTON");
    }

    // ── buildChatPrompt ───────────────────────────────────────────────────────

    @Test
    void buildChatPrompt_containsSessionScore() {
        User user = buildUser();
        CoachingSession session = CoachingSession.builder()
                .id(1L).user(user).sport(SportType.CRICKET).overallScore(9)
                .strengths("[]").areasToImprove("[]")
                .priorityFix("Watch the ball").drillSuggestion("Shadow batting")
                .build();

        String prompt = service.buildChatPrompt(user, session, List.of(), "What should I do?");

        assertThat(prompt).contains("9");
        assertThat(prompt).contains("What should I do?");
    }

    @Test
    void buildChatPrompt_emptyHistory_doesNotIncludeConversationBlock() {
        User user = buildUser();
        CoachingSession session = CoachingSession.builder()
                .id(1L).user(user).sport(SportType.CRICKET).overallScore(7)
                .strengths("[]").areasToImprove("[]").priorityFix("").drillSuggestion("")
                .build();

        String prompt = service.buildChatPrompt(user, session, List.of(), "Hello coach");

        assertThat(prompt).doesNotContain("Conversation so far");
    }

    @Test
    void buildChatPrompt_withHistory_includesConversationBlock() {
        User user = buildUser();
        CoachingSession session = CoachingSession.builder()
                .id(1L).user(user).sport(SportType.CRICKET).overallScore(7)
                .strengths("[]").areasToImprove("[]").priorityFix("").drillSuggestion("")
                .build();
        ChatMessage prior = ChatMessage.builder()
                .role("user").content("How do I improve my grip?").session(session)
                .build();

        String prompt = service.buildChatPrompt(user, session, List.of(prior), "And what about footwork?");

        assertThat(prompt).contains("Conversation so far");
        assertThat(prompt).contains("How do I improve my grip?");
        assertThat(prompt).contains("And what about footwork?");
    }

    @Test
    void buildChatPrompt_assistantHistoryMessage_labelledAsCoach() {
        User user = buildUser();
        CoachingSession session = CoachingSession.builder()
                .id(1L).user(user).sport(SportType.CRICKET).overallScore(7)
                .strengths("[]").areasToImprove("[]").priorityFix("").drillSuggestion("")
                .build();
        ChatMessage assistantMsg = ChatMessage.builder()
                .role("assistant").content("Focus on your wrist position.").session(session)
                .build();

        String prompt = service.buildChatPrompt(user, session, List.of(assistantMsg), "Thanks!");

        assertThat(prompt).contains("Coach: Focus on your wrist position.");
    }

    @Test
    void buildChatPrompt_nullSessionSport_fallsBackToFirstSportProfile() {
        User user = buildUser();
        CoachingSession session = CoachingSession.builder()
                .id(1L).user(user).sport(null).overallScore(6)  // old session, no sport stored
                .strengths("[]").areasToImprove("[]").priorityFix("").drillSuggestion("")
                .build();

        String prompt = service.buildChatPrompt(user, session, List.of(), "Advice?");

        // Falls back to first sport profile's sport name ("Cricket")
        assertThat(prompt).contains("Cricket");
    }
}
