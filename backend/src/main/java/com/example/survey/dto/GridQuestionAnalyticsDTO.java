package com.example.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GridQuestionAnalyticsDTO {
    private Long questionId;
    private String title;
    private String prompt;
    private List<GridRowStatDTO> rows;
    private String topRowLabel;
    private double topRowScore;
    private int totalEvaluators;
}
