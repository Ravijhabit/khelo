package com.ghostcoach.controller;

import com.ghostcoach.dto.*;
import com.ghostcoach.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manages the AI coaching chat thread scoped to a specific coaching session.
 * The session ID is embedded in the URL path so all chat operations are naturally
 * scoped — no separate session ID in the request body needed.
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /api/sessions/{sessionId}/chat
     * Sends a player message to the AI coach and returns the assistant's reply.
     * The service loads the full session context + prior chat history before
     * calling Gemini, so each response is context-aware even though the API is stateless.
     */
    @PostMapping
    public ResponseEntity<ChatMessageDto> send(
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatRequest req,
            Authentication auth) {
        return ResponseEntity.ok(chatService.sendMessage(auth.getName(), sessionId, req.getMessage()));
    }

    /**
     * GET /api/sessions/{sessionId}/chat
     * Returns the full ordered message history for a session.
     * Called on page load by {@code ChatBox.jsx} to restore the conversation
     * when the player navigates back to a session they previously chatted in.
     */
    @GetMapping
    public ResponseEntity<List<ChatMessageDto>> history(
            @PathVariable Long sessionId,
            Authentication auth) {
        return ResponseEntity.ok(chatService.getHistory(auth.getName(), sessionId));
    }
}
