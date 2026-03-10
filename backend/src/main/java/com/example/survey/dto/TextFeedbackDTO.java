package com.example.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TextFeedbackDTO {
    private String userEmail;
    private String textResponse;
}
