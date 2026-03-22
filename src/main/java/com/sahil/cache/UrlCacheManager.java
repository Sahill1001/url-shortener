package com.sahil.cache;

import com.sahil.model.Url;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/**
 * Redis cache manager for URL caching
 * Reduces database lookups by caching frequently accessed URLs
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UrlCacheManager {
    private static final String URL_CACHE_PREFIX = "url:";
    private static final long DEFAULT_CACHE_TTL_HOURS = 24;

    private final RedisTemplate<String, Url> redisTemplate;

    public void cacheUrl(Url url) {
        try {
            String cacheKey = URL_CACHE_PREFIX + url.getShortCode();
            redisTemplate.opsForValue().set(
                    cacheKey,
                    url,
                    DEFAULT_CACHE_TTL_HOURS,
                    TimeUnit.HOURS
            );
            log.debug("Cached URL: {} with short code: {}", url.getOriginalUrl(), url.getShortCode());
        } catch (Exception e) {
            log.warn("Failed to cache URL: {}", url.getShortCode(), e);
            // Cache failure should not block the operation
        }
    }

    public Url getCachedUrl(String shortCode) {
        try {
            String cacheKey = URL_CACHE_PREFIX + shortCode;
            Url cachedUrl = redisTemplate.opsForValue().get(cacheKey);
            if (cachedUrl != null) {
                log.debug("Cache hit for short code: {}", shortCode);
            }
            return cachedUrl;
        } catch (Exception e) {
            log.warn("Failed to retrieve cached URL: {}", shortCode, e);
            return null;
        }
    }

    public void invalidateCache(String shortCode) {
        try {
            String cacheKey = URL_CACHE_PREFIX + shortCode;
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated cache for short code: {}", shortCode);
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for: {}", shortCode, e);
        }
    }

    public void incrementClickCount(String shortCode, long increment) {
        try {
            String cacheKey = URL_CACHE_PREFIX + shortCode + ":clicks";
            redisTemplate.opsForValue().increment(cacheKey, increment);
            // Expire the counter after 24 hours
            redisTemplate.expire(cacheKey, DEFAULT_CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to increment click count for: {}", shortCode, e);
        }
    }

    public Long getClickCount(String shortCode) {
        try {
            String cacheKey = URL_CACHE_PREFIX + shortCode + ":clicks";
            Object count = redisTemplate.opsForValue().get(cacheKey);
            return count != null ? (Long) count : 0L;
        } catch (Exception e) {
            log.warn("Failed to get click count for: {}", shortCode, e);
            return 0L;
        }
    }
}
