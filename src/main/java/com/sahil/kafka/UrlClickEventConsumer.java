package com.sahil.kafka;

import com.sahil.dto.UrlClickEvent;
import com.sahil.model.UrlAnalytics;
import com.sahil.repository.UrlAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Kafka consumer for processing URL click events
 * Stores analytics data asynchronously
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UrlClickEventConsumer {
    private final UrlAnalyticsRepository analyticsRepository;

    @KafkaListener(topics = UrlClickEventProducer.TOPIC, groupId = "url-shortener-analytics")
    public void processClickEvent(UrlClickEvent event) {
        try {
            UrlAnalytics analytics = UrlAnalytics.builder()
                    .shortCode(event.getShortCode())
                    .clickTime(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(event.getTimestamp()),
                            ZoneId.systemDefault()
                    ))
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .build();

            analyticsRepository.save(analytics);
            log.debug("Stored analytics for short code: {}", event.getShortCode());
        } catch (Exception e) {
            log.error("Failed to process click event for short code: {}", event.getShortCode(), e);
            // Log error but don't fail - analytics is non-critical
        }
    }
}
