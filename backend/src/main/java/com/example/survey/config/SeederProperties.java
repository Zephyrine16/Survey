package com.example.survey.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "seeders")
@Validated
public class SeederProperties {

    @NotNull
    private Boolean enabled;

    @NotBlank
    private String questionsPath;

    @NotBlank
    private String menuItemsPath;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
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
