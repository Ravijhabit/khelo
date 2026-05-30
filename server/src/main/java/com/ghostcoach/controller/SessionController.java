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

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SessionResponse> upload(
            @RequestParam("image") MultipartFile file,
            Authentication auth) throws IOException {
        return ResponseEntity.ok(sessionService.analyze(auth.getName(), file));
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> list(Authentication auth) {
        return ResponseEntity.ok(sessionService.listSessions(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> get(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(sessionService.getSession(auth.getName(), id));
    }

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
