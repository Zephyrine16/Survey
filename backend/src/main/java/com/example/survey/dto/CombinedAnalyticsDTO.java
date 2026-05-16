package com.example.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class CombinedAnalyticsDTO {
    private Map<Long, Object> analyticsData;
    private DashboardStatsDTO stats;
}
