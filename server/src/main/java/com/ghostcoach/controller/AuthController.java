package com.ghostcoach.controller;

import com.ghostcoach.dto.*;
import com.ghostcoach.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Handles player authentication endpoints. These routes are permitted without a JWT
 * (configured in {@code SecurityConfig}) — everything else in the API requires one.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Creates a new player account and immediately returns a JWT so the player
     * is logged in without a separate step. {@code @Valid} triggers Bean Validation
     * on the request body — errors are caught by {@code GlobalExceptionHandler}.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    /**
     * POST /api/auth/login
     * Authenticates with email + password and returns a fresh JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /**
     * GET /api/auth/me
     * Returns the current player's profile. Called by {@code AuthContext} on app load
     * to restore the user object from a token stored in localStorage, avoiding a
     * full re-login on page refresh. {@code auth.getName()} resolves to the JWT subject (email).
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication auth) {
        return ResponseEntity.ok(authService.getMe(auth.getName()));
    }
}
