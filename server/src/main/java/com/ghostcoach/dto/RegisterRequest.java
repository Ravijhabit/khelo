package com.ghostcoach.dto;

import com.ghostcoach.model.ExperienceLevel;
import com.ghostcoach.model.Sport;
import com.ghostcoach.validation.ValidEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank private String name;
    @Email @NotBlank private String email;
    @Size(min = 6) @NotBlank private String password;
    @NotBlank @ValidEnum(enumClass = Sport.class, message = "sport must be CRICKET, FOOTBALL, BASKETBALL, or BADMINTON") private String sport;
    @NotBlank private String position;
    @NotBlank @ValidEnum(enumClass = ExperienceLevel.class, message = "experienceLevel must be BEGINNER, INTERMEDIATE, or ADVANCED") private String experienceLevel;
}
