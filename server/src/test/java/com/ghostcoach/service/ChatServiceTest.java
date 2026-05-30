package com.ghostcoach.service;

import com.ghostcoach.dto.ChatMessageDto;
import com.ghostcoach.model.ChatMessage;
import com.ghostcoach.model.CoachingSession;
import com.ghostcoach.model.SportProfile;
import com.ghostcoach.model.User;
import com.ghostcoach.repository.ChatMessageRepository;
import com.ghostcoach.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatMessageRepository chatMessageRepository;
    @Mock UserRepository userRepository;
    @Mock SessionService sessionService;
    @Mock GeminiService geminiService;
    @Mock PromptBuilderService promptBuilder;

    @InjectMocks ChatService chatService;

    private User user;
    private CoachingSession session;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).name("Alice").email("alice@example.com")
                .sportProfiles(List.of(
                        SportProfile.builder().sport("Cricket").position("Batsman").experienceLevel("Beginner").build()))
                .build();
        session = CoachingSession.builder()
                .id(1L).user(user).imagePath("img.jpg").overallScore(7)
                .strengths("[\"Good balance\"]").areasToImprove("[\"Footwork\"]")
                .priorityFix("Widen stance").drillSuggestion("Cone drills").confidenceLevel("Medium")
                .build();
    }

    @Test
    void sendMessage_success_persists2MessagesAndReturnsAssistantDto() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(sessionService.getRawSession(user.getEmail(), 1L)).thenReturn(session);
        when(chatMessageRepository.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of());
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(promptBuilder.buildChatPrompt(eq(user), eq(session), anyList(), anyString())).thenReturn("prompt");
        when(geminiService.chat("prompt")).thenReturn("  Work on your stance.  ");

        ChatMessageDto dto = chatService.sendMessage(user.getEmail(), 1L, "How to improve?");

        assertThat(dto.getRole()).isEqualTo("assistant");
        assertThat(dto.getContent()).isEqualTo("Work on your stance.");
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_userNotFound_throwsRuntimeException() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage("ghost@example.com", 1L, "Hello"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void sendMessage_withPriorHistory_passesHistoryToPromptBuilder() {
        ChatMessage priorMsg = ChatMessage.builder()
                .id(1L).session(session).role("user").content("Previous question").build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(sessionService.getRawSession(user.getEmail(), 1L)).thenReturn(session);
        when(chatMessageRepository.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of(priorMsg));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(promptBuilder.buildChatPrompt(eq(user), eq(session), eq(List.of(priorMsg)), eq("Follow up?")))
                .thenReturn("prompt-with-history");
        when(geminiService.chat("prompt-with-history")).thenReturn("Here's a follow-up answer.");

        ChatMessageDto dto = chatService.sendMessage(user.getEmail(), 1L, "Follow up?");

        assertThat(dto.getContent()).isEqualTo("Here's a follow-up answer.");
    }

    @Test
    void getHistory_existingMessages_returnsMappedDtos() {
        ChatMessage msg = ChatMessage.builder()
                .id(1L).session(session).role("user").content("Hi coach").build();
        when(sessionService.getRawSession(user.getEmail(), 1L)).thenReturn(session);
        when(chatMessageRepository.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of(msg));

        List<ChatMessageDto> history = chatService.getHistory(user.getEmail(), 1L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getContent()).isEqualTo("Hi coach");
        assertThat(history.get(0).getRole()).isEqualTo("user");
    }

    @Test
    void getHistory_noMessages_returnsEmptyList() {
        when(sessionService.getRawSession(user.getEmail(), 1L)).thenReturn(session);
        when(chatMessageRepository.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of());

        List<ChatMessageDto> history = chatService.getHistory(user.getEmail(), 1L);

        assertThat(history).isEmpty();
    }
}
