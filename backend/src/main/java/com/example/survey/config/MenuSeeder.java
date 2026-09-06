package com.example.survey.config;

import com.example.survey.model.MenuItem;
import com.example.survey.repository.MenuItemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@NullMarked
public class MenuSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(MenuSeeder.class);
    private static final String SEED_KEY = "menu_items";

    private final MenuItemRepository menuItemRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final SeederProperties seederProperties;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        if(!seederProperties.isEnabled()) {
            log.info("Seeders disabled. Skipping menu item seed.");
            return;
        }

        // Only seed once — if a seed_metadata row already exists for "menu_items"
        // the admin may have intentionally deleted items; we must not re-add them.
        Integer alreadySeeded = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seed_metadata WHERE seed_key = ?",
                Integer.class, SEED_KEY);
        if (alreadySeeded != null && alreadySeeded > 0) {
            log.info("Menu items already seeded previously. Skipping.");
            return;
        }

        List<SeedMenuItem> seedItems = loadSeedMenuItems();
        if(seedItems.isEmpty()) {
            log.warn("No seed menu items found at {}", seederProperties.getMenuItemsPath());
            return;
        }

        List<MenuItem> fullMenu = new ArrayList<>();
        for(SeedMenuItem seedItem : seedItems) {
            if(seedItem.name().isBlank() || seedItem.category().isBlank()) {
                continue;
            }
            fullMenu.add(createItem(seedItem.name().trim(), seedItem.category().trim()));
        }

        if(!fullMenu.isEmpty()) {
            menuItemRepository.saveAll(fullMenu);
            log.info("Successfully added {} menu items to the database.", fullMenu.size());
        }

        // Mark seeding as done so it never repeats
        jdbcTemplate.update(
                "INSERT INTO seed_metadata (seed_key) VALUES (?) ON CONFLICT (seed_key) DO NOTHING",
                SEED_KEY);
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
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        } catch(Exception e) {
            log.error("Failed to load seed menu items from {}", seederProperties.getMenuItemsPath(), e);
            return Collections.emptyList();
        }
    }

    private record SeedMenuItem(String name, String category) {}
}