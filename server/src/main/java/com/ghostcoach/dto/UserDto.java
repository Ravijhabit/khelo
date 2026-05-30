package com.ghostcoach.dto;

import com.ghostcoach.model.User;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private List<SportProfileDto> sportProfiles;

    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .sportProfiles(user.getSportProfiles().stream()
                        .map(SportProfileDto::from)
                        .toList())
                .build();
    }
}
