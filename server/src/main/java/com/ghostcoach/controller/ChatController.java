package com.ghostcoach.controller;

import com.ghostcoach.dto.*;
import com.ghostcoach.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions/{sessionId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatMessageDto> send(
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatRequest req,
            Authentication auth) {
        return ResponseEntity.ok(chatService.sendMessage(auth.getName(), sessionId, req.getMessage()));
    }

    @GetMapping
    public ResponseEntity<List<ChatMessageDto>> history(
            @PathVariable Long sessionId,
            Authentication auth) {
        return ResponseEntity.ok(chatService.getHistory(auth.getName(), sessionId));
    }
}
