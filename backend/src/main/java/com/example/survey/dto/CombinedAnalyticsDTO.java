package com.example.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinedAnalyticsDTO {
    private Map<Long, Object> analyticsData;
    private DashboardStatsDTO stats;
    private GridQuestionAnalyticsDTO moodAnalytics;
    private GridQuestionAnalyticsDTO weatherAnalytics;
    private DemographicAnalyticsDTO demographics;
    private List<SurveyResponseDetailDTO> recentResponses;

    public CombinedAnalyticsDTO(Map<Long, Object> analyticsData, DashboardStatsDTO stats) {
        this.analyticsData = analyticsData;
        this.stats = stats;
    }
}