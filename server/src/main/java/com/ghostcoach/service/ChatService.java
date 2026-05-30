package com.ghostcoach.service;

import com.ghostcoach.dto.ChatMessageDto;
import com.ghostcoach.model.*;
import com.ghostcoach.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final GeminiService geminiService;
    private final PromptBuilderService promptBuilder;

    public ChatMessageDto sendMessage(String email, Long sessionId, String userMessage) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));

        CoachingSession session = sessionService.getRawSession(email, sessionId);
        List<ChatMessage> history = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session);

        // Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .role("user")
                .content(userMessage)
                .build();
        chatMessageRepository.save(userMsg);

        // Build context-aware prompt and call Gemini
        String prompt = promptBuilder.buildChatPrompt(user, session, history, userMessage);
        String reply = geminiService.chat(prompt);

        // Save assistant reply
        ChatMessage assistantMsg = ChatMessage.builder()
                .session(session)
                .role("assistant")
                .content(reply.trim())
                .build();
        chatMessageRepository.save(assistantMsg);

        return ChatMessageDto.from(assistantMsg);
    }

    public List<ChatMessageDto> getHistory(String email, Long sessionId) {
        CoachingSession session = sessionService.getRawSession(email, sessionId);
        return chatMessageRepository.findBySessionOrderByCreatedAtAsc(session)
                .stream()
                .map(ChatMessageDto::from)
                .toList();
    }
}
