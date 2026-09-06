package com.example.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyResponseDetailDTO {
    private String userId;
    private Map<String, Integer> moodRatings;
    private Map<String, Integer> weatherRatings;
    private String textFeedback;
}
