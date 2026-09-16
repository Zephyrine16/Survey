package com.example.survey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuestionRequest {
    @NotBlank(message = "text is required")
    @Size(max = 500, message = "text is too long")
    private String text;

    @NotBlank(message = "type is required")
    @Pattern(regexp = "RADIO|TEXT|MATRIX", message = "type must be RADIO, TEXT, or MATRIX")
    private String type;
}
