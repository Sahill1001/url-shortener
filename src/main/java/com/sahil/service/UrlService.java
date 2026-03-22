package com.sahil.service;

import com.sahil.cache.RateLimiter;
import com.sahil.cache.UrlCacheManager;
import com.sahil.dto.CreateUrlRequest;
import com.sahil.dto.CreateUrlResponse;
import com.sahil.dto.UrlClickEvent;
import com.sahil.dto.UrlStatsResponse;
import com.sahil.exception.UrlExpiredException;
import com.sahil.exception.UrlNotFoundException;
import com.sahil.kafka.UrlClickEventProducer;
import com.sahil.model.Url;
import com.sahil.repository.UrlAnalyticsRepository;
import com.sahil.repository.UrlRepository;
import com.sahil.util.Base62Encoder;
import com.sahil.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service class for URL shortening operations
 * Handles caching, database operations, and event publishing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {
    private final UrlRepository urlRepository;
    private final UrlAnalyticsRepository analyticsRepository;
    private final UrlCacheManager cacheManager;
    private final RateLimiter rateLimiter;
    private final UrlClickEventProducer eventProducer;
    private final SnowflakeIdGenerator idGenerator;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.custom-short-code-enabled:true}")
    private boolean customShortCodeEnabled;

    /**
     * Creates a shortened URL with rate limiting and caching
     * @param request The request containing original URL and optional expiration
     * @param clientIp The client IP for rate limiting
     * @return The response containing short URL and metadata
     */
    @Transactional
    public CreateUrlResponse shortenUrl(CreateUrlRequest request, String clientIp) {
        // Check rate limit
        rateLimiter.checkRateLimit(clientIp);

        // Generate or use custom short code
        String shortCode;
        if (customShortCodeEnabled && request.getCustomShortCode() != null) {
            shortCode = request.getCustomShortCode();
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new IllegalArgumentException("Short code already exists: " + shortCode);
            }
        } else {
            long id = idGenerator.generateId();
            shortCode = Base62Encoder.generateShortCode(id);

            // Ensure uniqueness
            while (urlRepository.existsByShortCode(shortCode)) {
                id = idGenerator.generateId();
                shortCode = Base62Encoder.generateShortCode(id);
            }
        }

        // Create URL entity
        Url url = Url.builder()
                .id(idGenerator.generateId())
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .expiresAt(request.getExpiresAt())
                .clickCount(0L)
                .build();

        // Save to database
        url = urlRepository.save(url);

        // Cache the URL
        cacheManager.cacheUrl(url);

        log.info("Created short URL: {} -> {}", shortCode, request.getOriginalUrl());

        return CreateUrlResponse.builder()
                .shortUrl(baseUrl + "/" + shortCode)
                .shortCode(shortCode)
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .build();
    }

    /**
     * Retrieves the original URL for a given short code
     * Publishes click event to Kafka asynchronously
     * @param shortCode The short code
     * @param clientIp The client IP address
     * @param userAgent The client user agent
     * @return The original URL
     */
    @Transactional
    public String getOriginalUrl(String shortCode, String clientIp, String userAgent) {
        // Try cache first (expected to be much faster)
        Url url = cacheManager.getCachedUrl(shortCode);

        // If not in cache, fetch from database
        if (url == null) {
            url = urlRepository.findByShortCode(shortCode)
                    .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));
            cacheManager.cacheUrl(url);
            log.debug("Cache miss, fetched from database: {}", shortCode);
        }

        // Check if URL has expired
        if (url.getExpiresAt() != null && LocalDateTime.now().isAfter(url.getExpiresAt())) {
            throw new UrlExpiredException("This short URL has expired");
        }

        // Increment click count asynchronously via Kafka
        UrlClickEvent event = UrlClickEvent.builder()
                .shortCode(shortCode)
                .timestamp(System.currentTimeMillis())
                .ipAddress(clientIp)
                .userAgent(userAgent)
                .build();

        eventProducer.publishClickEvent(event);

        // Update click count in cache
        cacheManager.incrementClickCount(shortCode, 1);

        log.info("Redirecting from short code: {} to {}", shortCode, url.getOriginalUrl());

        return url.getOriginalUrl();
    }

    /**
     * Retrieves statistics for a short URL
     * @param shortCode The short code
     * @return Statistics including click count
     */
    public UrlStatsResponse getUrlStats(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        // Get click count from cache if available, fallback to database
        Long clickCount = cacheManager.getClickCount(shortCode);
        if (clickCount == 0) {
            clickCount = analyticsRepository.countByShortCode(shortCode);
        }

        return UrlStatsResponse.builder()
                .shortCode(shortCode)
                .originalUrl(url.getOriginalUrl())
                .clickCount(clickCount)
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .build();
    }

    /**
     * Deletes a short URL
     * @param shortCode The short code to delete
     */
    @Transactional
    public void deleteUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        urlRepository.delete(url);
        cacheManager.invalidateCache(shortCode);

        log.info("Deleted short URL: {}", shortCode);
    }
}
