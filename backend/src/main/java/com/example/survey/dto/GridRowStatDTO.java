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
public class GridRowStatDTO {
    private String id;
    private String label;
    private String shortLabel;
    private double avgRating;
    private int totalVotes;
    private double suitabilityPct;
    private Map<Integer, Integer> distribution;
}
