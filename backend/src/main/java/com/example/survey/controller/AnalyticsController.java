package com.example.survey.controller;

import com.example.survey.dto.CombinedAnalyticsDTO;
import com.example.survey.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{menuItemId}")
    public Map<Long, Object> getAnalyticsForMenuItem(@PathVariable Long menuItemId) {
        return analyticsService.getAnalyticsForMenuItem(menuItemId);
    }

    @GetMapping("/combined/{menuItemId}")
    public CombinedAnalyticsDTO getCombinedAnalytics(@PathVariable Long menuItemId) {
        return analyticsService.getCombinedAnalytics(menuItemId);
    }
}
