package com.ghostcoach.dto;

import com.ghostcoach.model.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String sport;
    private String position;
    private String experienceLevel;

    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .sport(user.getSport())
                .position(user.getPosition())
                .experienceLevel(user.getExperienceLevel())
                .build();
    }
}
