package com.ghostcoach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostcoach.dto.FeedbackDto;
import com.ghostcoach.dto.SessionResponse;
import com.ghostcoach.model.CoachingSession;
import com.ghostcoach.model.SportType;
import com.ghostcoach.model.User;
import com.ghostcoach.repository.CoachingSessionRepository;
import com.ghostcoach.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.ghostcoach.model.SportProfile;
import java.io.IOException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock CoachingSessionRepository sessionRepository;
    @Mock UserRepository userRepository;
    @Mock GeminiService geminiService;
    @Mock PromptBuilderService promptBuilder;

    @TempDir Path tempDir;

    private SessionService sessionService;
    private User user;
    private CoachingSession session;

    @BeforeEach
    void setUp() {
        // Construct manually so ObjectMapper is real (needed for JSON serialisation) and
        // @PostConstruct init() is not called (tempDir already exists).
        sessionService = new SessionService(
                sessionRepository, userRepository, geminiService, promptBuilder, new ObjectMapper());
        ReflectionTestUtils.setField(sessionService, "uploadDir", tempDir.toString());

        user = User.builder()
                .id(1L).name("Alice").email("alice@example.com")
                .sportProfiles(List.of(
                        SportProfile.builder().sport("Cricket").position("Batsman").experienceLevel("Beginner").build()))
                .build();

        session = CoachingSession.builder()
                .id(1L).user(user).imagePath("test.jpg").sport(SportType.CRICKET)
                .overallScore(8).strengths("[\"Good balance\"]").areasToImprove("[\"Footwork\"]")
                .priorityFix("Fix grip").drillSuggestion("Cone drills").confidenceLevel("High")
                .build();
    }

    // ── analyze ───────────────────────────────────────────────────────────────

    @Test
    void analyze_validJpeg_savesFileAndReturnsSessionWithFeedback() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "image", "stance.jpg", "image/jpeg", "fake-image-bytes".getBytes());
        FeedbackDto feedback = new FeedbackDto(
                8, List.of("Good posture"), List.of("Improve follow-through"),
                "Keep elbow up", "Shadow batting 15 min", "High");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(promptBuilder.buildAnalysisPrompt(user, SportType.CRICKET)).thenReturn("analysis-prompt");
        when(geminiService.analyzeStance(any(), eq("image/jpeg"), eq("analysis-prompt"))).thenReturn(feedback);
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = sessionService.analyze(user.getEmail(), file, SportType.CRICKET);

        assertThat(response.getOverallScore()).isEqualTo(8);
        assertThat(response.getFeedback()).isNotNull();
        assertThat(response.getFeedback().getStrengths()).containsExactly("Good posture");
        assertThat(response.getImagePath()).endsWith(".jpg");
        assertThat(Files.list(tempDir).count()).isEqualTo(1);
    }

    @Test
    void analyze_validPng_savesWithPngExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "image", "stance.png", "image/png", "fake-png".getBytes());
        FeedbackDto feedback = new FeedbackDto(
                7, List.of("OK"), List.of("Work harder"), "Tip", "Drill", "Medium");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(promptBuilder.buildAnalysisPrompt(user, SportType.BASKETBALL)).thenReturn("prompt");
        when(geminiService.analyzeStance(any(), eq("image/png"), anyString())).thenReturn(feedback);
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = sessionService.analyze(user.getEmail(), file, SportType.BASKETBALL);

        assertThat(response.getImagePath()).endsWith(".png");
    }

    @Test
    void analyze_invalidContentType_throwsRuntimeException() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "stance.gif", "image/gif", "data".getBytes());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> sessionService.analyze(user.getEmail(), file, SportType.CRICKET))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only JPEG and PNG");
    }

    @Test
    void analyze_nullContentType_throwsRuntimeException() {
        MockMultipartFile file = new MockMultipartFile("image", "stance.jpg", null, "data".getBytes());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> sessionService.analyze(user.getEmail(), file, SportType.CRICKET))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only JPEG and PNG");
    }

    @Test
    void analyze_userNotFound_throwsRuntimeException() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "stance.jpg", "image/jpeg", "data".getBytes());
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.analyze("ghost@example.com", file, SportType.CRICKET))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── listSessions ──────────────────────────────────────────────────────────

    @Test
    void listSessions_returnsSessionsNewestFirst() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(sessionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(session));

        List<SessionResponse> responses = sessionService.listSessions(user.getEmail());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void listSessions_noSessions_returnsEmptyList() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(sessionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of());

        assertThat(sessionService.listSessions(user.getEmail())).isEmpty();
    }

    // ── getSession ────────────────────────────────────────────────────────────

    @Test
    void getSession_existingSession_returnsDto() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(session));

        SessionResponse response = sessionService.getSession(user.getEmail(), 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getConfidenceLevel()).isEqualTo("High");
        assertThat(response.getSport()).isEqualTo(SportType.CRICKET);
    }

    @Test
    void getSession_notFound_throwsRuntimeException() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getSession(user.getEmail(), 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Session not found");
    }

    // ── getSessionImage ───────────────────────────────────────────────────────

    @Test
    void getSessionImage_existingFile_returnsBytesFromDisk() throws IOException {
        Path imageFile = tempDir.resolve("test.jpg");
        Files.write(imageFile, "fake-image-content".getBytes());

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(session));

        byte[] bytes = sessionService.getSessionImage(user.getEmail(), 1L);

        assertThat(new String(bytes)).isEqualTo("fake-image-content");
    }

    @Test
    void getSessionImage_sessionNotFound_throwsRuntimeException() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getSessionImage(user.getEmail(), 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Session not found");
    }

    // ── getRawSession ─────────────────────────────────────────────────────────

    @Test
    void getRawSession_existingSession_returnsEntity() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(session));

        CoachingSession raw = sessionService.getRawSession(user.getEmail(), 1L);

        assertThat(raw.getImagePath()).isEqualTo("test.jpg");
        verify(sessionRepository).findByIdAndUser(1L, user);
    }

    @Test
    void getRawSession_notFound_throwsRuntimeException() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndUser(2L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getRawSession(user.getEmail(), 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Session not found");
    }
}
