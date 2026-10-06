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
public class StatisticalOverviewDTO {
    private List<MenuItemOptionDTO> items;
    private List<String> subcategories;
    private List<String> supercategories;
    private List<String> dimensions;
    private List<String> ageGroups;
    private List<String> diningFrequencies;

    private int totalRatingsCount;
    private int totalEvaluatorsCount;

    // Quick default test previews
    private TTestResultDTO sampleTTest;
    private AnovaResultDTO sampleAnova;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuItemOptionDTO {
        private Long id;
        private String name;
        private String category;
        private int ratingCount;
    }
}
