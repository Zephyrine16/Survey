package com.example.survey.controller;

import com.example.survey.dto.*;
import com.example.survey.service.AnalyticsService;
import com.example.survey.service.StatisticalAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final StatisticalAnalysisService statisticalAnalysisService;

    @GetMapping("/{menuItemId}")
    public Map<Long, Object> getAnalyticsForMenuItem(@PathVariable Long menuItemId) {
        return analyticsService.getAnalyticsForMenuItem(menuItemId);
    }

    @GetMapping("/combined/{menuItemId}")
    public CombinedAnalyticsDTO getCombinedAnalytics(@PathVariable Long menuItemId) {
        return analyticsService.getCombinedAnalytics(menuItemId);
    }

    @GetMapping("/demographics")
    public com.example.survey.dto.DemographicAnalyticsDTO getDemographics() {
        return analyticsService.getDemographics();
    }

    @GetMapping("/statistical-tests/overview")
    public StatisticalOverviewDTO getStatisticalOverview() {
        return statisticalAnalysisService.getStatisticalOverview();
    }

    @PostMapping("/statistical-tests/t-test")
    public TTestResultDTO runTTest(@RequestBody TTestRequestDTO request) {
        return statisticalAnalysisService.runTTest(request);
    }

    @PostMapping("/statistical-tests/anova")
    public AnovaResultDTO runAnova(@RequestBody AnovaRequestDTO request) {
        return statisticalAnalysisService.runAnova(request);
    }
}
