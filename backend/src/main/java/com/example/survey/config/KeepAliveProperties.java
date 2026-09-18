package com.example.survey.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "keep-alive")
@Getter
@Setter
public class KeepAliveProperties {

    private boolean enabled = true;
    private long intervalMs = 600_000;
}
