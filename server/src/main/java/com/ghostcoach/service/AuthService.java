package com.ghostcoach.service;

import com.ghostcoach.dto.*;
import com.ghostcoach.model.SportProfile;
import com.ghostcoach.model.User;
import com.ghostcoach.repository.UserRepository;
import com.ghostcoach.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles player registration, login, and profile retrieval.
 * Passwords are never stored in plaintext — BCrypt hashes are persisted instead.
 * Authentication state is carried entirely in the JWT; no server-side sessions exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Creates a new player account and returns an immediate auth token.
     * Registering auto-logs the player in (no separate login step) for a smoother UX.
     *
     * @param req registration payload including sport, position, and experience level —
     *            these are stored on the user and injected into every AI prompt later
     * @throws RuntimeException if the email is already taken
     */
    public AuthResponse register(RegisterRequest req) {
        log.info("Registration attempt");
        if (userRepository.existsByEmail(req.getEmail())) {
            log.warn("Registration rejected — email already registered");
            throw new RuntimeException("Email already registered.");
        }
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .sportProfiles(req.getSportProfiles().stream()
                        .map(r -> SportProfile.builder()
                                .sport(r.getSport())
                                .position(r.getPosition())
                                .experienceLevel(r.getExperienceLevel())
                                .build())
                        .toList())
                .build();
        userRepository.save(user);
        log.info("User registered [userId={}]", user.getId());
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, UserDto.from(user));
    }

    /**
     * Authenticates a player by email and password.
     * The generic "Invalid email or password" message is intentional — revealing
     * which field is wrong would let an attacker enumerate registered emails.
     *
     * @param req login credentials
     * @throws RuntimeException with a deliberately vague message on any failure
     */
    public AuthResponse login(LoginRequest req) {
        log.info("Login attempt");
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed — credentials invalid");
                    return new RuntimeException("Invalid email or password.");
                });
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed — credentials invalid [userId={}]", user.getId());
            throw new RuntimeException("Invalid email or password.");
        }
        log.info("Login successful [userId={}]", user.getId());
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, UserDto.from(user));
    }

    /**
     * Returns the current player's profile by resolving the email from the JWT subject.
     * Used by the frontend on app load to restore the user object from the stored token
     * without requiring the user to log in again.
     *
     * @param email extracted from the JWT by {@code Authentication.getName()}
     */
    public UserDto getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
        return UserDto.from(user);
    }
}
