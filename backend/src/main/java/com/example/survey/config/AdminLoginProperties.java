package com.example.survey.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "admin.login")
@Validated
public class AdminLoginProperties {
    @NotNull
    @Positive
    private Integer maxFailedAttempts;

    @NotNull
    @Positive
    private Long lockoutMinutes;

    @NotNull
    @Positive
    private Long cacheMaxSize;

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public long getLockoutMinutes() {
        return lockoutMinutes;
    }

    public void setLockoutMinutes(long lockoutMinutes) {
        this.lockoutMinutes = lockoutMinutes;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setCacheMaxSize(long cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }
}
