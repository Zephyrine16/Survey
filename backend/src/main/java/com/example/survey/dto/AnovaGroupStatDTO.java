package com.example.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnovaGroupStatDTO {
    private String groupName;
    private int n;
    private double mean;
    private double stdDev;
    private double standardError;
    private double ciLower;
    private double ciUpper;
}
