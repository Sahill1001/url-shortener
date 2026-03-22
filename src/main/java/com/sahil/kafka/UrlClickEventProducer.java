package com.sahil.kafka;

import com.sahil.dto.UrlClickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for publishing URL click events
 * Events are processed asynchronously for analytics
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UrlClickEventProducer {
    public static final String TOPIC = "url-click-events";

    private final KafkaTemplate<String, UrlClickEvent> kafkaTemplate;

    public void publishClickEvent(UrlClickEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getShortCode(), event);
            log.debug("Published click event for short code: {}", event.getShortCode());
        } catch (Exception e) {
            log.error("Failed to publish click event for short code: {}", event.getShortCode(), e);
            // Don't fail the redirect if Kafka is down
        }
    }
}
