package com.ghostcoach.dto;

import com.ghostcoach.model.ChatMessage;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageDto {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;

    public static ChatMessageDto from(ChatMessage msg) {
        return ChatMessageDto.builder()
                .id(msg.getId())
                .role(msg.getRole())
                .content(msg.getContent())
                .createdAt(msg.getCreatedAt())
                // what is build
                .build();
    }
}
