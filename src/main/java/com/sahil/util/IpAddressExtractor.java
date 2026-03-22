package com.sahil.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Utility class for extracting client information from HTTP requests
 */
@Component
public class IpAddressExtractor {
    /**
     * Extracts the client IP address from an HTTP request
     * Handles proxies and load balancers
     */
    public String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp != null && !clientIp.isEmpty() && !"unknown".equalsIgnoreCase(clientIp)) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            return clientIp.split(",")[0].trim();
        }

        clientIp = request.getHeader("X-Real-IP");
        if (clientIp != null && !clientIp.isEmpty() && !"unknown".equalsIgnoreCase(clientIp)) {
            return clientIp;
        }

        clientIp = request.getRemoteAddr();
        return clientIp != null ? clientIp : "unknown";
    }
}
