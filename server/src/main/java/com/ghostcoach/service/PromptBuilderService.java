package com.ghostcoach.service;

import com.ghostcoach.model.CoachingSession;
import com.ghostcoach.model.ChatMessage;
import com.ghostcoach.model.User;
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
 *   <li>Player profile injected as context — a Beginner Batsman and an Advanced Goalkeeper
 *       receive meaningfully different feedback from the same image.</li>
 *   <li>Role instruction ("You are an expert X coach") grounds the model's persona.</li>
 * </ul>
 */
@Service
public class PromptBuilderService {

    /**
     * Builds the stance analysis prompt for a given player.
     * The prompt is intentionally verbose — explicit schema + guidelines reduce
     * hallucination and ensure all 6 feedback fields are always populated.
     *
     * <p>The sport and experience level are repeated in multiple places within
     * the prompt because LLMs respond to emphasis; a single mention mid-prompt
     * is often ignored in favour of more prominent instructions.
     *
     * @param user the authenticated player whose profile drives personalization
     * @return a complete, ready-to-send prompt string for the Gemini vision call
     */
    public String buildAnalysisPrompt(User user) {
        return String.format("""
                You are an expert %s coaching assistant analyzing a stance photo.

                Player Profile:
                - Name: %s
                - Sport: %s
                - Position/Role: %s
                - Experience Level: %s

                Analyze the technique and stance visible in the image. Provide detailed, actionable coaching feedback tailored specifically to a %s-level %s %s player.

                Return ONLY valid JSON matching this exact structure with no additional text:
                {
                  "overallScore": <integer 1-10>,
                  "strengths": ["<strength 1>", "<strength 2>", "<strength 3>"],
                  "areasToImprove": ["<area 1>", "<area 2>", "<area 3>"],
                  "priorityFix": "<single most important correction for the next practice session>",
                  "drillSuggestion": "<one specific drill or exercise to address the priority fix>",
                  "confidenceLevel": "<Low|Medium|High>"
                }

                Guidelines:
                - Be specific to %s biomechanics and use sport-appropriate terminology
                - Use language a %s-level player will understand
                - priorityFix must be one concrete, actionable correction based on what you see
                - drillSuggestion must be a named drill or specific exercise with brief instructions
                - confidenceLevel reflects how clearly the technique is visible in the image
                - Do not include markdown, code blocks, or any text outside the JSON object
                """,
                user.getSport(),
                user.getName(), user.getSport(), user.getPosition(), user.getExperienceLevel(),
                user.getExperienceLevel(), user.getSport(), user.getPosition(),
                user.getSport(), user.getExperienceLevel()
        );
    }

    /**
     * Builds the coaching chat prompt by injecting the session's full coaching report
     * and the entire conversation history into a single-turn prompt.
     *
     * <p>This avoids Gemini's multi-turn {@code contents} API (which requires careful
     * role alternation and state management) in favour of a simpler "summarised context"
     * approach. Trade-off: the prompt grows with each message, but sessions are short
     * enough (typically 5–10 exchanges) that this is not a practical concern.
     *
     * <p>The session report is injected as a literal block so the AI has access to
     * the exact strengths, areas to improve, and priority fix without needing to
     * "remember" a previous API call — this is stateless by design.
     *
     * @param user        the player whose profile grounds the coaching persona
     * @param session     the coaching session the player is asking about
     * @param history     all prior messages in this session's chat thread (oldest first)
     * @param userMessage the new question from the player
     * @return a fully-assembled prompt ready to send to the Gemini text model
     */
    public String buildChatPrompt(User user, CoachingSession session, List<ChatMessage> history, String userMessage) {
        StringBuilder historyBlock = new StringBuilder();
        for (ChatMessage msg : history) {
            String role = msg.getRole().equals("user") ? "Player" : "Coach";
            historyBlock.append(role).append(": ").append(msg.getContent()).append("\n");
        }

        return String.format("""
                You are Ghost Coach, an expert AI %s coaching assistant. You are helping %s, a %s-level %s %s player.

                === Session Coaching Report ===
                Overall Score: %d/10
                Strengths: %s
                Areas to Improve: %s
                Priority Fix: %s
                Drill Suggestion: %s
                ================================

                %s

                Player: %s

                Respond as a knowledgeable, encouraging coach. Be concise (2–4 sentences), practical, and always relate your advice to the player's sport (%s), position (%s), and experience level (%s). Do not repeat the session report back. Just answer the player's question directly.

                Coach:""",
                user.getSport(),
                user.getName(), user.getExperienceLevel(), user.getSport(), user.getPosition(),
                session.getOverallScore(),
                session.getStrengths(),
                session.getAreasToImprove(),
                session.getPriorityFix(),
                session.getDrillSuggestion(),
                history.isEmpty() ? "" : "Conversation so far:\n" + historyBlock,
                userMessage,
                user.getSport(), user.getPosition(), user.getExperienceLevel()
        );
    }
}
