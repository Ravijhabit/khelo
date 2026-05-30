package com.ghostcoach.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class RegisterRequest {
    @NotBlank private String name;
    @Email @NotBlank private String email;
    @Size(min = 6) @NotBlank private String password;
    @NotEmpty(message = "Select at least one sport")
    private List<@Valid SportProfileRequest> sportProfiles;
}
