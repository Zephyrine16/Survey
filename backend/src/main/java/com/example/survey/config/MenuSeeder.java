package com.example.survey.config;

import com.example.survey.model.MenuItem;
import com.example.survey.repository.MenuItemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.io.InputStream;

@Component
public class MenuSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(MenuSeeder.class);
    private final MenuItemRepository menuItemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private SeederProperties seederProperties;

    public MenuSeeder(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if(!seederProperties.isEnabled()) {
            log.info("Seeders disabled. Skipping menu item seed.");
            return;
        }

        if(menuItemRepository.count() == 0) {
            List<SeedMenuItem> seedItems = loadSeedMenuItems();
            if(seedItems.isEmpty()) {
                log.warn("No seed menu items found at {}", seederProperties.getMenuItemsPath());
                return;
            }

            List<MenuItem> fullMenu = new ArrayList<>();
            for(SeedMenuItem seedItem : seedItems) {
                if(seedItem == null || seedItem.name() == null || seedItem.name().isBlank() ||
                seedItem.category() == null || seedItem.category().isBlank()) {
                    continue;
                }
                fullMenu.add(createItem(seedItem.name().trim(), seedItem.category().trim()));
            }

            if(!fullMenu.isEmpty()) {
                menuItemRepository.saveAll(fullMenu);
                log.info("Successfully added {} menu items to the database.", fullMenu.size());
            }
        }
    }

    private MenuItem createItem(String name, String category) {
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setCategory(category);
        
        String baseName = name.replaceFirst("^(?i)(Hot |Iced )", "");

        String generatedFileName = baseName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-$", "")
                + ".webp";

        item.setImageName(generatedFileName);
        return item;
    }

    private List<SeedMenuItem> loadSeedMenuItems() {
        Resource resource = resourceLoader.getResource(seederProperties.getMenuItemsPath());
        if(!resource.exists()) {
            return Collections.emptyList();
        }

        try(InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<SeedMenuItem>>() {});
        } catch(Exception e) {
            log.error("Failed to load seed menu items from {}", seederProperties.getMenuItemsPath(), e);
            return Collections.emptyList();
        }
    }

    private record SeedMenuItem(String name, String category) {}
}
