package com.example.survey.controller;

import com.example.survey.dto.CombinedAnalyticsDTO;
import com.example.survey.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/{menuItemId}")
    public Map<Long, Object> getAnalyticsForMenuItem(@PathVariable Long menuItemId) {
        return analyticsService.getAnalyticsForMenuItem(menuItemId);
    }

    @GetMapping("/combined/{menuItemId}")
    public CombinedAnalyticsDTO getCombinedAnalytics(@PathVariable Long menuItemId) {
        return analyticsService.getCombinedAnalytics(menuItemId);
    }
}
