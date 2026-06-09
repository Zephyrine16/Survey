package com.example.survey.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "seeders")
@Validated
@Getter
@Setter
public class SeederProperties {

    private boolean enabled;

    @NotBlank
    private String questionsPath;

    @NotBlank
    private String menuItemsPath;
}