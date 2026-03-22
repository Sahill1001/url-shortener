package com.sahil.cache;

import com.sahil.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter using Redis
 * Prevents abuse of URL shortening API
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_MINUTES = 1;

    private final RedisTemplate<String, Long> redisTemplate;

    public void checkRateLimit(String ipAddress) {
        String key = RATE_LIMIT_PREFIX + ipAddress;

        try {
            Long currentCount = redisTemplate.opsForValue().get(key);

            if (currentCount == null) {
                redisTemplate.opsForValue().set(key, 1L, WINDOW_MINUTES, TimeUnit.MINUTES);
            } else if (currentCount >= MAX_REQUESTS) {
                log.warn("Rate limit exceeded for IP: {}", ipAddress);
                throw new RateLimitExceededException(
                        "Too many requests. Maximum " + MAX_REQUESTS + " requests per minute allowed."
                );
            } else {
                redisTemplate.opsForValue().increment(key);
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error checking rate limit for IP: {}", ipAddress, e);
            // Fail open - allow request if rate limiter fails
        }
    }
}
