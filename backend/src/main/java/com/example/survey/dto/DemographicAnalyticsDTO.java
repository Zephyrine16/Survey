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
public class DemographicAnalyticsDTO {
    /** Total distinct participants globally (for KPI cards). */
    private Long globalParticipants;
    /** Total respondents who completed Section 1 demographics (denominator for bar charts). */
    private Long totalParticipants;
    private Map<String, Long> ageGroupCounts;
    private Map<String, Long> diningFrequencyCounts;
}

