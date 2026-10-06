package com.example.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TTestRequestDTO {
    /**
     * Mode of comparison:
     * "ITEMS" (Compare two specific menu items by ID)
     * "SUPER_CATEGORIES" (Compare Meals vs. Beverages)
     * "SUBCATEGORIES" (Compare two subcategories e.g. Pasta vs. Coffee)
     * "AGE_GROUPS" (Compare two age brackets)
     * "DINING_FREQUENCY" (Compare two dining frequency groups)
     * "DIMENSIONS" (Compare two mood/weather dimensions across all ratings)
     */
    private String mode;

    /**
     * Identifiers or labels for the two groups.
     * For "ITEMS", these are stringified MenuItem IDs (e.g. "74", "67").
     * For categories/demographics, these are the labels (e.g. "Pasta", "Coffee" or "18–20", "21–23").
     */
    private String group1;
    private String group2;

    /**
     * Optional filter on evaluation dimension:
     * "all" (Overall suitability across all dimensions)
     * or specific mood/weather label (e.g. "relaxation", "comfort", "focus", "rainy", etc.)
     */
    private String dimension;

    /**
     * Significance level (default 0.05).
     */
    @Builder.Default
    private Double alpha = 0.05;
}
