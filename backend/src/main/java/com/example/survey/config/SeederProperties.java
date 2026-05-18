package com.example.survey.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seeder")
public class SeederProperties {
    private boolean enabled = true;
    private String questionsPath = "classpath:seed/questions.json";
    private String menuItemsPath = "classpath:seed/menu-items.json";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getQuestionsPath() {
        return questionsPath;
    }

    public void setQuestionsPath(String questionsPath) {
        this.questionsPath = questionsPath;
    }

    public String getMenuItemsPath() {
        return menuItemsPath;
    }

    public void setMenuItemsPath(String menuItemsPath) {
        this.menuItemsPath = menuItemsPath;
    }
}
