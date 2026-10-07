package com.example.survey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminLoginRequest {
    @NotBlank(message = "username is required")
    @Size(max = 200)
    private String username;

    @NotBlank(message = "password is required")
    @Size(max = 72)
    private String password;
}
