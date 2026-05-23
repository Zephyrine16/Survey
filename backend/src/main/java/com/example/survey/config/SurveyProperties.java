package com.example.survey.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "survey")
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

    public long getParticipantLimit() {
        return participantLimit;
    }

    public void setParticipantLimit(Long participantLimit) {
        this.participantLimit = participantLimit;
    }

    public int getTextResponseMaxLength() {
        return textResponseMaxLength;
    }

    public void setTextResponseMaxLength(Integer textResponseMaxLength) {
        this.textResponseMaxLength = textResponseMaxLength;
    }

    public String getParticipantCookieName() {
        return participantCookieName;
    }

    public void setParticipantCookieName(String participantCookieName) {
        this.participantCookieName = participantCookieName;
    }

    public long getParticipantCookieMaxAgeDays() {
        return participantCookieMaxAgeDays;
    }

    public void setParticipantCookieMaxAgeDays(Long participantCookieMaxAgeDays) {
        this.participantCookieMaxAgeDays = participantCookieMaxAgeDays;
    }

    public String getExportFilename() {
        return exportFilename;
    }

    public void setExportFilename(String exportFilename) {
        this.exportFilename = exportFilename;
    }
}