package com.ghostcoach.dto;

import com.ghostcoach.model.ExperienceLevel;
import com.ghostcoach.model.Sport;
import com.ghostcoach.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SportProfileRequest {

    @NotBlank
    @ValidEnum(enumClass = Sport.class, message = "Invalid sport value")
    private String sport;

    @NotBlank
    private String position;

    @NotBlank
    @ValidEnum(enumClass = ExperienceLevel.class, message = "Must be BEGINNER, INTERMEDIATE, or ADVANCED")
    private String experienceLevel;
}
