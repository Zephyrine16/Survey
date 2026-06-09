package com.example.survey.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "rate-limit")
@Validated
@Getter
@Setter
public class RateLimitProperties {

    @NotNull
    private Boolean enabled;

    @NotBlank
    private String path;

    @NotNull
    @Positive
    private Long windowSeconds;

    @NotNull
    @Positive
    private Long maxSize;

    @NotBlank
    private String message;
}