package com.ghostcoach.service;

import com.ghostcoach.dto.AuthResponse;
import com.ghostcoach.dto.LoginRequest;
import com.ghostcoach.dto.RegisterRequest;
import com.ghostcoach.dto.SportProfileRequest;
import com.ghostcoach.dto.UserDto;
import com.ghostcoach.model.SportProfile;
import com.ghostcoach.model.User;
import com.ghostcoach.repository.UserRepository;
import com.ghostcoach.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    private static final SportProfile CRICKET_PROFILE =
            SportProfile.builder().sport("Cricket").position("Batsman").experienceLevel("Beginner").build();
    private static final SportProfile BADMINTON_PROFILE =
            SportProfile.builder().sport("Badminton").position("Singles Player").experienceLevel("Intermediate").build();

    private User buildUser() {
        return User.builder()
                .id(1L).name("Alice").email("alice@example.com")
                .passwordHash("hashed_pw")
                .sportProfiles(List.of(CRICKET_PROFILE, BADMINTON_PROFILE))
                .build();
    }

    private RegisterRequest buildRegisterRequest() {
        SportProfileRequest cricket = new SportProfileRequest();
        cricket.setSport("Cricket");
        cricket.setPosition("Batsman");
        cricket.setExperienceLevel("Beginner");

        SportProfileRequest badminton = new SportProfileRequest();
        badminton.setSport("Badminton");
        badminton.setPosition("Singles Player");
        badminton.setExperienceLevel("Intermediate");

        RegisterRequest req = new RegisterRequest();
        req.setName("Alice");
        req.setEmail("alice@example.com");
        req.setPassword("password123");
        req.setSportProfiles(List.of(cricket, badminton));
        return req;
    }

    @Test
    void register_newEmail_savesUserAndReturnsToken() {
        RegisterRequest req = buildRegisterRequest();
        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed_pw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(req.getEmail())).thenReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo(req.getEmail());
        assertThat(response.getUser().getSportProfiles()).hasSize(2);
        assertThat(response.getUser().getSportProfiles().get(0).getSport()).isEqualTo("Cricket");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsRuntimeException() {
        RegisterRequest req = buildRegisterRequest();
        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void login_validCredentials_returnsAuthResponse() {
        User user = buildUser();
        LoginRequest req = new LoginRequest();
        req.setEmail(user.getEmail());
        req.setPassword("password123");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(user.getEmail())).thenReturn("jwt-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getSportProfiles()).hasSize(2);
    }

    @Test
    void login_emailNotFound_throwsWithVagueMessage() {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@example.com");
        req.setPassword("pw");
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void login_wrongPassword_throwsWithVagueMessage() {
        User user = buildUser();
        LoginRequest req = new LoginRequest();
        req.setEmail(user.getEmail());
        req.setPassword("wrong");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void getMe_existingEmail_returnsUserDto() {
        User user = buildUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserDto dto = authService.getMe(user.getEmail());

        assertThat(dto.getName()).isEqualTo("Alice");
        assertThat(dto.getSportProfiles()).hasSize(2);
        assertThat(dto.getSportProfiles().get(0).getSport()).isEqualTo("Cricket");
        assertThat(dto.getSportProfiles().get(0).getPosition()).isEqualTo("Batsman");
        assertThat(dto.getSportProfiles().get(1).getSport()).isEqualTo("Badminton");
    }

    @Test
    void getMe_unknownEmail_throwsRuntimeException() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe("ghost@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }
}
