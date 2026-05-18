package com.example.survey.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "survey")
public class SurveyProperties {
    private long participantLimit = 30L;
    private int textResponseMaxLength = 250;
    private String participantCookieName = "participant_id";
    private long participantCookieMaxAgeDays = 30L;
    private String exportFilename = "CafeRater_Analytics.csv";

    public long getParticipantLimit() {
        return participantLimit;
    }

    public void setParticipantLimit(long participantLimit) {
        this.participantLimit = participantLimit;
    }

    public int getTextResponseMaxLength() {
        return textResponseMaxLength;
    }

    public void setTextResponseMaxLength(int textResponseMaxLength) {
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

    public void setParticipantCookieMaxAgeDays(long participantCookieMaxAgeDays) {
        this.participantCookieMaxAgeDays = participantCookieMaxAgeDays;
    }

    public String getExportFilename() {
        return exportFilename;
    }

    public void setExportFilename(String exportFilename) {
        this.exportFilename = exportFilename;
    }
}