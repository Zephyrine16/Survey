package com.example.survey.controller;

import com.example.survey.model.MenuItem;
import com.example.survey.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemRepository menuItemRepository;

    @GetMapping
    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }
}
