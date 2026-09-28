package com.example.survey.controller;

import com.example.survey.config.SurveyProperties;
import com.example.survey.model.MenuItem;
import com.example.survey.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemRepository menuItemRepository;
    private final SurveyProperties surveyProperties;

    @GetMapping
    public List<MenuItem> getAllMenuItems(
            @RequestParam(value = "availableOnly", required = false, defaultValue = "false") boolean availableOnly
    ) {
        if (availableOnly) {
            long limit = surveyProperties.getItemRespondentLimit();
            if (limit > 0) {
                return menuItemRepository.findAvailableMenuItems(limit);
            }
        }
        return menuItemRepository.findAll();
    }

    @GetMapping("/available")
    public List<MenuItem> getAvailableMenuItems() {
        long limit = surveyProperties.getItemRespondentLimit();
        if (limit > 0) {
            return menuItemRepository.findAvailableMenuItems(limit);
        }
        return menuItemRepository.findAll();
    }
}
