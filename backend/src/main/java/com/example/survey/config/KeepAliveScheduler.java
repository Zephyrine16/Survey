package com.example.survey.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class KeepAliveScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveScheduler.class);

    private final KeepAliveProperties keepAliveProperties;

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Uses fixedDelayString so the interval is read from config.
     * fixedDelay (vs fixedRate) means the next ping starts N ms AFTER the previous one
     * completes — no pile-up if the server is slow.
     */
    @Scheduled(fixedDelayString = "${keep-alive.interval-ms:600000}")
    public void selfPing() {
        if (!keepAliveProperties.isEnabled()) {
            return;
        }

        String url = "http://localhost:" + serverPort + "/api/ping";
        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);
            log.debug("Keep-alive ping OK → {}", response);
        } catch (Exception e) {
            log.warn("Keep-alive ping failed: {}", e.getMessage());
        }
    }
}
