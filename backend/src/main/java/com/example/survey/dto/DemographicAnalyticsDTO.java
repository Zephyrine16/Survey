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
    private Long totalParticipants;
    private Map<String, Long> ageGroupCounts;
    private Map<String, Long> diningFrequencyCounts;
}
