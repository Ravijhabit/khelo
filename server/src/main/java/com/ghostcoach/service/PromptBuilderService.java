package com.ghostcoach.service;

import com.ghostcoach.model.CoachingSession;
import com.ghostcoach.model.ChatMessage;
import com.ghostcoach.model.SportProfile;
import com.ghostcoach.model.SportType;
import com.ghostcoach.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Builds the AI prompts for both stance analysis and coaching chat.
 * All personalization (sport, position, experience level) is injected here
 * so {@link GeminiService} remains a pure transport layer with no domain knowledge.
 *
 * <p>Prompt design philosophy:
 * <ul>
 *   <li>Explicit JSON schema in the prompt — removes ambiguity about the expected output shape.</li>
 *   <li>Per-sport profile injected as context — a Beginner Batsman and an Advanced Singles
 *       Player receive meaningfully different feedback for their respective sports.</li>
 *   <li>Role instruction ("You are an expert X coach") grounds the model's persona.</li>
 * </ul>
 */
@Service
public class PromptBuilderService {

    @Value("${prompt.analysis}")
    private String analysisPromptTemplate;

    @Value("${prompt.chat}")
    private String chatPromptTemplate;

    /**
     * Builds the stance analysis prompt for the sport the player selected for this session.
     * Looks up the player's registered profile for that sport (position + experience level).
     * Falls back to the first profile if no exact match exists (e.g. old sessions).
     */
    public String buildAnalysisPrompt(User user, SportType sport) {
        String sportName = sport.displayName();
        SportProfile profile = findProfile(user, sportName);
        String position = profile != null ? profile.getPosition() : "";
        String expLevel  = profile != null ? profile.getExperienceLevel() : "";

        return String.format(analysisPromptTemplate,
                sportName,
                user.getName(), sportName, position, expLevel,
                expLevel, sportName, position,
                sportName, expLevel
        );
    }

    /**
     * Builds the coaching chat prompt by injecting the session's full coaching report
     * and the entire conversation history into a single-turn prompt.
     *
     * <p>Uses the sport recorded on the session to look up the player's matching profile
     * (position + experience level), so the coach persona stays sport-specific.
     */
    public String buildChatPrompt(User user, CoachingSession session, List<ChatMessage> history, String userMessage) {
        String sportName = session.getSport() != null
                ? session.getSport().displayName()
                : (user.getSportProfiles().isEmpty() ? "" : user.getSportProfiles().get(0).getSport());

        SportProfile profile = findProfile(user, sportName);
        String position = profile != null ? profile.getPosition() : "";
        String expLevel  = profile != null ? profile.getExperienceLevel() : "";

        StringBuilder historyBlock = new StringBuilder();
        for (ChatMessage msg : history) {
            String role = msg.getRole().equals("user") ? "Player" : "Coach";
            historyBlock.append(role).append(": ").append(msg.getContent()).append("\n");
        }

        return String.format(chatPromptTemplate,
                sportName,
                user.getName(), expLevel, sportName, position,
                session.getOverallScore(),
                session.getStrengths(),
                session.getAreasToImprove(),
                session.getPriorityFix(),
                session.getDrillSuggestion(),
                history.isEmpty() ? "" : "Conversation so far:\n" + historyBlock,
                userMessage,
                sportName, position, expLevel
        );
    }

    /**
     * Finds the sport profile matching the given sport name (case-insensitive).
     * Falls back to the first profile if no match — handles old sessions that pre-date
     * the per-sport profile model.
     */
    private SportProfile findProfile(User user, String sportName) {
        return user.getSportProfiles().stream()
                .filter(p -> p.getSport().equalsIgnoreCase(sportName))
                .findFirst()
                .orElseGet(() -> user.getSportProfiles().isEmpty() ? null : user.getSportProfiles().get(0));
    }
}
