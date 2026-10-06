package com.example.survey.dto;

import com.example.survey.validation.TextResponseLength;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategorySubmissionDTO {

    private String userId;

    @NotNull(message = "menuItemId is required")
    @Positive
    private Long menuItemId;

    @NotNull(message = "questionId is required")
    @Positive
    private Long questionId;

    @Positive
    private Long selectedOptionId;

    @TextResponseLength
    private String textResponse;
}
