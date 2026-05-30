package com.ghostcoach.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank private String name;
    @Email @NotBlank private String email;
    @Size(min = 6) @NotBlank private String password;
    @NotBlank private String sport;
    @NotBlank private String position;
    @NotBlank private String experienceLevel;
}
