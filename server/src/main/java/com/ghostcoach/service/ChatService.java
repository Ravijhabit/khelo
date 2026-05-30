package com.ghostcoach.service;

import com.ghostcoach.dto.ChatMessageDto;
import com.ghostcoach.model.*;
import com.ghostcoach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Manages the coaching chat thread for a given session.
 * Each message exchange is stateless at the API level — history is loaded from
 * the database on every call and injected into the Gemini prompt, so the AI has
 * full conversational context without any server-side session state.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final GeminiService geminiService;
    private final PromptBuilderService promptBuilder;

    /**
     * Processes a player's chat message and returns the AI coach's reply.
     *
     * <p>Sequence:
     * <ol>
     *   <li>Load session (with ownership check via {@link SessionService#getRawSession}).</li>
     *   <li>Load the full prior conversation history — injected into the prompt so the
     *       AI doesn't repeat advice already given in the same session.</li>
     *   <li>Persist the user's message before calling Gemini, so it's not lost
     *       if the API call fails partway through.</li>
     *   <li>Call Gemini with the full context prompt.</li>
     *   <li>Persist and return the assistant reply.</li>
     * </ol>
     *
     * @param email       the authenticated player's email (ownership enforcement)
     * @param sessionId   the coaching session this chat belongs to
     * @param userMessage the player's question or follow-up
     * @return the assistant's reply as a {@link ChatMessageDto}
     */
    public ChatMessageDto sendMessage(String email, Long sessionId, String userMessage) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));

        CoachingSession session = sessionService.getRawSession(email, sessionId);
        List<ChatMessage> history = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);

        // Persist user turn before the API call — if Gemini times out we don't lose the user's message
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .role("user")
                .content(userMessage)
                .build();
        chatMessageRepository.save(userMsg);

        String prompt = promptBuilder.buildChatPrompt(user, session, history, userMessage);
        String reply = geminiService.chat(prompt);

        ChatMessage assistantMsg = ChatMessage.builder()
                .session(session)
                .role("assistant")
                .content(reply.trim())
                .build();
        chatMessageRepository.save(assistantMsg);

        return ChatMessageDto.from(assistantMsg);
    }

    /**
     * Returns the complete ordered message history for a session.
     * The frontend renders this on page load to restore a conversation
     * that the player may have started in a previous browser session.
     *
     * @param email     authenticated player's email (ownership enforcement via getRawSession)
     * @param sessionId the coaching session to fetch history for
     */
    public List<ChatMessageDto> getHistory(String email, Long sessionId) {
        CoachingSession session = sessionService.getRawSession(email, sessionId);
        return chatMessageRepository.findBySessionOrderByCreatedAtAsc(session)
                .stream()
                .map(ChatMessageDto::from)
                .toList();
    }
}
