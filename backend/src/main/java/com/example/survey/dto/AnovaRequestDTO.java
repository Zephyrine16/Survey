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
public class AnovaRequestDTO {
    /**
     * Factor to test:
     * "SUBCATEGORIES" (Compare across Pasta, Waffle, Coffee, Frappe, etc.)
     * "AGE_GROUPS" (Compare across age demographics)
     * "DINING_FREQUENCY" (Compare across dining frequencies)
     * "MOOD_DIMENSIONS" (Compare across all 7 mood ratings)
     * "WEATHER_DIMENSIONS" (Compare across all 3 weather ratings)
     */
    private String factor;

    /**
     * Optional subset of groups/levels to include (null/empty = all available).
     */
    private List<String> selectedGroups;

    /**
     * Optional filter on evaluation dimension (when testing categories or demographics).
     * "all" or specific dimension like "relaxation", "comfort", etc.
     */
    private String dimension;

    /**
     * Optional filter on MenuItem ID (when testing mood/weather dimensions for one item).
     */
    private Long menuItemId;

    /**
     * Significance level (default 0.05).
     */
    @Builder.Default
    private Double alpha = 0.05;
}
