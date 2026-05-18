package com.example.survey.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "admin.login")
public class AdminLoginProperties {
    private int maxFailedAttempts = 5;
    private long lockoutMinutes = 15L;
    private long cacheMaxSize = 1000L;

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
