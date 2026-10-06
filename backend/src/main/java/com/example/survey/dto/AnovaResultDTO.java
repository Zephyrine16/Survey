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
public class AnovaResultDTO {
    private String testName;
    private String factor;
    private String dimension;
    private String hypothesis;

    private List<AnovaGroupStatDTO> groups;
    private double grandMean;
    private int totalN;

    // ANOVA Table metrics
    private int dfBetween;
    private int dfWithin;
    private int dfTotal;

    private double ssBetween;
    private double ssWithin;
    private double ssTotal;

    private double msBetween;
    private double msWithin;

    private double fStatistic;
    private double pValue;
    private double alpha;
    private boolean isSignificant;

    // Effect size
    private double etaSquared;
    private String effectSizeLabel;
    private String conclusion;
}
