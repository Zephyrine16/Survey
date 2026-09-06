package com.example.survey.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class SurveySubmissionRequest {

    private String phoneNumber;
    private String ageGroup;
    private String diningFrequency;

    @NotNull
    @Size(min = 1, message = "Submission must contain at least one answer")
    @Valid
    private List<CategorySubmissionDTO> answers;
}