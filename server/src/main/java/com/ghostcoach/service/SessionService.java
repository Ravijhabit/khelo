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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ghostcoach.model.SportType;
import com.ghostcoach.util.ImageUtil;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the full stance analysis lifecycle: image storage, AI invocation,
 * session persistence, and retrieval. Also serves as the gateway for {@link ChatService}
 * to fetch raw session entities without duplicating ownership checks.
 */
@Slf4j
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

    /**
     * Creates the upload directory on application startup if it doesn't exist.
     * Using {@code @PostConstruct} rather than a static initialiser so the
     * {@code uploadDir} value is already injected when this runs.
     */
    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
    }

    /**
     * The core feature: validates the uploaded image, saves it to disk, calls the
     * Gemini Vision API with a personalised prompt, and persists the resulting
     * coaching session to the database.
     *
     * <p>Strengths and areasToImprove are stored as JSON strings in the TEXT column
     * rather than a child table — avoids a join on every session list fetch and is
     * sufficient given these arrays are always read together with the session.
     *
     * <p>The image filename uses UUID to avoid collisions and prevent path traversal
     * attacks (the original filename from the client is never used on disk).
     *
     * @param email the authenticated player's email (from JWT)
     * @param file  the uploaded stance image (validated for MIME type here + client-side)
     * @return {@link SessionResponse} with the nested {@link FeedbackDto} for immediate display
     */
    public SessionResponse analyze(String email, MultipartFile file, SportType sport) throws IOException {
        log.info("Stance analysis started [sport={}, fileSize={}B, contentType={}]",
                sport, file.getSize(), file.getContentType());
        User user = getUser(email);

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            log.warn("Analysis rejected — unsupported content type [contentType={}]", contentType);
            throw new RuntimeException("Only JPEG and PNG images are accepted.");
        }

        // Store image with a UUID name — the original filename is untrusted client input
        String ext = contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + ext;
        Path filePath = Paths.get(uploadDir, filename);
        Files.write(filePath, file.getBytes());
        log.debug("Image saved [filename={}]", filename);

        String prompt = promptBuilder.buildAnalysisPrompt(user, sport);
        FeedbackDto feedback = geminiService.analyzeStance(file.getBytes(), contentType, prompt);

        // Serialize lists as JSON strings for storage in a single TEXT column
        CoachingSession session = CoachingSession.builder()
                .user(user)
                .sport(sport)
                .imagePath(filename)
                .overallScore(feedback.getOverallScore())
                .strengths(objectMapper.writeValueAsString(feedback.getStrengths()))
                .areasToImprove(objectMapper.writeValueAsString(feedback.getAreasToImprove()))
                .priorityFix(feedback.getPriorityFix())
                .drillSuggestion(feedback.getDrillSuggestion())
                .confidenceLevel(feedback.getConfidenceLevel())
                .build();

        sessionRepository.save(session);
        log.info("Session created [sessionId={}, sport={}, score={}, confidence={}]",
                session.getId(), sport, feedback.getOverallScore(), feedback.getConfidenceLevel());
        return SessionResponse.fromWithFeedback(session, feedback);
    }

    /**
     * Returns all sessions for a player, newest first.
     * Strengths and areasToImprove remain as JSON strings — the frontend
     * parses them with {@code JSON.parse()} when rendering session cards.
     *
     * @param email authenticated player's email
     */
    public List<SessionResponse> listSessions(String email) {
        User user = getUser(email);
        List<SessionResponse> sessions = sessionRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(SessionResponse::from)
                .toList();
        log.debug("Listed sessions [count={}]", sessions.size());
        return sessions;
    }

    /**
     * Fetches a single session by ID with ownership enforcement.
     * {@code findByIdAndUser} is a single query — avoids fetching the session
     * and then doing a separate ownership check in Java.
     *
     * @param email authenticated player's email
     * @param id    session primary key
     * @throws RuntimeException (→ 404) if the session doesn't exist or belongs to another user
     */
    public SessionResponse getSession(String email, Long id) {
        User user = getUser(email);
        CoachingSession session = sessionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Session not found."));
        return SessionResponse.from(session);
    }

    /**
     * Reads the stored image file from disk and returns its raw bytes.
     * Ownership is checked via {@code findByIdAndUser} before the file is read,
     * so a player cannot access another player's image by guessing an ID.
     *
     * @param email authenticated player's email
     * @param id    session primary key
     * @return raw image bytes (served with correct MediaType by the controller)
     */
    public byte[] getSessionImage(String email, Long id) throws IOException {
        User user = getUser(email);
        CoachingSession session = sessionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Session not found."));
        Path imagePath = Paths.get(uploadDir, session.getImagePath());
        return Files.readAllBytes(imagePath);
    }

    /**
     * Returns the raw {@link CoachingSession} entity rather than a DTO.
     * Used by {@link ChatService} which needs access to the stored JSON strings
     * (strengths, areasToImprove) to inject them into the chat prompt without re-parsing.
     *
     * @param email authenticated player's email
     * @param id    session primary key
     */
    public CoachingSession getRawSession(String email, Long id) {
        User user = getUser(email);
        return sessionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Session not found."));
    }

    /** Shared user lookup with a consistent 400 error message across all methods. */
    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }
}
