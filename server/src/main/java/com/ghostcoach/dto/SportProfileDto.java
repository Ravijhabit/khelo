package com.ghostcoach.dto;

import com.ghostcoach.model.SportProfile;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SportProfileDto {
    private String sport;
    private String position;
    private String experienceLevel;

    public static SportProfileDto from(SportProfile profile) {
        return SportProfileDto.builder()
                .sport(profile.getSport())
                .position(profile.getPosition())
                .experienceLevel(profile.getExperienceLevel())
                .build();
    }
}
