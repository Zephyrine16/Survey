package com.example.survey.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "survey")
@Validated
@Getter
@Setter
public class SurveyProperties {

    @NotNull
    @PositiveOrZero
    private Long participantLimit;

    @NotNull
    @Positive
    private Integer textResponseMaxLength;

    @NotBlank
    private String participantCookieName;

    @NotNull
    @Positive
    private Long participantCookieMaxAgeDays;

    @NotBlank
    private String exportFilename;
}