package com.example.survey.dto;

import com.example.survey.validation.TextResponseLength;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategorySubmissionDTO {

    private String userId;

    @NotNull(message = "menuItemId is required")
    private Long menuItemId;

    @NotNull(message = "questionId is required")
    private Long questionId;

    private Long selectedOptionId;

    @TextResponseLength
    private String textResponse;
}