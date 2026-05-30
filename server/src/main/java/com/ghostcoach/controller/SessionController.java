package com.ghostcoach.controller;

import com.ghostcoach.dto.SessionResponse;
import com.ghostcoach.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Exposes coaching session endpoints: image upload + analysis, session listing,
 * detail retrieval, and image serving. All routes require a valid JWT.
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * POST /api/sessions (multipart/form-data, field name: "image")
     * The core product endpoint. Accepts an image upload, triggers Gemini analysis,
     * persists the session, and returns the full coaching report including the
     * nested {@code feedback} object that the frontend displays immediately.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SessionResponse> upload(
            @RequestParam("image") MultipartFile file,
            Authentication auth) throws IOException {
        return ResponseEntity.ok(sessionService.analyze(auth.getName(), file));
    }

    /**
     * GET /api/sessions
     * Returns all of the authenticated player's sessions, newest first.
     * Used by the History page to populate session cards.
     */
    @GetMapping
    public ResponseEntity<List<SessionResponse>> list(Authentication auth) {
        return ResponseEntity.ok(sessionService.listSessions(auth.getName()));
    }

    /**
     * GET /api/sessions/{id}
     * Returns a single session with full report fields. Ownership is enforced in
     * the service layer — a player cannot access another player's session by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> get(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(sessionService.getSession(auth.getName(), id));
    }

    /**
     * GET /api/sessions/{id}/image
     * Serves the stored image file with the correct Content-Type header.
     * The frontend uses this URL as an {@code <img src>} in session cards and the detail view.
     * Reads the extension from the stored filename to determine JPEG vs PNG content type.
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id, Authentication auth) throws IOException {
        byte[] imageBytes = sessionService.getSessionImage(auth.getName(), id);
        SessionResponse session = sessionService.getSession(auth.getName(), id);
        MediaType mediaType = session.getImagePath() != null && session.getImagePath().endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(imageBytes);
    }
}
