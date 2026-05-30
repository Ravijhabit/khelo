package com.ghostcoach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostcoach.dto.FeedbackDto;
import com.ghostcoach.dto.SessionResponse;
import com.ghostcoach.model.CoachingSession;
import com.ghostcoach.model.User;
import com.ghostcoach.repository.CoachingSessionRepository;
import com.ghostcoach.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final CoachingSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;
    private final PromptBuilderService promptBuilder;
    private final ObjectMapper objectMapper;

    @Value("${storage.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
    }

    public SessionResponse analyze(String email, MultipartFile file) throws IOException {
        User user = getUser(email);

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new RuntimeException("Only JPEG and PNG images are accepted.");
        }

        // Save image to disk
        String ext = contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + ext;
        Path filePath = Paths.get(uploadDir, filename);
        Files.write(filePath, file.getBytes());

        // Build prompt and call Gemini
        String prompt = promptBuilder.buildAnalysisPrompt(user);
        FeedbackDto feedback = geminiService.analyzeStance(file.getBytes(), contentType, prompt);

        // Persist session
        CoachingSession session = CoachingSession.builder()
                .user(user)
                .imagePath(filename)
                .overallScore(feedback.getOverallScore())
                .strengths(objectMapper.writeValueAsString(feedback.getStrengths()))
                .areasToImprove(objectMapper.writeValueAsString(feedback.getAreasToImprove()))
                .priorityFix(feedback.getPriorityFix())
                .drillSuggestion(feedback.getDrillSuggestion())
                .confidenceLevel(feedback.getConfidenceLevel())
                .build();

        sessionRepository.save(session);
        return SessionResponse.fromWithFeedback(session, feedback);
    }

    public List<SessionResponse> listSessions(String email) {
        User user = getUser(email);
        return sessionRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(SessionResponse::from)
                .toList();
    }

    public SessionResponse getSession(String email, Long id) {
        User user = getUser(email);
        CoachingSession session = sessionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Session not found."));
        return SessionResponse.from(session);
    }

    public byte[] getSessionImage(String email, Long id) throws IOException {
        User user = getUser(email);
        CoachingSession session = sessionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Session not found."));
        Path imagePath = Paths.get(uploadDir, session.getImagePath());
        return Files.readAllBytes(imagePath);
    }

    public CoachingSession getRawSession(String email, Long id) {
        User user = getUser(email);
        return sessionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Session not found."));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }
}
