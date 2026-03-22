package com.sahil.controller;

import com.sahil.dto.CreateUrlRequest;
import com.sahil.dto.CreateUrlResponse;
import com.sahil.dto.UrlStatsResponse;
import com.sahil.service.UrlService;
import com.sahil.util.IpAddressExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API Controller for URL shortening operations
 */
@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
@Slf4j
public class UrlApiController {
    private final UrlService urlService;
    private final IpAddressExtractor ipAddressExtractor;

    /**
     * Create a shortened URL
     * POST /api/v1/urls
     */
    @PostMapping
    public ResponseEntity<CreateUrlResponse> createShortenedUrl(
            @Valid @RequestBody CreateUrlRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = ipAddressExtractor.getClientIp(servletRequest);
        CreateUrlResponse response = urlService.shortenUrl(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get statistics for a shortened URL
     * GET /api/v1/urls/{shortCode}/stats
     */
    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<UrlStatsResponse> getUrlStats(@PathVariable String shortCode) {
        UrlStatsResponse stats = urlService.getUrlStats(shortCode);
        return ResponseEntity.ok(stats);
    }

    /**
     * Delete a shortened URL
     * DELETE /api/v1/urls/{shortCode}
     */
    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(@PathVariable String shortCode) {
        urlService.deleteUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("URL Shortener API is running");
    }
}
