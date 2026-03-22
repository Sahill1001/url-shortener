package com.sahil.controller;

import com.sahil.service.UrlService;
import com.sahil.util.IpAddressExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;

/**
 * Controller for URL redirects
 * Handles GET /{shortCode} and performs redirects with minimal latency
 * This is the critical path for performance
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RedirectController {
    private final UrlService urlService;
    private final IpAddressExtractor ipAddressExtractor;

    /**
     * Redirect to original URL
     * GET /{shortCode}
     * 
     * Performance: Optimized for low latency (<10ms with cache)
     * - Redis cache hit avoids database query
     * - Analytics are published asynchronously via Kafka
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {
        String clientIp = ipAddressExtractor.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // Get original URL (from cache or database)
        String originalUrl = urlService.getOriginalUrl(shortCode, clientIp, userAgent);

        // Return HTTP 301 Moved Permanently for permanent redirects
        // This allows browsers and search engines to cache the redirect
        return ResponseEntity
                .status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(originalUrl))
                .build();
    }
}
