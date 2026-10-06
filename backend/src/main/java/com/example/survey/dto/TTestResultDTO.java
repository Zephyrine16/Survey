package com.example.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TTestResultDTO {
    private String testName;
    private String mode;
    private String dimension;
    private String hypothesis;

    private String group1Label;
    private String group2Label;

    private int n1;
    private int n2;
    private double mean1;
    private double mean2;
    private double sd1;
    private double sd2;

    private double meanDifference;
    private double standardError;
    private double ciLower;
    private double ciUpper;

    private double tStatistic;
    private double degreesOfFreedom;
    private double pValue;
    private double alpha;
    private boolean isSignificant;

    private double cohensD;
    private String effectSizeLabel;
    private String conclusion;
}
